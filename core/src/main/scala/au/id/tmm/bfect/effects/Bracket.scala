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

import au.id.tmm.bfect.ExitCase
import au.id.tmm.bfect.BME

trait Bracket[F[+_, +_]] extends BME[F] {

  def bracketCase[R, E, A](acquire: F[E, R])(release: (R, ExitCase[E, A]) => F[Nothing, _])(use: R => F[E, A]): F[E, A]

  def bracket[R, E, A](acquire: F[E, R])(release: R => F[Nothing, _])(use: R => F[E, A]): F[E, A] =
    bracketCase[R, E, A](acquire){ case (resource, exitCase) => release(resource) }(use)

  def ensure[E, A](fea: F[E, A])(finalizer: F[Nothing, _]): F[E, A] =
    bracket[Unit, E, A](rightPure(()))(_ => finalizer)(_ => fea)

  def ensureCase[E, A](fea: F[E, A])(finalizer: ExitCase[E, A] => F[Nothing, _]): F[E, A] =
    bracketCase[Unit, E, A](rightPure(()))({ case (resource, exitCase) => finalizer(exitCase) })(_ => fea)

}

object Bracket {
  def apply[F[+_, +_] : Bracket]: Bracket[F] = implicitly[Bracket[F]]
}
