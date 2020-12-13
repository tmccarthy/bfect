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

import au.id.tmm.bfect.syntax.≈>

trait BifunctorMonadError[F[_, _]] extends BifunctorMonad[F] {

  def raiseError[E, A](e: E): F[E, A] = leftPure(e)

  def handleErrorWith[E1, A, E2](fea: F[E1, A])(f: E1 => F[E2, A]): F[E2, A]

  def recoverWith[E1, A, E2 >: E1](fea: F[E1, A])(catchPf: PartialFunction[E1, F[E2, A]]): F[E2, A] = {
    val totalHandler: E1 => F[E2, A] = catchPf.orElse {
      case e => leftPure(e)
    }

    handleErrorWith(fea)(totalHandler)
  }

  def catchLeft[E1, A, E2 >: E1](fea: F[E1, A])(catchPf: PartialFunction[E1, F[E2, A]]): F[E2, A] =
    recoverWith[E1, A, E2](fea)(catchPf)

  def attempt[E, A](fea: F[E, A]): F[Nothing, Either[E, A]] =
    handleErrorWith[E, Either[E, A], Nothing] {
      rightMap(fea)(a => Right(a): Either[E, A])
    } { e =>
      rightPure(Left(e): Either[E, A])
    }

}

object BifunctorMonadError extends BifunctorMonadErrorStaticOps {

  def apply[F[_, _] : BifunctorMonadError]: BifunctorMonadError[F] = implicitly[BifunctorMonadError[F]]

  implicit class Ops[F[_, _], E, A](fea: F[E, A])(implicit bme: BME[F]) extends BifunctorMonad.Ops[F, E, A](fea) {
    def attempt: F[Nothing, Either[E, A]]                                     = bme.attempt(fea)
    def handleErrorWith[E2 >: E](f: E => F[E2, A]): F[E2, A]                  = bme.handleErrorWith[E, A, E2](fea)(f)
    def recoverWith[E2 >: E](catchPf: PartialFunction[E, F[E2, A]]): F[E2, A] = bme.recoverWith[E, A, E2](fea)(catchPf)
    def catchLeft[E2 >: E](catchPf: PartialFunction[E, F[E2, A]]): F[E2, A]   = bme.catchLeft[E, A, E2](fea)(catchPf)
  }

  implicit val bifunctorMonadErrorBiInvariantK: BiInvariantK[BifunctorMonadError] =
    new BiInvariantK[BifunctorMonadError] {
      override def biImapK[F[_, _], G[_, _]](F: BME[F])(fFG: F ≈> G)(fGF: G ≈> F): BME[G] = new BME[G] {
        override def rightPure[E, A](a: A): G[E, A] = fFG(F.rightPure(a))

        override def leftPure[E, A](e: E): G[E, A] = fFG(F.leftPure(e))

        override def flatMap[E1, E2 >: E1, A, B](ge1a: G[E1, A])(fage2b: A => G[E2, B]): G[E2, B] =
          fFG(F.flatMap[E1, E2, A, B](fGF(ge1a))(fage2b.andThen(ge2b => fGF(ge2b))))

        override def tailRecM[E, A, A1](a: A)(f: A => G[E, Either[A, A1]]): G[E, A1] =
          fFG(F.tailRecM(a)(f.andThen { g: G[E, Either[A, A1]] =>
            fGF(g)
          }))

        override def biMap[L1, R1, L2, R2](g: G[L1, R1])(leftF: L1 => L2, rightF: R1 => R2): G[L2, R2] =
          fFG(F.biMap(fGF(g))(leftF, rightF))

        override def handleErrorWith[E1, A, E2](gea: G[E1, A])(f: E1 => G[E2, A]): G[E2, A] =
          fFG(F.handleErrorWith[E1, A, E2](fGF(gea))(f.andThen(g => fGF(g))))
      }
    }

}

trait BifunctorMonadErrorStaticOps extends BifunctorMonadStaticOps {
  def raiseError[F[_, _] : BME, E, A](e: E): F[E, A] = BME[F].raiseError(e)
}
