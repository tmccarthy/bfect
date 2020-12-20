package au.id.tmm.bfect.catsinterop.instances

import au.id.tmm.bfect.effects.extra._
import cats.data.EitherT

class ExtraEffectsInstances[F[_] : cats.effect.Sync] private[instances]
    extends SyncInstance[F]
    with Calendar.Live[EitherT[F, *, *]]
    with Console.Live[EitherT[F, *, *]]
    with EnvVars.Live[EitherT[F, *, *]]
    with Resources.Live[EitherT[F, *, *]]
