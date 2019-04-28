package au.id.tmm.bifunctorio

import au.id.tmm.bifunctorio

sealed trait ExitCase[+E, +A]

object ExitCase {

  final case class Succeeded[A](a: A) extends ExitCase[Nothing, A]
  final case class Failed[E](failure: bifunctorio.Failure[E]) extends ExitCase[E, Nothing]

}
