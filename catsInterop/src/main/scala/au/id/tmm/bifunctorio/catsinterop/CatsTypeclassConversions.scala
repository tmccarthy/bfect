package au.id.tmm.bifunctorio.catsinterop

import au.id.tmm.bifunctorio.typeclasses._
import au.id.tmm.bifunctorio.typeclasses.effects.{Async, Bracket, Concurrent, Sync}

private[catsinterop] object TmmToCatsTypeclassConversionsImpls {

  class CatsBifunctorForTmmBifunctor[F[_, _]](implicit tmmBifunctor: Bifunctor[F]) extends cats.Bifunctor[F] {
    override def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D] = tmmBifunctor.biMap(fab)(f, g)

    override def leftMap[A, B, C](fab: F[A, B])(f: A => C): F[C, B] = tmmBifunctor.leftMap(fab)(f)
  }

  class CatsMonadForTmmBifunctorMonad[F[+_, +_], E](implicit tmmBifunctorMonad: BifunctorMonad[F]) extends cats.Monad[F[E, +?]] {
    override def flatMap[A, A1](fea: F[E, A])(f: A => F[E, A1]): F[E, A1] = tmmBifunctorMonad.flatMap[E, E, A, A1](fea)(f)

    override def tailRecM[A, A1](a: A)(f: A => F[E, Either[A, A1]]): F[E, A1] = tmmBifunctorMonad.tailRecM[E, A, A1](a)(f)

    override def pure[A](a: A): F[E, A] = tmmBifunctorMonad.rightPure(a)
  }

  class CatsMonadErrorForTmmBME[F[+_, +_], E](implicit tmmBme: BME[F]) extends cats.MonadError[F[E, +?], E] {
    override def flatMap[A, A1](fea: F[E, A])(f: A => F[E, A1]): F[E, A1] = tmmBme.flatMap[E, E, A, A1](fea)(f)

    override def tailRecM[A, A1](a: A)(f: A => F[E, Either[A, A1]]): F[E, A1] = tmmBme.tailRecM[E, A, A1](a)(f)

    override def pure[A](a: A): F[E, A] = tmmBme.rightPure(a)

    override def raiseError[A](e: E): F[E, A] = tmmBme.leftPure(e)

    override def handleErrorWith[A](fea: F[E, A])(f: E => F[E, A]): F[E, A] = tmmBme.handleErrorWith(fea)(f)
  }

  class CatsBracketForTmmBracket[F[+_, +_]](implicit tmmBracket: Bracket[F]) extends CatsMonadErrorForTmmBME[F, Throwable] with cats.effect.Bracket[F[Throwable, +?], Throwable] {
    override def bracketCase[A, B](acquire: F[Throwable, A])(use: A => F[Throwable, B])(release: (A, cats.effect.ExitCase[Throwable]) => F[Throwable, Unit]): F[Throwable, B] = {

      val releaseForTmmBracket: (A, ExitCase[Throwable, B]) => F[Nothing, _] = { case (resource, exitCase) =>
        val catsExitCase: cats.effect.ExitCase[Throwable] = exitCase match {
          case ExitCase.Succeeded(a)                 => cats.effect.ExitCase.Completed
          case ExitCase.Failed(Failure.Checked(e))   => cats.effect.ExitCase.Error(e)
          case ExitCase.Failed(Failure.Unchecked(t)) => cats.effect.ExitCase.Error(t)
        }

        tmmBracket.handleErrorWith[Throwable, Unit, Nothing](release(resource, catsExitCase))(t => throw t)
      }

      tmmBracket.bracketCase[A, Throwable, B](acquire)(releaseForTmmBracket)(use)
    }
  }

  private object CatsBracketForTmmBracket {
    final case class FailureInResourceReleaseException[E](cause: E) extends Exception
  }

  class CatsSyncForTmmSync[F[+_, +_]](implicit tmmSync: Sync[F]) extends CatsBracketForTmmBracket[F] with cats.effect.Sync[F[Throwable, +?]] {
    override def suspend[A](thunk: => F[Throwable, A]): F[Throwable, A] = tmmSync.suspend(thunk)
  }

  class CatsAsyncForTmmAsync[F[+_, +_]](implicit tmmAsync: Async[F]) extends CatsSyncForTmmSync[F] with cats.effect.Async[F[Throwable, +?]] {
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[Throwable, A] = tmmAsync.async(k)
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => F[Throwable, Unit]): F[Throwable, A] = tmmAsync.asyncF[Throwable, A](k.andThen(makeFailureUnchecked(_)))
  }

  class CatsConcurrentForTmmConcurrent[F[+_, +_]](implicit tmmConcurrent: Concurrent[F]) extends CatsAsyncForTmmAsync with cats.effect.Concurrent[F[Throwable, +?]] {
    import CatsConcurrentForTmmConcurrent.asCatsFiber

    override def start[A](fa: F[Throwable, A]): F[Throwable, cats.effect.Fiber[F[Throwable, +?], A]] = tmmConcurrent.map(tmmConcurrent.start(fa))(asCatsFiber)

    override def racePair[A, B](fa: F[Throwable, A], fb: F[Throwable, B]): F[Throwable, Either[(A, cats.effect.Fiber[F[Throwable, +?], B]), (cats.effect.Fiber[F[Throwable, +?], A], B)]] =
      tmmConcurrent.map(tmmConcurrent.racePair(fa, fb)) {
        case Left((a, bFiber))  => Left((a, asCatsFiber(bFiber)))
        case Right((aFiber, b)) => Right((asCatsFiber(aFiber), b))
      }

    override def race[A, B](fa: F[Throwable, A], fb: F[Throwable, B]): F[Throwable, Either[A, B]] = tmmConcurrent.race(fa, fb)

    override def cancelable[A](k: (Either[Throwable, A] => Unit) => cats.effect.CancelToken[F[Throwable, +?]]): F[Throwable, A] = tmmConcurrent.cancelable(k.andThen(makeFailureUnchecked(_)))
  }

  object CatsConcurrentForTmmConcurrent {
    def asCatsFiber[F[+_, +_], A](tmmFibre: Fibre[F, Throwable, A]): cats.effect.Fiber[F[Throwable, +?], A] = new cats.effect.Fiber[F[Throwable, +?], A] {
      override def cancel: cats.effect.CancelToken[F[Throwable, +?]] = tmmFibre.cancel

      override def join: F[Throwable, A] = tmmFibre.join
    }
  }

  private def makeFailureUnchecked[F[+_, +_], E, A](fea: F[E, A])(implicit syncInstance: Sync[F]): F[Nothing, A] =
    syncInstance.handleErrorWith(fea) {
      case t: Throwable => syncInstance.sync(throw t)
      case e            => syncInstance.sync(throw FailureInCancellationToken(e))
    }

  final case class FailureInCancellationToken[E](e: E) extends Exception

}

trait TmmToCatsTypeclassConversions {

  import TmmToCatsTypeclassConversionsImpls._

  implicit def tmmBifunctorIsCatsBifunctor[F[_, _] : Bifunctor]: cats.Bifunctor[F] = new CatsBifunctorForTmmBifunctor[F]()
  implicit def tmmBifunctorMonadIsCatsMonad[F[+_, +_] : BifunctorMonad, E]: cats.Monad[F[E, +?]] = new CatsMonadForTmmBifunctorMonad[F, E]()
  implicit def tmmBifunctorMonadErrorIsCatsMonadError[F[+_, +_] : BifunctorMonadError, E]: cats.MonadError[F[E, +?], E] = new CatsMonadErrorForTmmBME[F, E]()
  implicit def tmmBracketIsCatsBracket[F[+_, +_] : Bracket]: cats.effect.Bracket[F[Throwable, +?], Throwable] = new CatsBracketForTmmBracket[F]()
  implicit def tmmSyncIsCatsSync[F[+_, +_] : Sync]: cats.effect.Sync[F[Throwable, +?]] = new CatsSyncForTmmSync[F]()
  implicit def tmmAsyncIsCatsAsync[F[+_, +_] : Async]: cats.effect.Async[F[Throwable, +?]] = new CatsAsyncForTmmAsync[F]()
  implicit def tmmConcurrentIsCatsConcurrent[F[+_, +_] : Concurrent]: cats.effect.Concurrent[F[Throwable, +?]] = new CatsConcurrentForTmmConcurrent[F]()

}

trait CatsToTmmTypeclassConversions {

  implicit def catsBifunctorIsTmmBifunctor[F[_, _]](implicit catsBifunctor: cats.Bifunctor[F]): Bifunctor[F] = new Bifunctor[F] {
    override def biMap[L1, R1, L2, R2](f: F[L1, R1])(leftF: L1 => L2, rightF: R1 => R2): F[L2, R2] = catsBifunctor.bimap(f)(leftF, rightF)

    override def leftMap[L1, R, L2](f: F[L1, R])(leftF: L1 => L2): F[L2, R] = catsBifunctor.leftMap(f)(leftF)
  }

}

object CatsToTmmTypeclassConversions extends CatsToTmmTypeclassConversions

trait CatsTypeclassConversions extends TmmToCatsTypeclassConversions

object CatsTypeclassConversions extends CatsTypeclassConversions
