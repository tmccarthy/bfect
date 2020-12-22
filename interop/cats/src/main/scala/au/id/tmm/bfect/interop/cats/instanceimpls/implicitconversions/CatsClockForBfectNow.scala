package au.id.tmm.bfect.interop.cats.instanceimpls.implicitconversions

import au.id.tmm.bfect.BFunctor
import au.id.tmm.bfect.effects.Now

import scala.concurrent.duration.TimeUnit

class CatsClockForBfectNow[F[_, _]](implicit bfectNow: Now[F], bFunctor: BFunctor[F])
    extends cats.effect.Clock[F[Throwable, *]] {
  override def realTime(unit: TimeUnit): F[Throwable, Long] =
    bFunctor.map(bFunctor.asThrowableFallible(Now.now[F]))(_.toEpochMilli)

  override def monotonic(unit: TimeUnit): F[Throwable, Long] =
    bFunctor.map(bFunctor.asThrowableFallible(Now.now[F]))(i => i.getEpochSecond * i.getNano)
}

object CatsClockForBfectNow {
  trait ToCatsClock {
    implicit def bfectNowIsCatsClock[F[_, _] : Now : BFunctor]: cats.effect.Clock[F[Throwable, *]] =
      new CatsClockForBfectNow[F]()
  }
}
