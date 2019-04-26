package au.id.tmm.bifunctorio.typeclasses.effects

import au.id.tmm.bifunctorio.typeclasses.BME

trait Sync[F[+_, +_]] extends BME[F] {

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

}

object Sync {
  def apply[F[+_, +_] : Sync]: Sync[F] = implicitly[Sync[F]]
}
