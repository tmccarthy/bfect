package au.id.tmm.bifunctorio

sealed trait Failure[+E]

object Failure {
  case class Checked[E](e: E) extends Failure[E]
  case class Unchecked(exception: Throwable) extends Failure[Nothing]
}
