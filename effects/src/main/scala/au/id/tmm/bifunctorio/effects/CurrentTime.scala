package au.id.tmm.bifunctorio.effects

import java.time.{Instant, LocalDate, ZonedDateTime}

import au.id.tmm.bifunctorio.typeclasses.effects.Sync

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
