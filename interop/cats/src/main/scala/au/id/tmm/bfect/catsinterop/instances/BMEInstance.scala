package au.id.tmm.bfect.catsinterop.instances

import au.id.tmm.bfect.BifunctorMonadError
import cats.Monad
import cats.data.EitherT

class BMEInstance[F[_]] private[instances] (implicit monad: Monad[F])
    extends BifunctorInstance[F]
    with BifunctorMonadError[EitherT[F, *, *]] {
  override def flatMap[E1, E2 >: E1, A, B](
    fe1a: EitherT[F, E1, A],
  )(
    fafe2b: A => EitherT[F, E2, B],
  ): EitherT[F, E2, B] = fe1a.flatMap(fafe2b)

  override final def tailRecM[E, A, A1](a: A)(f: A => EitherT[F, E, Either[A, A1]]): EitherT[F, E, A1] =
    Monad[EitherT[F, E, *]].tailRecM(a)(f)

  def rightPure[E, A](a: A): EitherT[F, E, A] = EitherT.rightT[F, E](a)

  def leftPure[E, A](e: E): EitherT[F, E, A] = EitherT.leftT[F, A](e)

  override def handleErrorWith[E1, A, E2](fea: EitherT[F, E1, A])(f: E1 => EitherT[F, E2, A]): EitherT[F, E2, A] =
    fea.leftFlatMap(f)
}
