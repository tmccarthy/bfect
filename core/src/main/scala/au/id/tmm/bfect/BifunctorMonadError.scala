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

import scala.util.Try

trait BifunctorMonadError[F[+_, +_]] extends BifunctorMonad[F] {

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

object BifunctorMonadError {

  def apply[F[+_, +_] : BifunctorMonadError]: BifunctorMonadError[F] = implicitly[BifunctorMonadError[F]]

  def fromEither[F[+_, +_] : BifunctorMonad, E, A](either: Either[E, A]): F[E, A] = either match {
    case Left(e)  => BifunctorMonad[F].leftPure(e)
    case Right(a) => BifunctorMonad[F].rightPure(a)
  }

  def fromTry[F[+_, +_] : BifunctorMonad, A](aTry: Try[A]): F[Throwable, A] = aTry match {
    case scala.util.Success(a) => BifunctorMonad[F].rightPure(a)
    case scala.util.Failure(e) => BifunctorMonad[F].leftPure(e)
  }

  def unit[F[+_, +_] : BifunctorMonad]: F[Nothing, Unit] = BifunctorMonad[F].rightPure(())

}
