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
