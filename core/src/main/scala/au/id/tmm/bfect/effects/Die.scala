package au.id.tmm.bfect.effects

import au.id.tmm.bfect.{BME, BifunctorMonadErrorStaticOps}

trait Die[F[+_, +_]] extends BME[F] {

  def failUnchecked(t: Throwable): F[Nothing, Nothing]

  def die(t: Throwable): F[Nothing, Nothing] = failUnchecked(t)

  def orDie[E, A](fea: F[E, A])(implicit ev: E <:< Throwable): F[Nothing, A] = handleErrorWith[E, A, Nothing](fea)(die(_))

  def refineOrDie[E1, A, E2](fea: F[E1, A])(refinePf: PartialFunction[E1, E2])(implicit ev: E1 <:< Throwable): F[E2, A] =
    handleErrorWith[E1, A, E2](fea) {
      e => refinePf.andThen(leftPure).applyOrElse(e, (t: E1) => die(t))
    }

}

object Die extends DieStaticOps {
  def apply[F[+_, +_] : Die]: Die[F] = implicitly[Die[F]]

  implicit class Ops[F[+_, +_], E, A](fea: F[E, A])(implicit sync: Die[F]) extends BME.Ops[F, E, A](fea) {
    def orDie(implicit ev: E <:< Throwable): F[Nothing, A] = sync.orDie(fea)
    def refineOrDie[E2](refinePf: PartialFunction[E, E2])(implicit ev: E <:< Throwable): F[E2, A] = sync.refineOrDie[E, A, E2](fea)(refinePf)
  }
}

trait DieStaticOps extends BifunctorMonadErrorStaticOps {
  def failUnchecked[F[+_, +_] : Die](t: Throwable): F[Nothing, Nothing] = Die[F].failUnchecked(t)
  def die[F[+_, +_] : Die](t: Throwable): F[Nothing, Nothing] = Die[F].die(t)
}
