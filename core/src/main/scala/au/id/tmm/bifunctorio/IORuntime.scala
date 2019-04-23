package au.id.tmm.bifunctorio

import au.id.tmm.bifunctorio.IORuntime.Failure

class IORuntime private () {

  def run[E, A](io: IO[E, A]): Either[Failure[E], A] = io match {
    case IO.Pure(a)                    => Right(a)
    case IO.LeftPure(e)                => Left(Failure.Checked(e))
    case IO.FlatMap(io, f)             => runFlatMap(io, f)
    case IO.Effect(block)              =>
      try Right(block()) catch {
        case t: Throwable => Left(Failure.Unchecked(t))
      }
  }

  private def runFlatMap[E, A, A1](baseIO: IO[E, A], f: A => IO[E, A1]): Either[Failure[E], A1] = baseIO match {
    case IO.Pure(a)        => run(f(a))
    case IO.LeftPure(e)    => Left(Failure.Checked(e))
    case baseIO            => run(baseIO) match {
      case Right(a) => run(f(a))
      case Left(e)  => Left(e)
    }
  }

}

object IORuntime {
  def apply(): IORuntime = new IORuntime()

  sealed trait Failure[+E]

  object Failure {
    case class Checked[E](e: E) extends Failure[E]
    case class Unchecked(exception: Throwable) extends Failure[Nothing]
  }
}
