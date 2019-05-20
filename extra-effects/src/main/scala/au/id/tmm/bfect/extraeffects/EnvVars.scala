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
package au.id.tmm.bfect.extraeffects

import au.id.tmm.bfect.BifunctorMonad
import au.id.tmm.bfect.effects.Sync

trait EnvVars[F[+_, +_]] {
  def envVars: F[Nothing, Map[String, String]]
}

object EnvVars {

  def apply[F[+_, +_] : EnvVars]: EnvVars[F] = implicitly[EnvVars[F]]

  def envVars[F[+_, +_] : EnvVars]: F[Nothing, Map[String, String]] =
    EnvVars[F].envVars

  def envVar[F[+_, +_] : EnvVars : BifunctorMonad](key: String): F[Nothing, Option[String]] =
    BifunctorMonad[F].map(envVars)(_.get(key))

  def envVarOrError[F[+_, +_] : EnvVars : BifunctorMonad, E](key: String, onMissing: => E): F[E, String] =
    BifunctorMonad[F].flatMap(envVars)(_.get(key)
      .fold[F[E, String]](BifunctorMonad[F].leftPure(onMissing))(BifunctorMonad[F].rightPure(_)))

  trait SyncInstance {
    implicit def envVarsSyncInstance[F[+_, +_] : Sync]: EnvVars[F] = new EnvVars[F] {
      override def envVars: F[Nothing, Map[String, String]] = Sync[F].sync(sys.env)
    }
  }

  object SyncInstance extends SyncInstance

}
