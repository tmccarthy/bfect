package au.id.tmm.bfect.interop.cats.instanceimpls.implicitconversions

import au.id.tmm.bfect.BifunctorMonad

class CatsMonadForBfectBifunctorMonad[F[_, _], E](implicit bfectBifunctorMonad: BifunctorMonad[F])
    extends cats.Monad[F[E, *]] {
  override def flatMap[A, A1](fea: F[E, A])(f: A => F[E, A1]): F[E, A1] =
    bfectBifunctorMonad.flatMap[E, E, A, A1](fea)(f)

  override def tailRecM[A, A1](a: A)(f: A => F[E, Either[A, A1]]): F[E, A1] =
    bfectBifunctorMonad.tailRecM[E, A, A1](a)(f)

  override def pure[A](a: A): F[E, A] = bfectBifunctorMonad.rightPure(a)
}

object CatsMonadForBfectBifunctorMonad {
  trait ToCatsMonad {
    implicit def bfectBifunctorMonadIsCatsMonad[F[_, _] : BifunctorMonad, E]: cats.Monad[F[E, *]] =
      new CatsMonadForBfectBifunctorMonad[F, E]()
  }
}
