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

import java.time._

import au.id.tmm.bfect.BifunctorMonad
import au.id.tmm.bfect.effects.Now

trait Calendar[F[+_, +_]] extends BifunctorMonad[F] with Now[F] {

  def localTimezone: F[Nothing, ZoneId]

  def nowInstant: F[Nothing, Instant] = now

  def nowZonedDateTime: F[Nothing, ZonedDateTime] = flatMap(localTimezone) { tz =>
    map(now) { now =>
      now.atZone(tz)
    }
  }

  def nowLocalDateTime: F[Nothing, LocalDateTime] = map(nowZonedDateTime)(_.toLocalDateTime)

  def nowLocalDate: F[Nothing, LocalDate] = map(nowZonedDateTime)(_.toLocalDate)

  def nowLocalTime: F[Nothing, LocalTime] = map(nowZonedDateTime)(_.toLocalTime)

  def nowOffsetDateTime: F[Nothing, OffsetDateTime] = map(nowZonedDateTime)(_.toOffsetDateTime)

}

object Calendar {

  def apply[F[+_, +_] : Calendar]: Calendar[F] = implicitly[Calendar[F]]

}
