package au.id.tmm.bfect

import au.id.tmm.bfect.syntax.≈>

trait BiInvariantK[T[_[_, _]]] {

  def biImapK[F[_, _], G[_, _]](F: T[F])(fFG: F ≈> G)(fGF: G ≈> F): T[G]

}

object BiInvariantK {
  def apply[T[_[_, _]]](implicit T: BiInvariantK[T]): BiInvariantK[T] = implicitly

  trait ToBiInvariantKOps {
    implicit def toBiInvariantKOps[T[_[_, _]], F[_, _]](tf: T[F])(implicit biInvariantK: BiInvariantK[T]): Ops[T, F] =
      new Ops(tf)
  }

  final class Ops[T[_[_, _]], F[_, _]](tf: T[F])(implicit biInvariantK: BiInvariantK[T]) {
    def biImapK[G[_, _]](fFG: F ≈> G)(fGF: G ≈> F): T[G] = biInvariantK.biImapK[F, G](tf)(fFG)(fGF)
  }
}
