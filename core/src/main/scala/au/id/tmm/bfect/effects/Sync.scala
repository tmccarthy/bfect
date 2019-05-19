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

  def sync[A](block: => A): F[Nothing, A] = suspend(rightPure(block))

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

object Sync {
  def apply[F[+_, +_] : Sync]: Sync[F] = implicitly[Sync[F]]
}
