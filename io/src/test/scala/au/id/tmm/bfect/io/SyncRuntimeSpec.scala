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

import au.id.tmm.bfect.ExitCase.Failed
import au.id.tmm.bfect.Failure
import au.id.tmm.utilities.testing.ImprovedFlatSpec

class SyncRuntimeSpec extends ImprovedFlatSpec {

  private val runtime = IORuntime()

  "an IORuntime" can "run a sync action that throws" in {
    val exception = new RuntimeException

    val io = IO.sync {
      throw exception
    }

    assert(runtime.run(io) === Failed(Failure.Unchecked(exception)))
  }

  it can "run a fold action that changes the error type" in {
    val io = IO.leftPure("Error")
      .leftMap(s => s"Error: $s")

    assert(runtime.run(io) === Failed(Failure.Checked("Error: Error")))
  }

}
