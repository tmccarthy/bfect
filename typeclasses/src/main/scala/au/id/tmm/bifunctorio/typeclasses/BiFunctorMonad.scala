package au.id.tmm.bifunctorio.typeclasses

import simulacrum.typeclass

@typeclass trait BiFunctorMonad[F[+_, +_]] extends BiFunctor[F] {

  def rightPure[A](a: A): F[Nothing, A]

  def leftPure[E](e: E): F[E, Nothing]

  def flatten[E1, E2 >: E1, A](fefa: F[E1, F[E2, A]]): F[E2, A] = flatMap[E1, E2, F[E2, A], A](fefa)(identity)

  def flatMap[E1, E2 >: E1, A, B](fe1a: F[E1, A])(fafe2b: A => F[E2, B]): F[E2, B]

  /**
    * Keeps calling `f` until a `scala.util.Right[B]` is returned.
    */
  def tailRecM[E, A, A1](a: A)(f: A => F[E, Either[A, A1]]): F[E, A1]

}
