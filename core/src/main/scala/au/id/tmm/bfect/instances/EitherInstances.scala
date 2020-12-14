package au.id.tmm.bfect.instances

import au.id.tmm.bfect.BifunctorMonadError

import scala.annotation.tailrec

trait EitherInstances {

  implicit val bfectBifunctorMonadErrorForEither: BifunctorMonadError[Either] = new EitherBMEInstance

}

class EitherBMEInstance extends BifunctorMonadError[Either] {
  override def biImap[L1, L2, R1, R2](
    fl1r1: Either[L1, R1],
  )(
    fl1l2: L1 => L2,
    fr1r2: R1 => R2,
  )(
    fl2l1: L2 => L1,
    fr2r1: R2 => R1,
  ): Either[L2, R2] =
    fl1r1 match {
      case Left(l1)  => Left(fl1l2(l1))
      case Right(r1) => Right(fr1r2(r1))
    }
  override def biMap[L1, R1, L2, R2](f: Either[L1, R1])(leftF: L1 => L2, rightF: R1 => R2): Either[L2, R2] =
    f.fold(leftF.andThen(Left.apply), rightF.andThen(Right.apply))

  override def rightMap[L, R1, R2](f: Either[L, R1])(rightF: R1 => R2): Either[L, R2] = f.map(rightF)

  override def leftMap[L1, R, L2](f: Either[L1, R])(leftF: L1 => L2): Either[L2, R] = f.left.map(leftF)
  override def rightPure[E, A](a: A): Either[E, A]                                  = Right(a)

  override def leftPure[E, A](e: E): Either[E, A] = Left(e)

  override def flatMap[E1, E2 >: E1, A, B](fe1a: Either[E1, A])(fafe2b: A => Either[E2, B]): Either[E2, B] =
    fe1a.flatMap(fafe2b)

  @tailrec
  override final def tailRecM[E, A, A1](a: A)(f: A => Either[E, Either[A, A1]]): Either[E, A1] = f(a) match {
    case Right(Right(a)) => rightPure(a)
    case Right(Left(e))  => tailRecM(e)(f)
    case Left(e)         => Left(e)
  }

  override def handleErrorWith[E1, A, E2](fea: Either[E1, A])(f: E1 => Either[E2, A]): Either[E2, A] = fea match {
    case Right(a) => Right(a)
    case Left(e)  => f(e)
  }
}
