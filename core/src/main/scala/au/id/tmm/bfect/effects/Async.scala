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

import scala.util.Either

trait Async[F[+_, +_]] extends Sync[F] {

  def async[E, A](k: (Either[E, A] => Unit) => Unit): F[E, A] = asyncF { callback =>
    sync {
      k(callback)
    }
  }

  def asyncF[E, A](k: (Either[E, A] => Unit) => F[Nothing, _]): F[E, A]

}

object Async extends AsyncStaticOps {

  def apply[F[+_, +_] : Async]: Async[F] = implicitly[Async[F]]

  implicit class Ops[F[+_, +_], E, A](fea: F[E, A])(implicit async: Async[F]) extends Sync.Ops[F, E, A](fea)

}

trait AsyncStaticOps extends SyncStaticOps {
  def async[F[+_, +_] : Async, E, A](k: (Either[E, A] => Unit) => Unit): F[E, A] = Async[F].async(k)
  def asyncF[F[+_, +_] : Async, E, A](k: (Either[E, A] => Unit) => F[Nothing, _]): F[E, A] = Async[F].asyncF(k)
}
