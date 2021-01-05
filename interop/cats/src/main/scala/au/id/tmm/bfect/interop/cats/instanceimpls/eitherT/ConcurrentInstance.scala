package au.id.tmm.bfect.interop.cats.instanceimpls.eitherT

import au.id.tmm.bfect._
import ConcurrentInstance.CheckedException
import au.id.tmm.bfect.effects.Concurrent
import cats.data.EitherT
import cats.effect.{CancelToken, Fiber}
import cats.{ApplicativeError, MonadError}

class ConcurrentInstance[F[_] : cats.effect.Concurrent] private[instanceimpls]
    extends AsyncInstance[F]
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

object ConcurrentInstance {
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
