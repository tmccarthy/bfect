package au.id.tmm.bifunctorio

import au.id.tmm.bifunctorio.IORuntimeSpec.GenericError
import au.id.tmm.utilities.testing.ImprovedFlatSpec

class IORuntimeSpec extends ImprovedFlatSpec {

  private val runtime = IORuntime()

  "an IORuntime" can "run a pure flatMapped to a success" in {
    val io = IO.pure("hello")
        .flatMap(s => IO.pure(s.length))

    assert(runtime.run(io) === Right(5))
  }

  it can "run a pure flatMapped to a failure" in {
    val io = IO.pure("hello")
      .flatMap(s => IO.leftPure(GenericError))

    assert(runtime.run(io) === Left(IORuntime.Failure.Checked(GenericError)))
  }

  it can "run a sync action" in {
    val io = IO.sync("hello")

    assert(runtime.run(io) === Right("hello"))
  }

  it can "run a sync action that throws" in {
    val exception = new RuntimeException

    val io = IO.sync {
      throw exception
    }

    assert(runtime.run(io) === Left(IORuntime.Failure.Unchecked(exception)))
  }

  it can "run a sync action that throws an expected exception" in {
    val exception = new RuntimeException

    val io = IO.syncException {
      throw exception
    }

    assert(runtime.run(io) === Left(IORuntime.Failure.Checked(exception)))
  }

  it can "run a sync action that throws an expected exception subtype" in {
    val exception = new RuntimeException

    val io = IO.syncCatch(throw exception) {
      case e: RuntimeException => e
    }

    assert(runtime.run(io) === Left(IORuntime.Failure.Checked(exception)))
  }

}

object IORuntimeSpec {
  private case object GenericError
}
