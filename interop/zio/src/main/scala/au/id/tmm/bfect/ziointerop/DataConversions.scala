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

import au.id.tmm.bfect.{Failure => BfectFailure}

import scala.annotation.tailrec

object DataConversions {

  @tailrec
  def zioCauseToBfectFailure[E](zioFailure: zio.Cause[E]): BfectFailure[E] = zioFailure match {
    case zio.Cause.Fail(value)       => BfectFailure.Checked(value)
    case zio.Cause.Die(value)        => BfectFailure.Unchecked(value)
    case zio.Cause.Interrupt         => BfectFailure.Interrupted
    case zio.Cause.Then(left, right) => zioCauseToBfectFailure(left)
    case zio.Cause.Both(left, right) => zioCauseToBfectFailure(left)
    case zio.Cause.Traced(cause, trace) => zioCauseToBfectFailure(cause)
  }

}
