package au.id.tmm.bifunctorio.typeclasses

trait BiFunctor[F[_, _]] {

  def biMap[L1, R1, L2, R2](f: F[L1, R1])(leftF: L1 => L2, rightF: R1 => R2): F[L2, R2]

  def rightMap[L, R1, R2](f: F[L, R1])(rightF: R1 => R2): F[L, R2] = biMap(f)(identity, rightF)

  def map[L, R1, R2](f: F[L, R1])(rightF: R1 => R2): F[L, R2] = biMap(f)(identity, rightF)

  def leftMap[L1, R, L2](f: F[L1, R])(leftF: L1 => L2): F[L2, R] = biMap(f)(leftF, identity)

}

object BiFunctor {
  def apply[F[+_, +_] : BiFunctor]: BiFunctor[F] = implicitly[BiFunctor[F]]
}
