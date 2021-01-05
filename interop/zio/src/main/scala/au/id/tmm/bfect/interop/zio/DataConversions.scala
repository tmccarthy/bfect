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
package au.id.tmm.bfect.interop.zio

import au.id.tmm.bfect.{Failure => BfectFailure}

object DataConversions {

  def zioCauseToBfectFailure[E](zioFailure: zio.Cause[E]): BfectFailure[E] =
    zioFailure.fold[BfectFailure[E]](
      empty = ???,
      failCase = BfectFailure.Checked(_),
      dieCase = BfectFailure.Unchecked(_),
      interruptCase = _ => BfectFailure.Interrupted,
    )(
      thenCase = (left, right) => left,
      bothCase = (left, right) => left,
      tracedCase = (failure, trace) => failure,
    )

}
