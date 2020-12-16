package au.id.tmm.bfect.catsinterop.instances

import au.id.tmm.bfect.BifunctorMonadError
import cats.Monad
import cats.data.EitherT

class BMEInstance[F[_]] private[instances] (implicit monad: Monad[F])
    extends BifunctorMonadInstance[F]
    with BifunctorMonadError[EitherT[F, *, *]] {
  override def handleErrorWith[E1, A, E2](fea: EitherT[F, E1, A])(f: E1 => EitherT[F, E2, A]): EitherT[F, E2, A] =
    fea.leftFlatMap(f)
}
