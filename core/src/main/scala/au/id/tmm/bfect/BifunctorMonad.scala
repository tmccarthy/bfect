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

  def fromOption[E, A](option: Option[A], ifNone: => E): F[E, A] = option match {
    case Some(a) => rightPure(a)
    case None    => leftPure(ifNone)
  }

  def fromEither[E, A](either: Either[E, A]): F[E, A] = either match {
    case Left(e)  => leftPure(e)
    case Right(a) => rightPure(a)
  }

  def fromTry[A](aTry: Try[A]): F[Throwable, A] = aTry match {
    case scala.util.Success(a) => rightPure(a)
    case scala.util.Failure(e) => leftPure(e)
  }

  def pureCatch[E, A](block: => A)(catchPf: PartialFunction[Throwable, E]): F[E, A] =
    try {
      rightPure(block): F[E, A]
    } catch {
      catchPf.andThen(leftPure(_): F[E, A])
    }

  def pureCatchException[A](block: => A): F[Exception, A] = pureCatch(block) {
    case e: Exception => e
  }

  def pureCatchThrowable[A](block: => A): F[Throwable, A] = pureCatch(block) {
    case t: Throwable => t
  }

  def flatten[E1, E2 >: E1, A](fefa: F[E1, F[E2, A]]): F[E2, A] = flatMap[E1, E2, F[E2, A], A](fefa)(identity)

  def flatMap[E1, E2 >: E1, A, B](fe1a: F[E1, A])(fafe2b: A => F[E2, B]): F[E2, B]

  def forever[E](fea: F[E, _]): F[E, Nothing] = flatMap(fea)(_ => forever(fea))

  /**
    * Keeps calling `f` until a `scala.util.Right[B]` is returned.
    */
  def tailRecM[E, A, A1](a: A)(f: A => F[E, Either[A, A1]]): F[E, A1]

  def unit[E, A](fea: F[E, A]): F[E, Unit] = flatMap(fea)(_ => unit)

  def absolve[E, A](fEitherEA: F[E, Either[E, A]]): F[E, A] = flatMap(fEitherEA)(fromEither)

  def absolveOption[E, A](feOptionA: F[E, Option[A]], ifNone: => E): F[E, A] = flatMap(feOptionA)(fromOption(_, ifNone))

}

object BifunctorMonad extends BifunctorMonadStaticOps {
  def apply[F[+_, +_] : BifunctorMonad]: BifunctorMonad[F] = implicitly[BifunctorMonad[F]]

  implicit class Ops[F[+_, +_], E, A](fea: F[E, A])(implicit bifunctorMonad: BifunctorMonad[F]) extends Bifunctor.Ops[F, E, A](fea)(bifunctorMonad) {
    def flatMap[E2 >: E, B](f: A => F[E2, B]): F[E2, B] = bifunctorMonad.flatMap[E, E2, A, B](fea)(f)
    def forever: F[E, Nothing] = bifunctorMonad.forever(fea)
    def unit: F[E, Unit] = bifunctorMonad.unit(fea)
  }

  implicit class FlattenOps[F[+_, +_], E1, E2 >: E1, A](fefea: F[E1, F[E2, A]])(implicit bifunctorMonad: BifunctorMonad[F]) {
    def flatten: F[E2, A] = bifunctorMonad.flatten[E1, E2, A](fefea)
  }

  implicit class AbsolveOps[F[+_, +_], E, A](fEitherEA: F[E, Either[E, A]])(implicit bMonad: BMonad[F]) {
    def absolve: F[E, A] = bMonad.absolve(fEitherEA)
  }

  implicit class AbsolveOptionOps[F[+_, +_], E, A](feOptionA: F[E, Option[A]])(implicit bMonad: BMonad[F]) {
    def absolveOption[E2 >: E](ifNone: => E2): F[E2, A] = bMonad.absolveOption(feOptionA, ifNone)
  }

}

trait BifunctorMonadStaticOps {
  def rightPure[F[+_, +_] : BifunctorMonad, A](a: A): F[Nothing, A] = BifunctorMonad[F].rightPure(a)
  def pure[F[+_, +_] : BifunctorMonad, A](a: A): F[Nothing, A] = BifunctorMonad[F].pure(a)
  def leftPure[F[+_, +_] : BifunctorMonad, E](e: E): F[E, Nothing] = BifunctorMonad[F].leftPure(e)
  def fromEither[F[+_, +_] : BifunctorMonad, E, A](either: Either[E, A]): F[E, A] = BifunctorMonad[F].fromEither(either)
  def fromOption[F[+_, +_] : BifunctorMonad, E, A](option: Option[A], ifNone: => E): F[E, A] = BifunctorMonad[F].fromOption(option, ifNone)
  def fromTry[F[+_, +_] : BifunctorMonad, A](aTry: Try[A]): F[Throwable, A] = BifunctorMonad[F].fromTry(aTry)
  def pureCatch[F[+_, +_] : BifunctorMonad, E, A](block: => A)(catchPf: PartialFunction[Throwable, E]): F[E, A] = BifunctorMonad[F].pureCatch(block)(catchPf)
  def pureCatchException[F[+_, +_] : BifunctorMonad, A](block: => A): F[Exception, A] = BifunctorMonad[F].pureCatchException(block)
  def pureCatchThrowable[F[+_, +_] : BifunctorMonad, A](block: => A): F[Throwable, A] = BifunctorMonad[F].pureCatchThrowable(block)

  def unit[F[+_, +_] : BifunctorMonad]: F[Nothing, Unit] = BifunctorMonad[F].unit
}
