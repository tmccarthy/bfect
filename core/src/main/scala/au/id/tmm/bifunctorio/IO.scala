package au.id.tmm.bifunctorio

sealed trait IO[+E, +A] {

  def map[A2](f: A => A2): IO[E, A2] = this match {
    case IO.Pure(a)           => IO.Pure(f(a))
    case self: IO.Fail[E]  => self
    case self: IO[E, A]       => IO.FlatMap(self, (a: A) => IO.Pure(f(a)))
  }

  def leftMap[E2](f: E => E2): IO[E2, A] =
    foldM(f.andThen(IO.leftPure), IO.Pure(_))

  def biMap[E2, A2](leftF: E => E2, rightF: A => A2): IO[E2, A2] =
    foldM(leftF.andThen(IO.leftPure), rightF.andThen(IO.Pure(_)))

  def fold[A2](leftF: E => A2, rightF: A => A2): IO[Nothing, A2] =
    foldM(leftF.andThen(IO.Pure(_)), rightF.andThen(IO.Pure(_)))

  def foldM[E2, A2](leftF: E => IO[E2, A2], rightF: A => IO[E2, A2]): IO[E2, A2] =
    foldCauseM(
      leftF = {
        case Failure.Checked(e)             => leftF(e)
        case cause @ Failure.Unchecked(_)   => IO.Fail(cause)
      },
      rightF,
    )

  def foldCauseM[E2, A2](leftF: Failure[E] => IO[E2, A2], rightF: A => IO[E2, A2]): IO[E2, A2] =
    IO.FoldM(this, leftF, rightF)

  def flatMap[E2 >: E, A2](f: A => IO[E2, A2]): IO[E2, A2] = this match {
    case IO.FlatMap(io, f2) => IO.FlatMap(io, f2.andThen(_.flatMap(f)))
    case self: IO.Fail[E]   => self
    case self: IO[E, A]     => IO.FlatMap(self, f)
  }

  def flatten[E2 >: E, A1](implicit e: A <:< IO[E2, A1]): IO[E2, A1] = flatMap(io => io)

}

object IO {

  def pure[A](a: A): IO[Nothing, A] = Pure(a)

  def leftPure[E](e: E): IO[E, Nothing] = Fail(Failure.Checked(e))

  def sync[A](block: => A): IO[Nothing, A] = Effect(() => block)

  final case class Pure[A](a: A) extends IO[Nothing, A]
  final case class Fail[E](cause: Failure[E]) extends IO[E, Nothing]
  final case class FlatMap[E, A, A2](io: IO[E, A], f: A => IO[E, A2]) extends IO[E, A2]
  final case class FoldM[E, E2, A, A2](io: IO[E, A], leftF: Failure[E] => IO[E2, A2], rightF: A => IO[E2, A2]) extends IO[E2, A2]
  final case class Effect[A](block: () => A) extends IO[Nothing, A]

}
