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
package au.id.tmm.bfect.effects

import au.id.tmm.bfect.effects.Bracket.{PartialAcquire, PartialCaseAcquire}
import au.id.tmm.bfect.{BMonad, ExitCase}

trait Bracket[F[_, _]] {

  def bracketCase[R, E, A](
    acquire: F[E, R],
    release: (R, ExitCase[E, A]) => F[Nothing, _],
    use: R => F[E, A],
  ): F[E, A]

  /**
    * Returns a curried form of `bracketCase` which has better type inference.
    */
  def bracketCase[R, E](acquire: F[E, R]): PartialCaseAcquire[F, R, E] = Bracket.bracketCase(acquire)

  def bracket[R, E, A](
    acquire: F[E, R],
    release: R => F[Nothing, _],
    use: R => F[E, A],
  ): F[E, A] =
    bracketCase[R, E, A](acquire, { case (resource, exitCase) => release(resource) }, use)

  /**
    * Returns a curried form of `bracket` which has better type inference
    */
  def bracket[R, E](acquire: F[E, R]): PartialAcquire[F, R, E] = Bracket.bracket(acquire)

  def ensure[E, A](fea: F[E, A])(finalizer: F[Nothing, _]): F[E, A]

  def ensureCase[E, A](fea: F[E, A])(finalizer: ExitCase[E, A] => F[Nothing, _]): F[E, A]

}

object Bracket extends BracketStaticOps {
  def apply[F[_, _] : Bracket]: Bracket[F] = implicitly[Bracket[F]]

  trait WithBMonad[F[_, _]] extends Bracket[F] { self: BMonad[F] =>
    override def ensure[E, A](fea: F[E, A])(finalizer: F[Nothing, _]): F[E, A] =
      bracket[Unit, E, A](rightPure(()), _ => finalizer, _ => fea)

    override def ensureCase[E, A](fea: F[E, A])(finalizer: ExitCase[E, A] => F[Nothing, _]): F[E, A] =
      bracketCase[Unit, E, A](rightPure(()), { case (resource, exitCase) => finalizer(exitCase) }, _ => fea)
  }

  implicit class Ops[F[_, _], E, A](fea: F[E, A])(implicit bracket: Bracket[F]) {
    def ensure(finalizer: F[Nothing, _]): F[E, A]                       = bracket.ensure(fea)(finalizer)
    def ensureCase(finalizer: ExitCase[E, A] => F[Nothing, _]): F[E, A] = bracket.ensureCase(fea)(finalizer)
  }

  final class PartialCaseAcquire[F[_, _], R, E] private[effects] (acquire: F[E, R]) {
    def apply[A](release: (R, ExitCase[E, A]) => F[Nothing, _]): PartialCaseAcquireRelease[F, R, E, A] =
      new PartialCaseAcquireRelease[F, R, E, A](acquire, release)
  }

  final class PartialCaseAcquireRelease[F[_, _], R, E, A] private[effects] (
    acquire: F[E, R],
    release: (R, ExitCase[E, A]) => F[Nothing, _],
  ) {
    def apply(use: R => F[E, A])(bracket: Bracket[F]): F[E, A] = bracket.bracketCase(acquire, release, use)
  }

  final class PartialAcquire[F[_, _], R, E] private[effects] (acquire: F[E, R]) {
    def apply(release: R => F[Nothing, _]): PartialAcquireRelease[F, R, E] =
      new PartialAcquireRelease[F, R, E](acquire, release)
  }

  final class PartialAcquireRelease[F[_, _], R, E] private[effects] (acquire: F[E, R], release: R => F[Nothing, _]) {
    def apply[A](use: R => F[E, A])(implicit bracket: Bracket[F]): F[E, A] =
      bracket.bracket[R, E, A](acquire, release, use)
  }

}

trait BracketStaticOps {

  def bracketCase[F[_, _] : Bracket, R, E, A](
    acquire: F[E, R],
    release: (R, ExitCase[E, A]) => F[Nothing, _],
    use: R => F[E, A],
  ): F[E, A] = Bracket[F].bracketCase[R, E, A](acquire, release, use)

  def bracket[F[_, _] : Bracket, R, E, A](
    acquire: F[E, R],
    release: R => F[Nothing, _],
    use: R => F[E, A],
  ): F[E, A] = Bracket[F].bracket[R, E, A](acquire, release, use)

  /**
    * Returns a curried form of `bracketCase` which has better type inference.
    */
  def bracketCase[F[_, _], R, E](acquire: F[E, R]): PartialCaseAcquire[F, R, E] =
    new PartialCaseAcquire[F, R, E](acquire)

  /**
    * Returns a curried form of `bracket` which has better type inference
    */
  def bracket[F[_, _], R, E](acquire: F[E, R]): PartialAcquire[F, R, E] =
    new PartialAcquire[F, R, E](acquire)

}
