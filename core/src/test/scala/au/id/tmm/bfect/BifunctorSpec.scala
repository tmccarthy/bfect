package au.id.tmm.bfect

import org.scalatest.flatspec.AnyFlatSpec

class BifunctorSpec extends AnyFlatSpec {
  import EitherInstances._
  import Bifunctor.Ops

  "the .map extension method" should "compile when the left hand type is Nothing" in {
    val either: Either[Nothing, Unit] = Right(())

    def myMap[F[_, _] : Bifunctor](f: F[Nothing, Unit]): F[Nothing, Int] = f.map(_ => 1)

    assert(myMap(either) === Right(1))
  }

  it should "compile when neither type is Nothing" in {
    val either: Either[Unit, Unit] = Right(())

    def myMap[F[_, _] : Bifunctor](f: F[Unit, Unit]): F[Unit, Int] = f.map(_ => 1)

    assert(myMap(either) === Right(1))
  }

}
