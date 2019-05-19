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
package au.id.tmm.bfect.io

import au.id.tmm.bfect.ExitCase.{Failed, Succeeded}
import au.id.tmm.bfect.Failure
import au.id.tmm.utilities.testing.ImprovedFlatSpec

class FlatMapRuntimeSpec extends ImprovedFlatSpec {

  private val runtime = IORuntime()

  "A flatMap instance" can "run a pure flatMapped to a success" in {
    val io = IO.pure("hello")
      .flatMap(s => IO.pure(s.length))

    assert(runtime.run(io) === Succeeded(5))
  }

  it can "run a pure flatMapped to a failure" in {
    val io = IO.pure("hello")
      .flatMap(s => IO.leftPure(GenericError))

    assert(runtime.run(io) === Failed(Failure.Checked(GenericError)))
  }

}
