package au.id.tmm.bfect.catsinterop.instances

import au.id.tmm.bfect
import au.id.tmm.bfect._
import au.id.tmm.bfect.catsinterop.instances.EitherTBfectConcurrent.CheckedException
import au.id.tmm.bfect.effects.{Async, Concurrent, Die, Now, Sync, Timer}
import cats.{ApplicativeError, Functor, Monad, MonadError}
import cats.data.EitherT
import cats.effect.{CancelToken, Fiber}

import java.time.{Duration, Instant}
import scala.concurrent.CancellationException

class EitherTBfectDie[F[_]](implicit F: MonadError[F, Throwable])
    extends BMEInstance[F]
    with Die[EitherT[F, *, *]] {
  override def failUnchecked(t: Throwable): EitherT[F, Nothing, Nothing] =
    EitherT.liftF[F, Nothing, Nothing](F.raiseError[Nothing](t))
}

class EitherTBfectBracket[F[_]](implicit F: cats.effect.Bracket[F, Throwable])
    extends EitherTBfectDie[F]
    with bfect.effects.Bracket.WithBMonad[EitherT[F, *, *]] {
  override def bracketCase[R, E, A](
    acquire: EitherT[F, E, R],
    release: (R, ExitCase[E, Unit]) => EitherT[F, Nothing, _],
    use: R => EitherT[F, E, A],
  ): EitherT[F, E, A] = EitherT {
    F.bracketCase[Either[E, R], Either[E, A]](
      acquire.value,
    )(
      use = {
        case Left(e)  => F.pure(Left(e))
        case Right(r) => use(r).value
      },
    )(
      release = {
        case (Left(e), _) => F.pure(Left(e))
        case (Right(r), catsExitCase) => {
          val bfectExitCase: ExitCase[E, Unit] = catsExitCase match {
            case cats.effect.ExitCase.Completed => ExitCase.Succeeded(())
            case cats.effect.ExitCase.Error(e)  => ExitCase.Failed(Failure.Unchecked(e))
            case cats.effect.ExitCase.Canceled  => ExitCase.Failed(Failure.Unchecked(new CancellationException))
          }

          F.as(release(r, bfectExitCase).value, ())
        }
      },
    )
  }
}

class EitherTBfectSync[F[_] : cats.effect.Sync] extends EitherTBfectBracket[F] with Sync[EitherT[F, *, *]] {
  override def failUnchecked(t: Throwable): EitherT[F, Nothing, Nothing] =
    EitherT.liftF[F, Nothing, Nothing](cats.effect.Sync[F].raiseError[Nothing](t))

  override def suspend[E, A](effect: => EitherT[F, E, A]): EitherT[F, E, A] =
    EitherT {
      cats.effect.Sync[F].defer(effect.value)
    }
}

class EitherTBfectAsync[F[_] : cats.effect.Async] extends EitherTBfectSync[F] with Async[EitherT[F, *, *]] {
  override def asyncF[E, A](registerForBfect: (Either[E, A] => Unit) => EitherT[F, Nothing, _]): EitherT[F, E, A] = {
    val registerForCats: (Either[Throwable, Either[E, A]] => Unit) => F[Unit] = {
      cbForCats: (Either[Throwable, Either[E, A]] => Unit) =>
        val cbForBfect: Either[E, A] => Unit = either => cbForCats(Right(either))

        cats.effect.Async[F].as(registerForBfect(cbForBfect).value, ())
    }

    EitherT(cats.effect.Async[F].asyncF[Either[E, A]](registerForCats))
  }
}

class EitherTBfectConcurrent[F[_] : cats.effect.Concurrent]
    extends EitherTBfectAsync[F]
    with Concurrent.WithBMonad[EitherT[F, *, *]] {
  private def asBfectFibre[E, A](catsFiber: cats.effect.Fiber[F, Either[E, A]]): Fibre[EitherT[F, *, *], E, A] =
    new Fibre[EitherT[F, *, *], E, A] {
      override def cancel: EitherT[F, Nothing, Unit] = EitherT.liftF(catsFiber.cancel)

      override def join: EitherT[F, E, A] = EitherT(catsFiber.join)
    }

  override def start[E, A](fea: EitherT[F, E, A]): EitherT[F, Nothing, Fibre[EitherT[F, *, *], E, A]] =
    EitherT.liftF {
      cats.effect.Concurrent[F].map(cats.effect.Concurrent[F].start[Either[E, A]](fea.value))(asBfectFibre[E, A])
    }

  override def racePair[E, A, B](
    fea: EitherT[F, E, A],
    feb: EitherT[F, E, B],
  ): EitherT[F, E, Either[(A, Fibre[EitherT[F, *, *], E, B]), (Fibre[EitherT[F, *, *], E, A], B)]] =
    CheckedException
      .unsafeRescueCheckedExceptionsFor[F, E, Either[(A, Fiber[F, B]), (Fiber[F, A], B)]](
        cats.effect
          .Concurrent[F]
          .racePair(
            CheckedException.raiseErrorsAsCheckedExceptionsFor(fea),
            CheckedException.raiseErrorsAsCheckedExceptionsFor(feb),
          ),
      )
      .map {
        case Left((a, rightCatsFiberForB)) =>
          Left((a, asBfectFibre(CheckedException.unsafeRescueCheckedExceptionsFor[F, E, B](rightCatsFiberForB))))
        case Right((leftCatsFiberForA, b)) =>
          Right((asBfectFibre(CheckedException.unsafeRescueCheckedExceptionsFor[F, E, A](leftCatsFiberForA)), b))
      }

  override def cancelable[E, A](
    registerForBfect: (Either[E, A] => Unit) => EitherT[F, Nothing, _],
  ): EitherT[F, E, A] = {
    val registerForCats: ((Either[Throwable, Either[E, A]] => Unit) => cats.effect.CancelToken[F]) = {
      cbForCats: (Either[Throwable, Either[E, A]] => Unit) =>
        val cbForBfect: (Either[E, A] => Unit) = either => cbForCats(Right(either))

        cats.effect.Concurrent[F].as(registerForBfect(cbForBfect).value, ())
    }

    EitherT(cats.effect.Concurrent[F].cancelable(registerForCats))
  }

}

object EitherTBfectConcurrent {
  private final case class CheckedException[E](e: E) extends Exception

  private object CheckedException {
    def raiseErrorsAsCheckedExceptionsFor[F[_], E, A](
      fea: EitherT[F, E, A],
    )(implicit
      monadError: MonadError[F, Throwable],
    ): F[A] =
      monadError.flatMap(fea.value) {
        case Left(e)  => monadError.raiseError(new CheckedException(e))
        case Right(a) => monadError.pure(a)
      }

    def unsafeRescueCheckedExceptionsFor[F[_], E, A](
      fa: F[A],
    )(implicit
      monadError: ApplicativeError[F, Throwable],
    ): EitherT[F, E, A] =
      EitherT {
        monadError.recover(monadError.map[A, Either[E, A]](fa)(Right(_))) {
          case CheckedException(e) => Left(e.asInstanceOf[E])
        }
      }

    def unsafeRescueCheckedExceptionsFor[F[_], E, A](
      fiber: Fiber[F, A],
    )(implicit
      monadError: ApplicativeError[F, Throwable],
    ): Fiber[F, Either[E, A]] =
      new Fiber[F, Either[E, A]] {
        override def cancel: CancelToken[F] = fiber.cancel

        override def join: F[Either[E, A]] = unsafeRescueCheckedExceptionsFor(fiber.join).value
      }
  }
}

class EitherTBfectNow[F[_]](implicit clockF: cats.effect.Clock[F], F: Functor[F]) extends Now[EitherT[F, *, *]] {
  override def now: EitherT[F, Nothing, Instant] = EitherT.liftF[F, Nothing, Instant] {
    F.map(clockF.realTime(scala.concurrent.duration.MILLISECONDS))(Instant.ofEpochMilli)
  }
}

class EitherTBfectTimer[F[_] : cats.effect.Timer : Monad]
    extends BMEInstance[F]
    with Timer.WithBMonad[EitherT[F, *, *]] {
  private val bfectNowInstance: Now[EitherT[F, *, *]] = new EitherTBfectNow[F]

  override def now: EitherT[F, Nothing, Instant] = bfectNowInstance.now

  override def sleep(duration: Duration): EitherT[F, Nothing, Unit] =
    EitherT.liftF[F, Nothing, Unit](cats.effect.Timer[F].sleep(Timer.convertJavaDurationToScalaDuration(duration)))
}
