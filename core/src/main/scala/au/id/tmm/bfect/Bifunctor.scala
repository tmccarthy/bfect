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

trait Bifunctor[F[_, _]] {

  def biMap[L1, R1, L2, R2](f: F[L1, R1])(leftF: L1 => L2, rightF: R1 => R2): F[L2, R2]

  def rightMap[L, R1, R2](f: F[L, R1])(rightF: R1 => R2): F[L, R2] = biMap(f)(identity, rightF)

  def map[L, R1, R2](f: F[L, R1])(rightF: R1 => R2): F[L, R2] = biMap(f)(identity, rightF)

  def leftMap[L1, R, L2](f: F[L1, R])(leftF: L1 => L2): F[L2, R] = biMap(f)(leftF, identity)

  def mapError[L1, R, L2](f: F[L1, R])(leftF: L1 => L2): F[L2, R] = leftMap(f)(leftF)

}

object Bifunctor {
  def apply[F[_, _] : Bifunctor]: Bifunctor[F] = implicitly[Bifunctor[F]]

  implicit class Ops[F[_, _], L, R](flr: F[L, R])(implicit bifunctor: Bifunctor[F]) {
    def biMap[L2, R2](leftF: L => L2, rightF: R => R2): F[L2, R2] = bifunctor.biMap(flr)(leftF, rightF)
    def rightMap[R2](rightF: R => R2): F[L, R2] = bifunctor.rightMap(flr)(rightF)
    def map[R2](rightF: R => R2): F[L, R2] = bifunctor.map(flr)(rightF)
    def leftMap[L2](leftF: L => L2): F[L2, R] = bifunctor.leftMap(flr)(leftF)
    def mapError[L2](leftF: L => L2): F[L2, R] = bifunctor.mapError(flr)(leftF)
  }
}
