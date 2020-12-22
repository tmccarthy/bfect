package au.id.tmm.bfect.interop.cats.instanceimpls.implicitconversions

import au.id.tmm.bfect.BME

class CatsMonadErrorForBfectBME[F[_, _], E](implicit bfectBme: BME[F]) extends cats.MonadError[F[E, *], E] {
  override def flatMap[A, A1](fea: F[E, A])(f: A => F[E, A1]): F[E, A1] = bfectBme.flatMap[E, E, A, A1](fea)(f)

  override def tailRecM[A, A1](a: A)(f: A => F[E, Either[A, A1]]): F[E, A1] = bfectBme.tailRecM[E, A, A1](a)(f)

  override def pure[A](a: A): F[E, A] = bfectBme.rightPure(a)

  override def raiseError[A](e: E): F[E, A] = bfectBme.leftPure(e)

  override def handleErrorWith[A](fea: F[E, A])(f: E => F[E, A]): F[E, A] = bfectBme.handleErrorWith(fea)(f)
}

object CatsMonadErrorForBfectBME {
  trait ToCatsMonadError {
    implicit def bfectBifunctorMonadErrorIsCatsMonadError[F[_, _] : BME, E]: cats.MonadError[F[E, *], E] =
      new CatsMonadErrorForBfectBME[F, E]()
  }
}
