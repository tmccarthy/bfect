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

    assert(runtime.run(io) === Left(Failure.Checked(GenericError)))
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

    assert(runtime.run(io) === Left(Failure.Unchecked(exception)))
  }

  it can "run a fold action that changes the error type" in {
    val io = IO.leftPure("Error")
      .leftMap(s => s"Error: $s")

    assert(runtime.run(io) === Left(Failure.Checked("Error: Error")))

  }

}

object IORuntimeSpec {
  private case object GenericError
}
