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
package au.id.tmm.bfect.io.instances

import au.id.tmm.bfect.io.IO
import au.id.tmm.bfect.Bifunctor

class BifunctorInstance private[instances] () extends Bifunctor[IO] {
  override def biMap[L1, R1, L2, R2](f: IO[L1, R1])(leftF: L1 => L2, rightF: R1 => R2): IO[L2, R2] =
    f.biMap(leftF, rightF)

  override def rightMap[L, R1, R2](f: IO[L, R1])(rightF: R1 => R2): IO[L, R2] =
    f.map(rightF)

  override def leftMap[L1, R, L2](f: IO[L1, R])(leftF: L1 => L2): IO[L2, R] =
    f.leftMap(leftF)
}
