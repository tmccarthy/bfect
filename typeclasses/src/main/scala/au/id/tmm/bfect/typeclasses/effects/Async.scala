package au.id.tmm.bfect.typeclasses.effects

import scala.util.Either

trait Async[F[+_, +_]] extends Sync[F] {

  def async[E, A](k: (Either[E, A] => Unit) => Unit): F[E, A] = asyncF { callback =>
    sync {
      k(callback)
    }
  }

  def asyncF[E, A](k: (Either[E, A] => Unit) => F[Nothing, _]): F[E, A]

}

object Async {
  def apply[F[+_, +_] : Async]: Async[F] = implicitly[Async[F]]
}
