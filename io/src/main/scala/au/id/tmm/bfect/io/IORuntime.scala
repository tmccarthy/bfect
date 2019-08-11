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
package au.id.tmm.bfect.io

import au.id.tmm.bfect.io.IO._
import au.id.tmm.bfect.ExitCase._
import au.id.tmm.bfect.Failure.{Checked, Interrupted, Unchecked}
import au.id.tmm.bfect.{ExitCase, Failure}

import scala.annotation.tailrec

class IORuntime private () {

  @tailrec
  final def run[E, A](io: IO[E, A]): ExitCase[E, A] = io match {
    case Pure(a)       => Succeeded(a)
    case Fail(failure) => Failed(failure)

    case FlatMap(Pure(a), f)       => run(f.asInstanceOf[Any => IO[E, A]](a))
    case FlatMap(Fail(failure), _) => Failed(failure)
    case FlatMap(FlatMap(baseIO, f1), f2) =>
      run(baseIO.flatMap((a: Any) => FlatMap(f2.asInstanceOf[Any => IO[E, A]](a), f1)).asInstanceOf[IO[E, A]])
    case FlatMap(baseIO, f) =>
      nonTailRecRun(baseIO) match {
        case Succeeded(a)    => run(f.asInstanceOf[Any => IO[E, A]](a))
        case Failed(failure) => Failed(failure)
      }

    case FoldM(Pure(a), leftF, rightF)       => run(rightF.asInstanceOf[Any => IO[E, A]](a))
    case FoldM(Fail(failure), leftF, rightF) => run(leftF.asInstanceOf[Any => IO[E, A]](failure))
    case FoldM(FoldM(baseIO, leftF1, rightF1), leftF2, rightF2) =>
      run(
        baseIO
          .foldCauseM(
            (a: Any) => FoldM(leftF2.asInstanceOf[Any => IO[E, A]](a), leftF1, rightF1),
            (a: Any) => FoldM(rightF2.asInstanceOf[Any => IO[E, A]](a), leftF1, rightF1),
          )
          .asInstanceOf[IO[E, A]],
      )
    case FoldM(baseIO, leftF, rightF) =>
      nonTailRecRun(baseIO) match {
        case Succeeded(a)    => run(rightF.asInstanceOf[Any => IO[E, A]](a))
        case Failed(failure) => run(leftF.asInstanceOf[Any => IO[E, A]](failure))
      }

    case Effect(block) =>
      try Succeeded(block())
      catch {
        case t: Throwable => Failed(Failure.Unchecked(t))
      }

    case Ensure(io, finalizer) => {
      val result: ExitCase[E, A] = try {
        nonTailRecRun(io)
      } catch {
        case t: Throwable => Failed(Unchecked(t))
      }

      nonTailRecRun(finalizer.asInstanceOf[Any => IO[Nothing, _]](result)) match {
        case Succeeded(_)         => result
        case Failed(Checked(e))   => Failed(Unchecked(e)) // This is impossible
        case Failed(Interrupted)  => Failed(Interrupted)
        case Failed(Unchecked(t)) => Failed(Unchecked(t))
      }
    }
  }

  private def nonTailRecRun[E, A](io: IO[E, A]): ExitCase[E, A] = run(io)

}

object IORuntime {
  def apply(): IORuntime = new IORuntime()
}
