package au.id.tmm.bfect.interop.cats.instances

import au.id.tmm.bfect.Bifunctor
import cats.Functor
import cats.data.EitherT

class BifunctorInstance[F[_]] private[instances] (implicit functor: Functor[F]) extends Bifunctor[EitherT[F, *, *]] {
  override def biMap[L1, R1, L2, R2](f: EitherT[F, L1, R1])(leftF: L1 => L2, rightF: R1 => R2): EitherT[F, L2, R2] =
    f.bimap(leftF, rightF)

  override def rightMap[L, R1, R2](f: EitherT[F, L, R1])(rightF: R1 => R2): EitherT[F, L, R2] = f.map(rightF)

  override def leftMap[L1, R, L2](f: EitherT[F, L1, R])(leftF: L1 => L2): EitherT[F, L2, R] = f.leftMap(leftF)
}
