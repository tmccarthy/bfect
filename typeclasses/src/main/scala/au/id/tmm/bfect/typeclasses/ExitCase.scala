package au.id.tmm.bfect.typeclasses

sealed trait ExitCase[+E, +A]

object ExitCase {

  final case class Succeeded[A](a: A) extends ExitCase[Nothing, A]
  final case class Failed[E](failure: Failure[E]) extends ExitCase[E, Nothing]

}
