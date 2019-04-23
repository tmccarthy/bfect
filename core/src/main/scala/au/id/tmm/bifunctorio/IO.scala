package au.id.tmm.bifunctorio

sealed trait IO[+E, +A] {

  def map[A1](f: A => A1): IO[E, A1] = this match {
    case IO.Pure(a)           => IO.Pure(f(a))
    case self: IO.LeftPure[E] => self
    case self: IO[E, A]       => IO.FlatMap(self, (a: A) => IO.Pure(f(a)))
  }

  def flatMap[E1 >: E, A1](f: A => IO[E1, A1]): IO[E1, A1] = this match {
    case self: IO.LeftPure[E] => self
    case self: IO[E, A]       => IO.FlatMap(self, f)
  }

  def flatten[E1 >: E, A1](implicit e: A <:< IO[E1, A1]): IO[E1, A1] = flatMap(io => io)

}

object IO {

  def pure[A](a: A): IO[Nothing, A] = Pure(a)

  def leftPure[E](e: E): IO[E, Nothing] = LeftPure(e)

  def sync[A](block: => A): IO[Nothing, A] = Effect(() => block)

  def syncException[A](block: => A): IO[Exception, A] = syncCatch(block) {
    case e: Exception => e
  }

  def syncCatch[E, A](block: => A)(catchPf: PartialFunction[Throwable, E]): IO[E, A] = Effect { () =>
    try {
      Pure(block)
    } catch catchPf.andThen(LeftPure(_))
  }.flatten

  final case class Pure[A](a: A) extends IO[Nothing, A]
  final case class LeftPure[E](e: E) extends IO[E, Nothing]
  final case class FlatMap[E, A, A1](io: IO[E, A], f: A => IO[E, A1]) extends IO[E, A1]
  final case class Effect[A](block: () => A) extends IO[Nothing, A]

}
