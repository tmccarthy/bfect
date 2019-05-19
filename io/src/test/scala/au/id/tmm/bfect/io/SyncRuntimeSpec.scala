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
