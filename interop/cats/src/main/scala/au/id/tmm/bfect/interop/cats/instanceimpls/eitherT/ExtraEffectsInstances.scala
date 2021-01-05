package au.id.tmm.bfect.interop.cats.instanceimpls.eitherT

import au.id.tmm.bfect.effects.extra._
import cats.data.EitherT

class ExtraEffectsInstances[F[_] : cats.effect.Sync] private[instanceimpls]
    extends SyncInstance[F]
    with Calendar.Live[EitherT[F, *, *]]
    with Console.Live[EitherT[F, *, *]]
    with EnvVars.Live[EitherT[F, *, *]]
    with Resources.Live[EitherT[F, *, *]]
