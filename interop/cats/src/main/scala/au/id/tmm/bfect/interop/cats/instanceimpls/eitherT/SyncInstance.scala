package au.id.tmm.bfect.interop.cats.instanceimpls.eitherT

import au.id.tmm.bfect.effects.Sync
import cats.data.EitherT

class SyncInstance[F[_] : cats.effect.Sync] private[instanceimpls]
    extends BracketInstance[F]
    with Sync[EitherT[F, *, *]] {
  override def failUnchecked(t: Throwable): EitherT[F, Nothing, Nothing] =
    EitherT.liftF[F, Nothing, Nothing](cats.effect.Sync[F].raiseError[Nothing](t))

  override def suspend[E, A](effect: => EitherT[F, E, A]): EitherT[F, E, A] =
    EitherT {
      cats.effect.Sync[F].defer(effect.value)
    }
}
