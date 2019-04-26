package au.id.tmm.bifunctorio

import au.id.tmm.bifunctorio.IO._

import scala.annotation.tailrec

class IORuntime private () {

  @tailrec
  final def run[E, A](io: IO[E, A]): Either[Failure[E], A] = io match {
    case Pure(a)                      => Right(a)
    case Fail(failure)                => Left(failure)

    case FlatMap(Pure(a), f)       => run(f.asInstanceOf[Any => IO[E, A]](a))
    case FlatMap(Fail(failure), _) => Left(failure)
    case FlatMap(FlatMap(baseIO, f1), f2) => run(baseIO.flatMap((a: Any) => FlatMap(f2.asInstanceOf[Any => IO[E, A]](a), f1)).asInstanceOf[IO[E, A]])
    case FlatMap(baseIO, f)           => nonTailRecRun(baseIO) match {
      case Right(a)      => run(f.asInstanceOf[Any => IO[E, A]](a))
      case Left(failure) => Left(failure)
    }

    case FoldM(Pure(a), leftF, rightF)       => run(rightF.asInstanceOf[Any => IO[E, A]](a))
    case FoldM(Fail(failure), leftF, rightF) => run(leftF.asInstanceOf[Any => IO[E, A]](failure))
    case FoldM(FoldM(baseIO, leftF1, rightF1), leftF2, rightF2)        =>
      run(
        baseIO.foldCauseM(
          (a: Any) => FoldM(leftF2.asInstanceOf[Any => IO[E, A]](a), leftF1, rightF1),
          (a: Any) => FoldM(rightF2.asInstanceOf[Any => IO[E, A]](a), leftF1, rightF1),
        ).asInstanceOf[IO[E, A]],
      )
    case FoldM(baseIO, leftF, rightF)        => nonTailRecRun(baseIO) match {
      case Right(a) => run(rightF.asInstanceOf[Any => IO[E, A]](a))
      case Left(failure) => run(leftF.asInstanceOf[Any => IO[E, A]](failure))
    }

    case Effect(block)                =>
      try Right(block()) catch {
        case t: Throwable => Left(Failure.Unchecked(t))
      }
  }

  private def nonTailRecRun[E, A](io: IO[E, A]): Either[Failure[E], A] = run(io)

}

object IORuntime {
  def apply(): IORuntime = new IORuntime()
}
