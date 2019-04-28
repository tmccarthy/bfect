package au.id.tmm.bifunctorio

import au.id.tmm.utilities.testing.ImprovedFlatSpec

class FlatMapRuntimeSpec extends ImprovedFlatSpec {

  private val runtime = IORuntime()

  "A flatMap instance" can "run a pure flatMapped to a success" in {
    val io = IO.pure("hello")
      .flatMap(s => IO.pure(s.length))

    assert(runtime.run(io) === Right(5))
  }

  it can "run a pure flatMapped to a failure" in {
    val io = IO.pure("hello")
      .flatMap(s => IO.leftPure(GenericError))

    assert(runtime.run(io) === Left(Failure.Checked(GenericError)))
  }

}
