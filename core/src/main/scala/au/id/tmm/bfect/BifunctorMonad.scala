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

import scala.util.Try

trait BifunctorMonad[F[_, _]] extends Bifunctor[F] {

  def rightPure[E, A](a: A): F[E, A]

  def pure[E, A](a: A): F[E, A] = rightPure(a)

  def unit[E]: F[E, Unit] = rightPure(())

  def leftPure[E, A](e: E): F[E, A]

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

  def forever[E, A](fea: F[E, A]): F[E, Nothing] = flatMap[E, E, A, Nothing](fea) { a =>
    tailRecM[E, A, Nothing](a) { _ =>
      map(fea)(Left.apply)
    }
  }

  /**
    * Keeps calling `f` until a `scala.util.Right[B]` is returned.
    */
  def tailRecM[E, A, A1](a: A)(f: A => F[E, Either[A, A1]]): F[E, A1]

  def absolve[E, E2 >: E, A](fEitherEA: F[E, Either[E2, A]]): F[E2, A] =
    flatMap[E, E2, Either[E2, A], A](fEitherEA)(fromEither)

  def absolveOption[E, A](feOptionA: F[E, Option[A]], ifNone: => E): F[E, A] = flatMap(feOptionA)(fromOption(_, ifNone))

}

object BifunctorMonad extends BifunctorMonadStaticOps {
  def apply[F[_, _] : BifunctorMonad]: BifunctorMonad[F] = implicitly[BifunctorMonad[F]]

  trait ToBifunctorMonadOps {
    implicit def toBifunctorMonadOps[F[_, _], E, A](
      fea: F[E, A],
    )(implicit
      bifunctorMonad: BifunctorMonad[F],
    ): Ops[F, E, A] =
      new Ops(fea)

    implicit def toBifunctorMonadFlattenOps[F[_, _], E1, E2 >: E1, A](
      fefea: F[E1, F[E2, A]],
    )(implicit
      bifunctorMonad: BifunctorMonad[F],
    ): FlattenOps[F, E1, E2, A] =
      new FlattenOps[F, E1, E2, A](fefea)

    implicit def toBifunctorMonadAbsolveOps[F[_, _], E, E2 >: E, A](
      fEitherEA: F[E, Either[E2, A]],
    )(implicit
      bMonad: BMonad[F],
    ): AbsolveOps[F, E, E2, A] =
      new AbsolveOps[F, E, E2, A](fEitherEA)

    implicit def toBifunctorMonadAbsolveNothingErrorOps[F[_, _], E2, A](
      fEitherEA: F[Nothing, Either[E2, A]],
    )(implicit
      bMonad: BMonad[F],
    ): AbsolveOps[F, Nothing, E2, A] =
      new AbsolveOps[F, Nothing, E2, A](fEitherEA)

    implicit def toBifunctorMonadAbsolveNothingValueOps[F[_, _], E, E2 >: E](
      fEitherEA: F[E, Either[E2, Nothing]],
    )(implicit
      bMonad: BMonad[F],
    ): AbsolveOps[F, E, E2, Nothing] =
      new AbsolveOps[F, E, E2, Nothing](fEitherEA)

    implicit def toBifunctorMonadAbsolveOptionOps[F[_, _], E, A](
      feOptionA: F[E, Option[A]],
    )(implicit
      bMonad: BMonad[F],
    ): AbsolveOptionOps[F, E, A] =
      new AbsolveOptionOps[F, E, A](feOptionA)
  }

  final class Ops[F[_, _], E, A](fea: F[E, A])(implicit bifunctorMonad: BifunctorMonad[F]) {
    def flatMap[E2 >: E, B](f: A => F[E2, B]): F[E2, B] = bifunctorMonad.flatMap[E, E2, A, B](fea)(f)
    def forever: F[E, Nothing]                          = bifunctorMonad.forever(fea)
  }

  final class FlattenOps[F[_, _], E1, E2 >: E1, A](
    fefea: F[E1, F[E2, A]],
  )(implicit
    bifunctorMonad: BifunctorMonad[F],
  ) {
    def flatten: F[E2, A] = bifunctorMonad.flatten[E1, E2, A](fefea)
  }

  final class AbsolveOps[F[_, _], E, E2 >: E, A](fEitherEA: F[E, Either[E2, A]])(implicit bMonad: BMonad[F]) {
    def absolve: F[E2, A] = bMonad.absolve(fEitherEA)
  }

  final class AbsolveOptionOps[F[_, _], E, A](feOptionA: F[E, Option[A]])(implicit bMonad: BMonad[F]) {
    def absolveOption[E2 >: E](ifNone: => E2): F[E2, A] =
      bMonad.absolveOption[E2, A](bMonad.leftWiden(feOptionA), ifNone)
  }

  implicit val bifunctorMonadBiInvariantK: BiInvariantK[BifunctorMonad] = new BiInvariantK[BMonad] {
    override def biImapK[F[_, _], G[_, _]](F: BMonad[F])(fFG: F ≈> G)(fGF: G ≈> F): BMonad[G] = new BMonad[G] {
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
    }
  }

}

trait BifunctorMonadStaticOps {
  def rightPure[F[_, _] : BifunctorMonad, E, A](a: A): F[E, A]                  = BifunctorMonad[F].rightPure(a)
  def pure[F[_, _] : BifunctorMonad, E, A](a: A): F[E, A]                       = BifunctorMonad[F].pure(a)
  def leftPure[F[_, _] : BifunctorMonad, E, A](e: E): F[E, A]                   = BifunctorMonad[F].leftPure(e)
  def fromEither[F[_, _] : BifunctorMonad, E, A](either: Either[E, A]): F[E, A] = BifunctorMonad[F].fromEither(either)
  def fromOption[F[_, _] : BifunctorMonad, E, A](option: Option[A], ifNone: => E): F[E, A] =
    BifunctorMonad[F].fromOption(option, ifNone)
  def fromTry[F[_, _] : BifunctorMonad, A](aTry: Try[A]): F[Throwable, A] = BifunctorMonad[F].fromTry(aTry)
  def pureCatch[F[_, _] : BifunctorMonad, E, A](block: => A)(catchPf: PartialFunction[Throwable, E]): F[E, A] =
    BifunctorMonad[F].pureCatch(block)(catchPf)
  def pureCatchException[F[_, _] : BifunctorMonad, A](block: => A): F[Exception, A] =
    BifunctorMonad[F].pureCatchException(block)
  def pureCatchThrowable[F[_, _] : BifunctorMonad, A](block: => A): F[Throwable, A] =
    BifunctorMonad[F].pureCatchThrowable(block)

  def unit[F[_, _] : BifunctorMonad]: F[Nothing, Unit] = BifunctorMonad[F].unit
}
