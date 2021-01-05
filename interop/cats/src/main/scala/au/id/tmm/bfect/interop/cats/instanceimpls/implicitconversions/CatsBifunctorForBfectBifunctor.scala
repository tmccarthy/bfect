package au.id.tmm.bfect.interop.cats.instanceimpls.implicitconversions

import au.id.tmm.bfect._

class CatsBifunctorForBfectBifunctor[F[_, _]](implicit bfectBifunctor: Bifunctor[F]) extends cats.Bifunctor[F] {
  override def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D] = bfectBifunctor.biMap(fab)(f, g)

  override def leftMap[A, B, C](fab: F[A, B])(f: A => C): F[C, B] = bfectBifunctor.leftMap(fab)(f)
}

object CatsBifunctorForBfectBifunctor {
  trait ToCatsBifunctor {
    implicit def bfectBifunctorIsCatsBifunctor[F[_, _] : Bifunctor]: cats.Bifunctor[F] =
      new CatsBifunctorForBfectBifunctor[F]()
  }
}
