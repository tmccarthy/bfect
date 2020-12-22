package au.id.tmm.bfect.interop.cats.instanceimpls.implicitconversions

import au.id.tmm.bfect.effects.{Async, Bracket}

class CatsAsyncForBfectAsync[F[_, _]](implicit bfectBracket: Bracket[F], bfectAsync: Async[F])
    extends CatsSyncForBfectSync[F]
    with cats.effect.Async[F[Throwable, ?]] {
  override def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[Throwable, A] = bfectAsync.async(k)
  override def asyncF[A](k: (Either[Throwable, A] => Unit) => F[Throwable, Unit]): F[Throwable, A] =
    bfectAsync.asyncF[Throwable, A](k.andThen(FailureHandlingUtils.makeFailureUnchecked(_)))
}

object CatsAsyncForBfectAsync {
  trait ToCatsAsync {
    implicit def bfectAsyncIsCatsAsync[F[_, _] : Async : Bracket]: cats.effect.Async[F[Throwable, ?]] =
      new CatsAsyncForBfectAsync[F]()
  }
}
