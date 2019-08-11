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
import au.id.tmm.bfect.BifunctorMonad

class BifunctorMonadInstance private[instances] () extends BifunctorInstance with BifunctorMonad[IO] {

  override def rightPure[A](a: A): IO[Nothing, A] = IO.pure(a)

  override def leftPure[E](e: E): IO[E, Nothing] = IO.leftPure(e)

  override def flatMap[E1, E2 >: E1, A, B](fe1a: IO[E1, A])(fafe2b: A => IO[E2, B]): IO[E2, B] = fe1a.flatMap(fafe2b)

  /**
    * Keeps calling `f` until a `scala.util.Right[B]` is returned.
    */
  override def tailRecM[E, A, A1](a: A)(f: A => IO[E, Either[A, A1]]): IO[E, A1] = f(a).flatMap {
    case Right(a1) => IO.pure(a1)
    case Left(a)   => tailRecM(a)(f)
  }

}
