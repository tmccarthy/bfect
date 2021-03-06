/**
  *    Copyright 2019 Timothy McCarthy
  *
  *    Licensed under the Apache License, Version 2.0 (the "License");
  *    you may not use this file except in compliance with the License.
  *    You may obtain a copy of the License at
  *
  *        http://www.apache.org/licenses/LICENSE-2.0
  *
  *    Unless required by applicable law or agreed to in writing, software
  *    distributed under the License is distributed on an "AS IS" BASIS,
  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *    See the License for the specific language governing permissions and
  *    limitations under the License.
  */
package au.id.tmm.bfect

import au.id.tmm.bfect.syntax.biFunctionK.≈>

trait Bifunctor[F[_, _]] extends BiInvariant[F] {

  override def biImap[L1, L2, R1, R2](
    fl1r1: F[L1, R1],
  )(
    fl1l2: L1 => L2,
    fr1r2: R1 => R2,
  )(
    fl2l1: L2 => L1,
    fr2r1: R2 => R1,
  ): F[L2, R2] = biMap(fl1r1)(fl1l2, fr1r2)

  def biMap[L1, R1, L2, R2](f: F[L1, R1])(leftF: L1 => L2, rightF: R1 => R2): F[L2, R2]

  def rightMap[L, R1, R2](f: F[L, R1])(rightF: R1 => R2): F[L, R2] = biMap(f)(identity, rightF)

  def map[L, R1, R2](f: F[L, R1])(rightF: R1 => R2): F[L, R2] = biMap(f)(identity, rightF)

  def leftMap[L1, R, L2](f: F[L1, R])(leftF: L1 => L2): F[L2, R] = biMap(f)(leftF, identity)

  def mapError[L1, R, L2](f: F[L1, R])(leftF: L1 => L2): F[L2, R] = leftMap(f)(leftF)

  def biAs[L1, L2, R1, R2](fl1r1: F[L1, R1])(l2: L2, r2: R2): F[L2, R2] = biMap(fl1r1)(_ => l2, _ => r2)

  def rightAs[L, R1, R2](flr1: F[L, R1])(r2: R2): F[L, R2] = rightMap(flr1)(_ => r2)

  def as[L, R1, R2](flr1: F[L, R1])(r2: R2): F[L, R2] = rightAs(flr1)(r2)

  def unit[L, R](flr: F[L, R]): F[L, Unit] = rightAs(flr)(())

  def leftAs[L1, L2, R](fl1r: F[L1, R])(l2: L2): F[L2, R] = leftMap(fl1r)(_ => l2)

  @inline def biWiden[L1, L2 >: L1, R1, R2 >: R1](f: F[L1, R1]): F[L2, R2] = f.asInstanceOf[F[L2, R2]]

  @inline def rightWiden[L, R1, R2 >: R1](f: F[L, R1]): F[L, R2] = f.asInstanceOf[F[L, R2]]

  @inline def widen[L, R1, R2 >: R1](f: F[L, R1]): F[L, R2] = f.asInstanceOf[F[L, R2]]

  @inline def leftWiden[L1, L2 >: L1, R](f: F[L1, R]): F[L2, R] = f.asInstanceOf[F[L2, R]]

  @inline def asExceptionFallible[A](fa: F[Nothing, A]): F[Exception, A] = leftWiden(fa)

  @inline def asThrowableFallible[A](fa: F[Nothing, A]): F[Throwable, A] = leftWiden(fa)

}

object Bifunctor {
  def apply[F[_, _] : Bifunctor]: Bifunctor[F] = implicitly[Bifunctor[F]]

  trait ToBifunctorOps {
    implicit def toBiFunctorOps[F[_, _], L, R](flr: F[L, R])(implicit bifunctor: Bifunctor[F]): Ops[F, L, R] =
      new Ops[F, L, R](flr)

    implicit def toBiFunctorOpsLeftNothing[F[_, _], R](
      flr: F[Nothing, R],
    )(implicit
      bifunctor: Bifunctor[F],
    ): Ops[F, Nothing, R] =
      new Ops[F, Nothing, R](flr)

    implicit def toBiFunctorOpsRightNothing[F[_, _], L](
      flr: F[L, Nothing],
    )(implicit
      bifunctor: Bifunctor[F],
    ): Ops[F, L, Nothing] =
      new Ops[F, L, Nothing](flr)

    implicit def toBiFunctorOpsLeftNothingRightNothing[F[_, _]](
      flr: F[Nothing, Nothing],
    )(implicit
      bifunctor: Bifunctor[F],
    ): Ops[F, Nothing, Nothing] =
      new Ops[F, Nothing, Nothing](flr)
  }

  final class Ops[F[_, _], L, R](flr: F[L, R])(implicit bifunctor: Bifunctor[F]) {
    def biMap[L2, R2](leftF: L => L2, rightF: R => R2): F[L2, R2] = bifunctor.biMap(flr)(leftF, rightF)
    def rightMap[R2](rightF: R => R2): F[L, R2]                   = bifunctor.rightMap(flr)(rightF)
    def map[R2](rightF: R => R2): F[L, R2]                        = bifunctor.map(flr)(rightF)
    def leftMap[L2](leftF: L => L2): F[L2, R]                     = bifunctor.leftMap(flr)(leftF)
    def mapError[L2](leftF: L => L2): F[L2, R]                    = bifunctor.mapError(flr)(leftF)
    def biAs[L2, R2](l2: L2, r2: R2): F[L2, R2]                   = bifunctor.biAs(flr)(l2, r2)
    def rightAs[R2](r2: R2): F[L, R2]                             = bifunctor.rightAs(flr)(r2)
    def as[R2](r2: R2): F[L, R2]                                  = bifunctor.as(flr)(r2)
    def unit: F[L, Unit]                                          = bifunctor.unit(flr)
    def leftAs[L2](l2: L2): F[L2, R]                              = bifunctor.leftAs(flr)(l2)
    @inline def biWiden[L2 >: L, R2 >: R]: F[L2, R2]              = bifunctor.biWiden(flr)
    @inline def rightWiden[R2 >: R]: F[L, R2]                     = bifunctor.rightWiden(flr)
    @inline def widen[R2 >: R]: F[L, R2]                          = bifunctor.widen(flr)
    @inline def leftWiden[L2 >: L]: F[L2, R]                      = bifunctor.leftWiden(flr)

    @inline def asExceptionFallible(implicit ev: L =:= Nothing): F[Exception, R] =
      bifunctor.asExceptionFallible(flr.asInstanceOf[F[Nothing, R]])
    @inline def asThrowableFallible(implicit ev: L =:= Nothing): F[Throwable, R] =
      bifunctor.asThrowableFallible(flr.asInstanceOf[F[Nothing, R]])
  }

  implicit val bifunctorBiInvariantK: BiInvariantK[Bifunctor] = new BiInvariantK[Bifunctor] {
    override def biImapK[F[_, _], G[_, _]](F: Bifunctor[F])(fFG: F ≈> G)(fGF: G ≈> F): Bifunctor[G] = new BFunctor[G] {
      override def biMap[L1, R1, L2, R2](g: G[L1, R1])(leftF: L1 => L2, rightF: R1 => R2): G[L2, R2] =
        fFG(F.biMap(fGF(g))(leftF, rightF))
    }
  }
}
