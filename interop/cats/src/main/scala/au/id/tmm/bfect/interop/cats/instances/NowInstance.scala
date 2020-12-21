package au.id.tmm.bfect.interop.cats.instances

import au.id.tmm.bfect.effects.Now
import cats.Functor
import cats.data.EitherT

import java.time.Instant

class NowInstance[F[_]] private[instances] (implicit clockF: cats.effect.Clock[F], F: Functor[F])
    extends Now[EitherT[F, *, *]] {
  override def now: EitherT[F, Nothing, Instant] = EitherT.liftF[F, Nothing, Instant] {
    F.map(clockF.realTime(scala.concurrent.duration.MILLISECONDS))(Instant.ofEpochMilli)
  }
}
