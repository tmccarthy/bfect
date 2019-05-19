package au.id.tmm.bfect

import scala.util.Try

trait BifunctorMonadError[F[+_, +_]] extends BifunctorMonad[F] {

  def handleErrorWith[E1, A, E2](fea: F[E1, A])(f: E1 => F[E2, A]): F[E2, A]

  def recoverWith[E1, A, E2 >: E1](fea: F[E1, A])(catchPf: PartialFunction[E1, F[E2, A]]): F[E2, A] = {
    val totalHandler: E1 => F[E2, A] = catchPf.orElse {
      case e => leftPure(e)
    }

    handleErrorWith(fea)(totalHandler)
  }

  def attempt[E, A](fea: F[E, A]): F[Nothing, Either[E, A]] =
    handleErrorWith {
      rightMap(fea)(a => Right(a): Either[E, A])
    } { e =>
      rightPure(Left(e): Either[E, A])
    }

}

object BifunctorMonadError {

  def apply[F[+_, +_] : BifunctorMonadError]: BifunctorMonadError[F] = implicitly[BifunctorMonadError[F]]

  def fromEither[F[+_, +_] : BifunctorMonad, E, A](either: Either[E, A]): F[E, A] = either match {
    case Left(e)  => BifunctorMonad[F].leftPure(e)
    case Right(a) => BifunctorMonad[F].rightPure(a)
  }

  def fromTry[F[+_, +_] : BifunctorMonad, A](aTry: Try[A]): F[Throwable, A] = aTry match {
    case scala.util.Success(a) => BifunctorMonad[F].rightPure(a)
    case scala.util.Failure(e) => BifunctorMonad[F].leftPure(e)
  }

  def unit[F[+_, +_] : BifunctorMonad]: F[Nothing, Unit] = BifunctorMonad[F].rightPure(())

}
