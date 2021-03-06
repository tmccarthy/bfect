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

import au.id.tmm.bfect.{BME, BifunctorMonadErrorStaticOps}

trait Die[F[_, _]] extends BME[F] {

  def failUnchecked(t: Throwable): F[Nothing, Nothing]

  def die(t: Throwable): F[Nothing, Nothing] = failUnchecked(t)

  def orDie[E, A](fea: F[E, A])(implicit ev: E <:< Throwable): F[Nothing, A] =
    handleErrorWith[E, A, Nothing](fea)(e => biWiden(die(e)))

  //noinspection ConvertibleToMethodValue
  def refineOrDie[E1, A, E2](
    fea: F[E1, A],
  )(
    refinePf: PartialFunction[E1, E2],
  )(implicit
    ev: E1 <:< Throwable,
  ): F[E2, A] =
    handleErrorWith[E1, A, E2](fea) { e =>
      refinePf.andThen(e2 => leftPure[E2, A](e2)).applyOrElse(e, (t: E1) => biWiden(die(t)))
    }

  def refineToExceptionOrDie[E, A](fea: F[E, A])(implicit ev: E <:< Throwable): F[Exception, A] = refineOrDie(fea) {
    case e: Exception => e
  }

}

object Die extends DieStaticOps {
  def apply[F[_, _] : Die]: Die[F] = implicitly[Die[F]]

  trait ToDieOps {
    implicit def toDieOps[F[_, _], E, A](fea: F[E, A])(implicit die: Die[F]): Ops[F, E, A] =
      new Ops[F, E, A](fea)

    implicit def toDieOpsErrorNothing[F[_, _], A](fea: F[Nothing, A])(implicit die: Die[F]): Ops[F, Nothing, A] =
      new Ops[F, Nothing, A](fea)

    implicit def toDieOpsValueNothing[F[_, _], E](fea: F[E, Nothing])(implicit die: Die[F]): Ops[F, E, Nothing] =
      new Ops[F, E, Nothing](fea)

    implicit def toDieOpsErrorNothingValueNothing[F[_, _]](
      fea: F[Nothing, Nothing],
    )(implicit
      die: Die[F],
    ): Ops[F, Nothing, Nothing] =
      new Ops[F, Nothing, Nothing](fea)
  }

  final class Ops[F[_, _], E, A](fea: F[E, A])(implicit die: Die[F]) {
    def orDie(implicit ev: E <:< Throwable): F[Nothing, A] = die.orDie(fea)
    def refineOrDie[E2](refinePf: PartialFunction[E, E2])(implicit ev: E <:< Throwable): F[E2, A] =
      die.refineOrDie[E, A, E2](fea)(refinePf)
    def refineToExceptionOrDie(implicit ev: E <:< Throwable): F[Exception, A] = die.refineToExceptionOrDie(fea)
  }
}

trait DieStaticOps extends BifunctorMonadErrorStaticOps {
  def failUnchecked[F[_, _] : Die](t: Throwable): F[Nothing, Nothing] = Die[F].failUnchecked(t)
  def die[F[_, _] : Die](t: Throwable): F[Nothing, Nothing]           = Die[F].die(t)
}
