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

trait Sync[F[+_, +_]] extends Bracket[F] {

  def suspend[E, A](effect: => F[E, A]): F[E, A]

  def failUnchecked(t: Throwable): F[Nothing, Nothing] = suspend(throw t)

  def die(t: Throwable): F[Nothing, Nothing] = failUnchecked(t)

  def orDie[E, A](fea: F[E, A])(implicit ev: E <:< Throwable): F[Nothing, A] = handleErrorWith[E, A, Nothing](fea)(die(_))

  def refineOrDie[E1, A, E2](fea: F[E1, A])(refinePf: PartialFunction[E1, E2])(implicit ev: E1 <:< Throwable): F[E2, A] =
    handleErrorWith[E1, A, E2](fea) {
      e => refinePf.andThen(leftPure).applyOrElse(e, (t: E1) => die(t))
    }

  def sync[A](block: => A): F[Nothing, A] = suspend(rightPure(block))

  def effectTotal[A](block: => A): F[Nothing, A] = sync(block)

  def syncCatch[E, A](block: => A)(catchPf: PartialFunction[Throwable, E]): F[E, A] = suspend {
    try {
      rightPure(block): F[E, A]
    } catch {
      catchPf.andThen(leftPure(_): F[E, A])
    }
  }

  def syncException[A](block: => A): F[Exception, A] =
    syncCatch(block) {
      case e: Exception => e
    }

  def syncThrowable[A](block: => A): F[Throwable, A] =
    syncCatch(block) {
      case t: Throwable => t
    }

  def bracketCloseable[R <: AutoCloseable, E, A](acquire: F[E, R])(use: R => F[E, A]): F[E, A] =
    bracket(acquire)(r => sync(r.close()))(use)

}

object Sync extends SyncStaticOps {
  def apply[F[+_, +_] : Sync]: Sync[F] = implicitly[Sync[F]]

  implicit class Ops[F[+_, +_], E, A](fea: F[E, A])(implicit sync: Sync[F]) extends Bracket.Ops[F, E, A](fea) {
    def orDie(implicit ev: E <:< Throwable): F[Nothing, A] = sync.orDie(fea)
    def refineOrDie[E2](refinePf: PartialFunction[E, E2])(implicit ev: E <:< Throwable): F[E2, A] = sync.refineOrDie[E, A, E2](fea)(refinePf)
  }
}

trait SyncStaticOps extends BracketStaticOps {
  def suspend[F[+_, +_] : Sync, E, A](effect: => F[E, A]): F[E, A] = Sync[F].suspend(effect)
  def failUnchecked[F[+_, +_] : Sync](t: Throwable): F[Nothing, Nothing] = Sync[F].failUnchecked(t)
  def die[F[+_, +_] : Sync](t: Throwable): F[Nothing, Nothing] = Sync[F].die(t)
  def sync[F[+_, +_] : Sync, A](block: => A): F[Nothing, A] = Sync[F].sync(block)
  def effectTotal[F[+_, +_] : Sync, A](block: => A): F[Nothing, A] = Sync[F].effectTotal(block)
  def syncCatch[F[+_, +_] : Sync, E, A](block: => A)(catchPf: PartialFunction[Throwable, E]): F[E, A] = Sync[F].syncCatch(block)(catchPf)
  def syncException[F[+_, +_] : Sync, A](block: => A): F[Exception, A] = Sync[F].syncException(block)
  def syncThrowable[F[+_, +_] : Sync, A](block: => A): F[Throwable, A] = Sync[F].syncThrowable(block)
  def bracketCloseable[F[+_, +_] : Sync, R <: AutoCloseable, E, A](acquire: F[E, R])(use: R => F[E, A]): F[E, A] = Sync[F].bracketCloseable(acquire)(use)
}
