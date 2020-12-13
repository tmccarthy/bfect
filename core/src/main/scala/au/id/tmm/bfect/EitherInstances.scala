/**
  *    Copyright 2019 Timothy McCarthy
  *
  *    Licensed under the Apache License, Version 2.0 (the "License");
  *    you may not use this file except in compliance with the License.
  *    You may obtain a copy of the License at
  *
  *        http://www.apache.org/licenses/LICENSE-2.0
  *
  *    Unless required by applicable law or agreed to in writing, software
  *    distributed under the License is distributed on an "AS IS" BASIS,
  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *    See the License for the specific language governing permissions and
  *    limitations under the License.
  */
package au.id.tmm.bfect

import scala.annotation.tailrec

object EitherInstanceImpls {

  class BiInvariantInstance extends BiInvariant[Either] {
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
  }

  class BifunctorInstance extends Bifunctor[Either] {
    override def biMap[L1, R1, L2, R2](f: Either[L1, R1])(leftF: L1 => L2, rightF: R1 => R2): Either[L2, R2] =
      f.fold(leftF.andThen(Left.apply), rightF.andThen(Right.apply))

    override def rightMap[L, R1, R2](f: Either[L, R1])(rightF: R1 => R2): Either[L, R2] = f.map(rightF)

    override def leftMap[L1, R, L2](f: Either[L1, R])(leftF: L1 => L2): Either[L2, R] = f.left.map(leftF)
  }

  class BifunctorMonadInstance extends BifunctorInstance with BifunctorMonad[Either] {
    override def rightPure[E, A](a: A): Either[E, A] = Right(a)

    override def leftPure[E, A](e: E): Either[E, A] = Left(e)

    override def flatMap[E1, E2 >: E1, A, B](fe1a: Either[E1, A])(fafe2b: A => Either[E2, B]): Either[E2, B] =
      fe1a.flatMap(fafe2b)

    @tailrec
    override final def tailRecM[E, A, A1](a: A)(f: A => Either[E, Either[A, A1]]): Either[E, A1] = f(a) match {
      case Right(Right(a)) => rightPure(a)
      case Right(Left(e))  => tailRecM(e)(f)
      case Left(e)         => Left(e)
    }
  }

  class BMEInstance extends BifunctorMonadInstance with BifunctorMonadError[Either] {
    override def handleErrorWith[E1, A, E2](fea: Either[E1, A])(f: E1 => Either[E2, A]): Either[E2, A] = fea match {
      case Right(a) => Right(a)
      case Left(e)  => f(e)
    }
  }

}

trait EitherInstances3 {
  implicit val biInvariantInstance: BiInvariant[Either] = new EitherInstanceImpls.BiInvariantInstance()
}

trait EitherInstances2 extends EitherInstances3 {
  implicit val biFunctorInstance: Bifunctor[Either] = new EitherInstanceImpls.BifunctorInstance()
}

trait EitherInstances1 extends EitherInstances2 {
  implicit val biFunctorMonadInstance: BifunctorMonad[Either] = new EitherInstanceImpls.BifunctorMonadInstance()
}

trait EitherInstances0 extends EitherInstances1 {
  implicit val bmeInstance: BifunctorMonadError[Either] = new EitherInstanceImpls.BMEInstance()
}

// TODO these aren't being resolved properly
object EitherInstances extends EitherInstances0
