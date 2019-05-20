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
package au.id.tmm.bfect.io.instances.effects

import au.id.tmm.bfect.io.IO
import au.id.tmm.bfect.io.instances.BMEInstance
import au.id.tmm.bfect.ExitCase
import au.id.tmm.bfect.effects.Bracket

class BracketInstance private[instances]() extends BMEInstance with Bracket[IO] {

  override def bracketCase[R, E, A](acquire: IO[E, R])(release: (R, ExitCase[E, A]) => IO[Nothing, _])(use: R => IO[E, A]): IO[E, A] =
    IO.bracketCase(acquire)(release)(use)

  override def bracket[R, E, A](acquire: IO[E, R])(release: R => IO[Nothing, _])(use: R => IO[E, A]): IO[E, A] =
    IO.bracket(acquire)(release)(use)

  override def ensure[E, A](fea: IO[E, A])(finalizer: IO[Nothing, _]): IO[E, A] =
    fea.ensure(finalizer)

  override def ensureCase[E, A](fea: IO[E, A])(finalizer: ExitCase[E, A] => IO[Nothing, _]): IO[E, A] =
    fea.ensureCase(finalizer)

}
