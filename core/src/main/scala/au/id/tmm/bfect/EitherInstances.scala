package au.id.tmm.bfect

import scala.annotation.tailrec

private object EitherInstanceImpls {

  class BifunctorInstance private[bfect] () extends Bifunctor[Either] {
    override def biMap[L1, R1, L2, R2](f: Either[L1, R1])(leftF: L1 => L2, rightF: R1 => R2): Either[L2, R2] =
      f.fold(leftF.andThen(Left.apply), rightF.andThen(Right.apply))

    override def rightMap[L, R1, R2](f: Either[L, R1])(rightF: R1 => R2): Either[L, R2] = f.map(rightF)

    override def leftMap[L1, R, L2](f: Either[L1, R])(leftF: L1 => L2): Either[L2, R] = f.left.map(leftF)
  }

  class BifunctorMonadInstance private[bfect] () extends BifunctorInstance with BifunctorMonad[Either] {
    override def rightPure[A](a: A): Either[Nothing, A] = Right(a)

    override def leftPure[E](e: E): Either[E, Nothing] = Left(e)

    override def flatMap[E1, E2 >: E1, A, B](fe1a: Either[E1, A])(fafe2b: A => Either[E2, B]): Either[E2, B] = fe1a.flatMap(fafe2b)

    @tailrec
    override final def tailRecM[E, A, A1](a: A)(f: A => Either[E, Either[A, A1]]): Either[E, A1] = f(a) match {
      case Right(Right(a)) => rightPure(a)
      case Right(Left(e)) => tailRecM(e)(f)
      case Left(e) => Left(e)
    }
  }

  class BMEInstance private[bfect]() extends BifunctorMonadInstance with BifunctorMonadError[Either] {
    override def handleErrorWith[E1, A, E2](fea: Either[E1, A])(f: E1 => Either[E2, A]): Either[E2, A] = fea match {
      case Right(a) => Right(a)
      case Left(e) => f(e)
    }
  }

}

trait LowPriorityEitherInstances {
  implicit val biFunctorInstance: Bifunctor[Either] = new EitherInstanceImpls.BifunctorInstance()
}

trait MiddlePriorityEitherInstances extends LowPriorityEitherInstances {
  implicit val biFunctorMonadInstance: BifunctorMonad[Either] = new EitherInstanceImpls.BifunctorMonadInstance()
}

trait HighPriorityEitherInstances extends MiddlePriorityEitherInstances {
  implicit val bmeInstance: BifunctorMonadError[Either] = new EitherInstanceImpls.BMEInstance()
}

object EitherInstances extends HighPriorityEitherInstances

