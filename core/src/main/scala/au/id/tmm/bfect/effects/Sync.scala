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
