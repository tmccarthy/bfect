package au.id.tmm.bfect.catsinterop.instances

import au.id.tmm.bfect.effects.{Now, Timer}
import cats.Monad
import cats.data.EitherT

import java.time.{Duration, Instant}

class TimerInstance[F[_] : cats.effect.Timer : Monad] private[instances]
    extends BMEInstance[F]
    with Timer.WithBMonad[EitherT[F, *, *]] {
  private val bfectNowInstance: Now[EitherT[F, *, *]] = new NowInstance[F]

  override def now: EitherT[F, Nothing, Instant] = bfectNowInstance.now

  override def sleep(duration: Duration): EitherT[F, Nothing, Unit] =
    EitherT.liftF[F, Nothing, Unit](cats.effect.Timer[F].sleep(Timer.convertJavaDurationToScalaDuration(duration)))
}
