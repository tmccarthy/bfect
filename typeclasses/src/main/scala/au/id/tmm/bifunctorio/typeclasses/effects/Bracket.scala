package au.id.tmm.bifunctorio.typeclasses.effects

import au.id.tmm.bifunctorio.ExitCase
import au.id.tmm.bifunctorio.typeclasses.BME

trait Bracket[F[+_, +_]] extends BME[F] {

  def bracketCase[R, E, A](acquire: F[E, R])(release: (R, ExitCase[E, A]) => F[Nothing, _])(use: R => F[E, A]): F[E, A]

  def bracket[R, E, A](acquire: F[E, R])(release: R => F[Nothing, _])(use: R => F[E, A]): F[E, A] =
    bracketCase[R, E, A](acquire){ case (resource, exitCase) => release(resource) }(use)

  def ensure[E, A](fea: F[E, A])(finalizer: F[Nothing, _]): F[E, A] =
    bracket[Unit, E, A](rightPure(()))(_ => finalizer)(_ => fea)

  def ensureCase[E, A](fea: F[E, A])(finalizer: ExitCase[E, A] => F[Nothing, _]): F[E, A] =
    bracketCase[Unit, E, A](rightPure(()))({ case (resource, exitCase) => finalizer(exitCase) })(_ => fea)

}
