package au.id.tmm.bfect.interop.cats.instances

import au.id.tmm.bfect.effects.Die
import cats.MonadError
import cats.data.EitherT

class DieInstance[F[_]] private[instances] (implicit F: MonadError[F, Throwable])
    extends BMEInstance[F]
    with Die[EitherT[F, *, *]] {
  override def failUnchecked(t: Throwable): EitherT[F, Nothing, Nothing] =
    EitherT.liftF[F, Nothing, Nothing](F.raiseError[Nothing](t))
}
