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

import au.id.tmm.bfect.Fibre

trait Concurrent[F[+_, +_]] extends Async[F] {

  def start[E, A](fea: F[E, A]): F[Nothing, Fibre[F, E, A]]

  def racePair[E, A, B](fea: F[E, A], feb: F[E, B]): F[E, Either[(A, Fibre[F, E, B]), (Fibre[F, E, A], B)]]

  def race[E, A, B](fea: F[E, A], feb: F[E, B]): F[E, Either[A, B]] =
  /*_*/
    flatMap(racePair(fea, feb)) {
      case Left((a, fibreForB)) => map(fibreForB.cancel)(_ => Left(a))
      case Right((fibreForA, b)) => map(fibreForA.cancel)(_ => Right(b))
    }
  /*_*/

  def cancelable[E, A](k: (Either[E, A] => Unit) => F[Nothing, _]): F[E, A]

}

object Concurrent {
  def apply[F[+_, +_] : Concurrent]: Concurrent[F] = implicitly[Concurrent[F]]
}

