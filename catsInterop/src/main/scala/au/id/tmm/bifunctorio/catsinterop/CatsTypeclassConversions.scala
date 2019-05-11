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

  class CatsMonadErrorForTmmBME[F[+_, +_], E](implicit tmmBme: BME[F]) extends CatsMonadForTmmBifunctorMonad with cats.MonadError[F[E, +?], E] {
    override def raiseError[A](e: E): F[E, A] = tmmBme.leftPure(e)

    override def handleErrorWith[A](fea: F[E, A])(f: E => F[E, A]): F[E, A] = tmmBme.handleErrorWith(fea)(f)
  }

  class CatsBracketForTmmBracket[F[+_, +_], E](implicit tmmBracket: Bracket[F]) extends CatsMonadErrorForTmmBME with cats.effect.Bracket[F[E, +?], E] {
    override def bracketCase[A, B](acquire: F[E, A])(use: A => F[E, B])(release: (A, cats.effect.ExitCase[E]) => F[E, Unit]): F[E, B] = {

      val releaseForTmmBracket: (A, ExitCase[E, B]) => F[Nothing, _] = { case (resource, exitCase) =>
        val catsExitCase = exitCase match {
          case ExitCase.Succeeded(a)    => cats.effect.ExitCase.Completed
          case ExitCase.Failed(failure) => cats.effect.ExitCase.Error(failure)
        }

        tmmBracket.handleErrorWith[E, Unit, Nothing](release(resource, catsExitCase)) {
          case t: Throwable => throw t
          case e            => throw CatsBracketForTmmBracket.FailureInResourceReleaseException[E](e)
        }
      }

      tmmBracket.bracketCase[A, E, B](acquire)(releaseForTmmBracket)(use)
    }
  }

  private object CatsBracketForTmmBracket {
    final case class FailureInResourceReleaseException[E](cause: E) extends Exception
  }

  class CatsSyncForTmmSync[F[+_, +_]](implicit tmmSync: Sync[F]) extends CatsBracketForTmmBracket[F, Throwable] with cats.effect.Sync[F[Throwable, +?]] {
    override def suspend[A](thunk: => F[Throwable, A]): F[Throwable, A] = tmmSync.suspend(thunk)
  }

  class CatsAsyncForTmmAsync[F[+_, +_]](implicit tmmAsync: Async[F]) extends CatsSyncForTmmSync[F] with cats.effect.Async[F[Throwable, +?]] {
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[Throwable, A] = tmmAsync.async(k)
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => F[Throwable, Unit]): F[Throwable, A] = tmmAsync.asyncF[Throwable, A](k.andThen(makeFailureUnchecked))
  }

  class CatsConcurrentForTmmConcurrent[F[+_, +_]](implicit tmmConcurrent: Concurrent[F]) extends CatsAsyncForTmmAsync with cats.effect.Concurrent[F[Throwable, +?]] {
    import CatsConcurrentForTmmConcurrent.asCatsFiber

    override def start[A](fa: F[Throwable, A]): F[Throwable, cats.effect.Fiber[F[Throwable, +?], A]] = map(tmmConcurrent.start(fa))(asCatsFiber)

    override def racePair[A, B](fa: F[Throwable, A], fb: F[Throwable, B]): F[Throwable, Either[(A, cats.effect.Fiber[F[Throwable, +?], B]), (cats.effect.Fiber[F[Throwable, +?], A], B)]] =
      map(tmmConcurrent.racePair(fa, fb)) {
        case Left((a, bFiber))  => Left((a, asCatsFiber(bFiber)))
        case Right((aFiber, b)) => Right((asCatsFiber(aFiber), b))
      }

    override def race[A, B](fa: F[Throwable, A], fb: F[Throwable, B]): F[Throwable, Either[A, B]] = tmmConcurrent.race(fa, fb)

    override def cancelable[A](k: (Either[Throwable, A] => Unit) => cats.effect.CancelToken[F[Throwable, +?]]): F[Throwable, A] = tmmConcurrent.cancelable(k.andThen(makeFailureUnchecked))
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
  implicit def tmmBracketIsCatsBracket[F[+_, +_] : Bracket, E]: cats.effect.Bracket[F[E, +?], E] = new CatsBracketForTmmBracket[F, E]()
  implicit def tmmSyncIsCatsSync[F[+_, +_] : Sync]: cats.effect.Sync[F[Throwable, +?]] = new CatsSyncForTmmSync[F]()
  implicit def tmmAsyncIsCatsAsync[F[+_, +_] : Async]: cats.effect.Async[F[Throwable, +?]] = new CatsAsyncForTmmAsync[F]()
  implicit def tmmConcurrentIsCatsConcurrent[F[+_, +_] : Concurrent]: cats.effect.Concurrent[F[Throwable, +?]] = new CatsConcurrentForTmmConcurrent[F]()

}

trait CatsToTmmTypeclassConversions {

  implicit def catsBifunctorIsTmmBifunctor[F[_, _] : cats.Bifunctor]: Bifunctor[F] = new Bifunctor[F] {
    override def biMap[L1, R1, L2, R2](f: F[L1, R1])(leftF: L1 => L2, rightF: R1 => R2): F[L2, R2] = cats.Bifunctor[F].bimap(f)(leftF, rightF)

    override def leftMap[L1, R, L2](f: F[L1, R])(leftF: L1 => L2): F[L2, R] = cats.Bifunctor[F].leftMap(f)(leftF)
  }

}

trait CatsTypeclassConversions extends TmmToCatsTypeclassConversions with CatsToTmmTypeclassConversions

object CatsTypeclassConversions extends CatsTypeclassConversions
