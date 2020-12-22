package au.id.tmm.bfect.interop.cats.instanceimpls.implicitconversions

import au.id.tmm.bfect.BFunctor
import au.id.tmm.bfect.effects.Timer
import cats.effect.Clock

import scala.concurrent.duration.FiniteDuration

class CatsTimerForBfectTimer[F[_, _]](implicit bfectTimer: Timer[F], bFunctor: BFunctor[F])
    extends CatsClockForBfectNow[F]
    with cats.effect.Timer[F[Throwable, *]] {
  override def clock: Clock[F[Throwable, *]] = this

  override def sleep(duration: FiniteDuration): F[Throwable, Unit] = bFunctor.leftWiden(bfectTimer.sleep(duration))
}

object CatsTimerForBfectTimer {
  trait ToCatsTimer {
    implicit def bfectTimerIsCatsTimer[F[_, _] : Timer : BFunctor]: cats.effect.Timer[F[Throwable, *]] =
      new CatsTimerForBfectTimer[F]()
  }
}
