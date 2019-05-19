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

trait BifunctorMonad[F[+_, +_]] extends Bifunctor[F] {

  def rightPure[A](a: A): F[Nothing, A]

  def pure[A](a: A): F[Nothing, A] = rightPure(a)

  def unit: F[Nothing, Unit] = rightPure(())

  def leftPure[E](e: E): F[E, Nothing]

  def fromEither[E, A](either: Either[E, A]): F[E, A] = either match {
    case Left(e)  => leftPure(e)
    case Right(a) => rightPure(a)
  }

  def fromTry[A](aTry: Try[A]): F[Throwable, A] = aTry match {
    case scala.util.Success(a) => rightPure(a)
    case scala.util.Failure(e) => leftPure(e)
  }

  def flatten[E1, E2 >: E1, A](fefa: F[E1, F[E2, A]]): F[E2, A] = flatMap[E1, E2, F[E2, A], A](fefa)(identity)

  def flatMap[E1, E2 >: E1, A, B](fe1a: F[E1, A])(fafe2b: A => F[E2, B]): F[E2, B]

  /**
    * Keeps calling `f` until a `scala.util.Right[B]` is returned.
    */
  def tailRecM[E, A, A1](a: A)(f: A => F[E, Either[A, A1]]): F[E, A1]

}

object BifunctorMonad extends BifunctorMonadStaticOps {
  def apply[F[+_, +_] : BifunctorMonad]: BifunctorMonad[F] = implicitly[BifunctorMonad[F]]

  implicit class Ops[F[+_, +_], E, A](fea: F[E, A])(implicit bifunctorMonad: BifunctorMonad[F]) extends Bifunctor.Ops[F, E, A](fea)(bifunctorMonad) {
    def flatMap[E2 >: E, B](f: A => F[E2, B]): F[E2, B] = bifunctorMonad.flatMap[E, E2, A, B](fea)(f)
  }

  implicit class FlattenOps[F[+_, +_], E1, E2 >: E1, A](fefea: F[E1, F[E2, A]])(implicit bifunctorMonad: BifunctorMonad[F]) {
    def flatten: F[E2, A] = bifunctorMonad.flatten[E1, E2, A](fefea)
  }

}

trait BifunctorMonadStaticOps {
  def rightPure[F[+_, +_] : BifunctorMonad, A](a: A): F[Nothing, A] = BifunctorMonad[F].rightPure(a)
  def pure[F[+_, +_] : BifunctorMonad, A](a: A): F[Nothing, A] = BifunctorMonad[F].pure(a)
  def leftPure[F[+_, +_] : BifunctorMonad, E](e: E): F[E, Nothing] = BifunctorMonad[F].leftPure(e)
  def fromEither[F[+_, +_] : BifunctorMonad, E, A](either: Either[E, A]): F[E, A] = BifunctorMonad[F].fromEither(either)
  def fromTry[F[+_, +_] : BifunctorMonad, A](aTry: Try[A]): F[Throwable, A] = BifunctorMonad[F].fromTry(aTry)
  def unit[F[+_, +_] : BifunctorMonad]: F[Nothing, Unit] = BifunctorMonad[F].unit
}
