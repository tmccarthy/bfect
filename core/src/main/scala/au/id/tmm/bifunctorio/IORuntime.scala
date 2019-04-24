package au.id.tmm.bifunctorio

class IORuntime private () {

  def run[E, A](io: IO[E, A]): Either[Failure[E], A] = io match {
    case IO.Pure(a)                    => Right(a)
    case IO.Fail(cause)                => Left(cause)
    case IO.FlatMap(io, f)             => runFlatMap(io, f)
    case IO.FoldM(io, leftF, rightF)   => runFoldM(io, leftF, rightF)
    case IO.Effect(block)              =>
      try Right(block()) catch {
        case t: Throwable => Left(Failure.Unchecked(t))
      }
  }

  private def runFlatMap[E, A, A1](baseIO: IO[E, A], f: A => IO[E, A1]): Either[Failure[E], A1] = baseIO match {
    case IO.Pure(a)       => run(f(a))
    case IO.Fail(failure) => Left(failure)
    case baseIO           => run(baseIO) match {
      case Right(a) => run(f(a))
      case Left(failure)  => Left(failure)
    }
  }

  private def runFoldM[E, E2, A, A2](
                                      baseIO: IO[E, A],
                                      leftF: Failure[E] => IO[E2, A2],
                                      rightF: A => IO[E2, A2],
                                    ): Either[Failure[E2], A2] = baseIO match {
    case IO.Pure(a)       => run(rightF(a))
    case IO.Fail(failure) => run(leftF(failure))
    case baseIO           => run(baseIO) match {
      case Right(a)      => run(rightF(a))
      case Left(failure) => run(leftF(failure))
    }
  }

}

object IORuntime {
  def apply(): IORuntime = new IORuntime()
}
