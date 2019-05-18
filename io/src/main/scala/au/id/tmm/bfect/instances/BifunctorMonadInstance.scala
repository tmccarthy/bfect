package au.id.tmm.bfect.instances

import au.id.tmm.bfect.IO
import au.id.tmm.bfect.typeclasses.BifunctorMonad

class BifunctorMonadInstance private[instances]() extends BifunctorInstance with BifunctorMonad[IO] {

  override def rightPure[A](a: A): IO[Nothing, A] = IO.pure(a)

  override def leftPure[E](e: E): IO[E, Nothing] = IO.leftPure(e)

  override def flatMap[E1, E2 >: E1, A, B](fe1a: IO[E1, A])(fafe2b: A => IO[E2, B]): IO[E2, B] = fe1a.flatMap(fafe2b)

  /**
    * Keeps calling `f` until a `scala.util.Right[B]` is returned.
    */
  override def tailRecM[E, A, A1](a: A)(f: A => IO[E, Either[A, A1]]): IO[E, A1] = f(a).flatMap {
    case Right(a1) => IO.pure(a1)
    case Left(a)   => tailRecM(a)(f)
  }

}
