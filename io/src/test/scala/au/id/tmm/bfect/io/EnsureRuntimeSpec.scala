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

class EnsureRuntimeSpec extends ImprovedFlatSpec {

  private val runtime = IORuntime()

  "an ensuring IO" should "ensure the execution of the finalizer after a successful IO" in {
    var ioHasRun = false
    var finalizerHasRun = false

    val io = IO.sync { ioHasRun = true }.ensure(IO.sync { finalizerHasRun = true })

    runtime.run(io)

    assert(ioHasRun)
    assert(finalizerHasRun)
  }

  it should "ensure the execution of the finalizer after a failed IO" in {
    var finalizerHasRun = false

    val io = IO.leftPure(Unit).ensure(IO.sync { finalizerHasRun = true })

    runtime.run(io)

    assert(finalizerHasRun)
  }

  it should "ensure the execution of the finalizer after an unchecked failure" in {
    var finalizerHasRun = false

    val io = IO.sync(throw new Exception).ensure(IO.sync { finalizerHasRun = true })

    runtime.run(io)

    assert(finalizerHasRun)
  }

  "a bracketed IO" should "close the resource if the use is successful" in {
    var resourceAcquired = false
    var resourceReleased = false

    val io = IO.bracket(
      acquire = IO.sync { resourceAcquired = true },
    )(
      release = _ => IO.sync { resourceReleased = true },
    ) { _ =>
      IO.unit
    }

    val result = runtime.run(io)

    assert(result === Succeeded(()))

    assert(resourceAcquired)
    assert(resourceReleased)
  }

  it should "fail if acquisition fails" in {
    var resourceReleased = false

    val io = IO.bracket(
      acquire = IO.leftPure(GenericError),
    )(
      release = _ => IO.sync { resourceReleased = true },
    ) { _ =>
      IO.unit
    }

    val result = runtime.run(io)

    assert(result === Failed(Failure.Checked(GenericError)))

    assert(!resourceReleased)
  }

  it should "fail if acquisition fails in an unchecked manner" in {
    var resourceReleased = false

    val exception = new Exception

    val io = IO.bracket(
      acquire = IO.sync(throw exception),
    )(
      release = _ => IO.sync { resourceReleased = true },
    ) { _ =>
      IO.unit
    }

    val result = runtime.run(io)

    assert(result === Failed(Failure.Unchecked(exception)))

    assert(!resourceReleased)
  }

  it should "close the resource if the use fails" in {
    var resourceAcquired = false
    var resourceReleased = false

    val io = IO.bracket(
      acquire = IO.sync { resourceAcquired = true },
    )(
      release = _ => IO.sync { resourceReleased = true },
    ) { _ =>
      IO.leftPure(GenericError)
    }

    val result = runtime.run(io)

    assert(result === Failed(Failure.Checked(GenericError)))

    assert(resourceAcquired)
    assert(resourceReleased)
  }

  it should "close the resource if the use fails in an unchecked manner" in {
    var resourceAcquired = false
    var resourceReleased = false

    val exception = new Exception

    val io = IO.bracket(
      acquire = IO.sync { resourceAcquired = true },
    )(
      release = _ => IO.sync { resourceReleased = true },
    ) { _ =>
      IO.sync(throw exception)
    }

    val result = runtime.run(io)

    assert(result === Failed(Failure.Unchecked(exception)))

    assert(resourceAcquired)
    assert(resourceReleased)
  }

  it should "fail if the closing of the resource fails" in {
    var resourceAcquired = false

    val exception = new Exception

    val io = IO.bracket(
      acquire = IO.sync { resourceAcquired = true },
    )(
      release = _ => IO.sync { throw exception },
    ) { _ =>
      IO.unit
    }

    val result = runtime.run(io)

    assert(result === Failed(Failure.Unchecked(exception)))

    assert(resourceAcquired)
  }

}
