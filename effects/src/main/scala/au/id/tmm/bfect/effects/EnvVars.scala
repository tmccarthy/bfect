package au.id.tmm.bfect.effects

import au.id.tmm.bfect.typeclasses.BifunctorMonad
import au.id.tmm.bfect.typeclasses.effects.Sync

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
    BifunctorMonad[F].flatMap(envVars)(_.get(key).fold(BifunctorMonad[F].leftPure(onMissing))(BifunctorMonad[F].rightPure(_)))

  trait SyncInstance {
    implicit def envVarsSyncInstance[F[+_, +_] : Sync]: EnvVars[F] = new EnvVars[F] {
      override def envVars: F[Nothing, Map[String, String]] = Sync[F].sync(sys.env)
    }
  }

  object SyncInstance extends SyncInstance

}
