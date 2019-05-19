package au.id.tmm.bfect.ziointerop

import au.id.tmm.bfect.{Failure => TmmFailure}
import scalaz.zio
import scalaz.zio.Exit.Cause

import scala.annotation.tailrec

object DataConversions {

  @tailrec
  def zioCauseToTmmFailure[E](zioFailure: zio.Exit.Cause[E]): TmmFailure[E] = zioFailure match {
    case Cause.Fail(value)       => TmmFailure.Checked(value)
    case Cause.Die(value)        => TmmFailure.Unchecked(value)
    case Cause.Interrupt         => TmmFailure.Interrupted
    case Cause.Then(left, right) => zioCauseToTmmFailure(left)
    case Cause.Both(left, right) => zioCauseToTmmFailure(left)
  }

}
