package au.id.tmm.bifunctorio.catsinterop

import au.id.tmm.utilities.testing.ImprovedFlatSpec

class TmmToCatsTypeclassConversionsSpec extends ImprovedFlatSpec {

  "the monad instance" should "be resolved without difficulty" in {
    import au.id.tmm.bifunctorio.typeclasses.EitherInstances._

    cats.Monad[Either[Nothing, ?]]

    succeed
  }

  "the MonadError instance" should "be resolved without difficulty" in {
    import au.id.tmm.bifunctorio.typeclasses.EitherInstances._

    cats.MonadError[Either[Nothing, ?], Nothing]

    succeed
  }

}
