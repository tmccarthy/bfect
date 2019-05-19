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

import java.time.{Instant, LocalDate, ZonedDateTime}

import au.id.tmm.bfect.effects.Sync

trait CurrentTime[F[+_, +_]] {
  def systemNanoTime: F[Nothing, Long]
  def currentTimeMillis: F[Nothing, Long]

  def nowInstant: F[Nothing, Instant]
  def nowLocalDate: F[Nothing, LocalDate]
  def nowZonedDateTime: F[Nothing, ZonedDateTime]
}

object CurrentTime {

  def apply[F[+_, +_] : CurrentTime]: CurrentTime[F] = implicitly[CurrentTime[F]]

  trait SyncInstance {
    implicit def currentTimeSyncInstance[F[+_, +_] : Sync]: CurrentTime[F] = new CurrentTime[F] {
      override def systemNanoTime: F[Nothing, Long] = Sync[F].sync(System.nanoTime())
      override def currentTimeMillis: F[Nothing, Long] = Sync[F].sync(System.currentTimeMillis())
      override def nowInstant: F[Nothing, Instant] = Sync[F].sync(Instant.now())
      override def nowLocalDate: F[Nothing, LocalDate] = Sync[F].sync(LocalDate.now())
      override def nowZonedDateTime: F[Nothing, ZonedDateTime] = Sync[F].sync(ZonedDateTime.now())
    }
  }

  object SyncInstance extends SyncInstance

}
