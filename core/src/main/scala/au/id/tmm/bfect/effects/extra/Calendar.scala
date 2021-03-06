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
package au.id.tmm.bfect.effects.extra

import java.time._

import au.id.tmm.bfect.BMonad
import au.id.tmm.bfect.effects.{Now, Sync}

trait Calendar[F[_, _]] extends Now[F] {

  def localTimezone: F[Nothing, ZoneId]

  def nowInstant: F[Nothing, Instant] = now

  def nowZonedDateTime: F[Nothing, ZonedDateTime]
  def nowLocalDateTime: F[Nothing, LocalDateTime]
  def nowLocalDate: F[Nothing, LocalDate]
  def nowLocalTime: F[Nothing, LocalTime]
  def nowOffsetDateTime: F[Nothing, OffsetDateTime]

}

object Calendar {

  def apply[F[_, _] : Calendar]: Calendar[F] = implicitly[Calendar[F]]

  trait WithBMonad[F[_, _]] extends Calendar[F] { self: BMonad[F] =>
    override def nowZonedDateTime: F[Nothing, ZonedDateTime] =
      flatMap[Nothing, Nothing, ZoneId, ZonedDateTime](localTimezone) { tz =>
        map[Nothing, Instant, ZonedDateTime](now) { now =>
          now.atZone(tz)
        }
      }

    override def nowLocalDateTime: F[Nothing, LocalDateTime] =
      map[Nothing, ZonedDateTime, LocalDateTime](nowZonedDateTime)(_.toLocalDateTime)

    override def nowLocalDate: F[Nothing, LocalDate] =
      map[Nothing, ZonedDateTime, LocalDate](nowZonedDateTime)(_.toLocalDate)

    override def nowLocalTime: F[Nothing, LocalTime] =
      map[Nothing, ZonedDateTime, LocalTime](nowZonedDateTime)(_.toLocalTime)

    override def nowOffsetDateTime: F[Nothing, OffsetDateTime] =
      map[Nothing, ZonedDateTime, OffsetDateTime](nowZonedDateTime)(_.toOffsetDateTime)
  }

  trait Live[F[_, _]] extends Calendar.WithBMonad[F] { self: Sync[F] =>
    override def localTimezone: F[Nothing, ZoneId]             = sync(ZoneId.systemDefault())
    override def now: F[Nothing, Instant]                      = sync(Instant.now())
    override def nowInstant: F[Nothing, Instant]               = sync(Instant.now())
    override def nowZonedDateTime: F[Nothing, ZonedDateTime]   = sync(ZonedDateTime.now())
    override def nowLocalDateTime: F[Nothing, LocalDateTime]   = sync(LocalDateTime.now())
    override def nowLocalDate: F[Nothing, LocalDate]           = sync(LocalDate.now())
    override def nowLocalTime: F[Nothing, LocalTime]           = sync(LocalTime.now())
    override def nowOffsetDateTime: F[Nothing, OffsetDateTime] = sync(OffsetDateTime.now())
  }

}
