package au.id.tmm.bifunctorio.typeclasses

import scala.util.{Failure, Success, Try}

trait BiFunctorMonadError[F[+_, +_]] extends BiFunctorMonad[F] {

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

object BiFunctorMonadError {

  def apply[F[+_, +_] : BiFunctorMonadError]: BiFunctorMonadError[F] = implicitly[BiFunctorMonadError[F]]

  def fromEither[F[+_, +_] : BiFunctorMonad, E, A](either: Either[E, A]): F[E, A] = either match {
    case Left(e)  => BiFunctorMonad[F].leftPure(e)
    case Right(a) => BiFunctorMonad[F].rightPure(a)
  }

  def fromTry[F[+_, +_] : BiFunctorMonad, A](aTry: Try[A]): F[Throwable, A] = aTry match {
    case Success(a) => BiFunctorMonad[F].rightPure(a)
    case Failure(e) => BiFunctorMonad[F].leftPure(e)
  }

  def unit[F[+_, +_] : BiFunctorMonad]: F[Nothing, Unit] = BiFunctorMonad[F].rightPure(())

}
