package au.id.tmm.bfect

trait BiInvariant[F[_, _]] {

  def biImap[L1, L2, R1, R2](
    fl1r1: F[L1, R1],
  )(
    fl1l2: L1 => L2,
    fr1r2: R1 => R2,
  )(
    fl2l1: L2 => L1,
    fr2r1: R2 => R1,
  ): F[L2, R2]

}

object BiInvariant {

  def apply[F[_, _] : BiInvariant]: BiInvariant[F] = implicitly

  trait ToBiInvariantOps {
    implicit def toBiInvariantOps[F[_, _], L1, R1](fl1r1: F[L1, R1])(implicit biInvariant: BiInvariant[F]): Ops[F, L1, R1] =
      new Ops[F, L1, R1](fl1r1)
  }

  class Ops[F[_, _], L1, R1](fl1r1: F[L1, R1])(implicit biInvariant: BiInvariant[F]) {
    def biImap[L2, R2](fl1l2: L1 => L2, fr1r2: R1 => R2)(fl2l1: L2 => L1, fr2r1: R2 => R1): F[L2, R2] =
      biInvariant.biImap(fl1r1)(fl1l2, fr1r2)(fl2l1, fr2r1)
  }

}
