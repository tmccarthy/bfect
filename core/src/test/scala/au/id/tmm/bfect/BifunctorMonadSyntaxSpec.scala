package au.id.tmm.bfect

import org.scalatest.flatspec.AnyFlatSpec
import au.id.tmm.bfect.syntax.bifunctorMonad._

class BifunctorMonadSyntaxSpec extends AnyFlatSpec {

  "the bifunctor monad syntax" should "compile" in {
    WrappedEither[Int, String](Right("asdf")).flatMap(s => WrappedEither(Right(0x00))): WrappedEither[Int, Byte]

    lazy val _ = WrappedEither[Int, String](Right("asdf")).forever

    WrappedEither[Int, WrappedEither[Int, String]](Right(WrappedEither(Right("")))).flatten: WrappedEither[Int, String]

    WrappedEither[CharSequence, Either[String, Int]](Right(Right(1))).absolve: WrappedEither[CharSequence, Int]

    WrappedEither[String, Option[Int]](Right(Some(1))).absolveOption(ifNone = "asdf"): WrappedEither[String, Int]
  }

  it should "compile when the left type is Nothing" in {
    WrappedEither[Nothing, String](Right("asdf")).flatMap(s => WrappedEither(Right(0x00))): WrappedEither[Int, Byte]

    lazy val _ = WrappedEither[Nothing, String](Right("asdf")).forever

    WrappedEither[Int, WrappedEither[Nothing, String]](Right(WrappedEither(Right("")))).flatten: WrappedEither[
      Int,
      String]

    WrappedEither[Nothing, Either[String, Int]](Right(Right(1))).absolve: WrappedEither[CharSequence, Int]

    WrappedEither[Nothing, Option[Int]](Right(Some(1))).absolveOption(ifNone = "asdf"): WrappedEither[String, Int]
  }

  it should "compile when the right type is Nothing" in {
    lazy val _ = WrappedEither[Int, Nothing](Left(1)).forever

    WrappedEither[Int, WrappedEither[Int, Nothing]](Right(WrappedEither(Left(1)))).flatten: WrappedEither[Int, String]

    WrappedEither[CharSequence, Either[String, Nothing]](Right(Left("asdf"))).absolve: WrappedEither[CharSequence, Int]

    WrappedEither[String, Option[Nothing]](Right(None)).absolveOption(ifNone = "asdf"): WrappedEither[String, Int]
  }

  it should "compile when both types are Nothing" in intercept[NotImplementedError] {
    lazy val _ = (??? : WrappedEither[Nothing, Nothing]).forever

    WrappedEither[Int, WrappedEither[Nothing, Nothing]](???).flatten: WrappedEither[Int, Nothing]

    WrappedEither[Nothing, Either[Int, Nothing]](???).absolve: WrappedEither[Int, Nothing]

    WrappedEither[Nothing, Option[Nothing]](???).absolveOption(ifNone = "asdf"): WrappedEither[String, Nothing]
  }

}
