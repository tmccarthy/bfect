package au.id.tmm.bfect

import org.scalatest.flatspec.AnyFlatSpec
import au.id.tmm.bfect.syntax.bifunctor._

class BifunctorSyntaxSpec extends AnyFlatSpec {

  "the bifunctor syntax" should "compile" in {
    val either: WrappedEither[Int, String] = WrappedEither(Right("asdf"))

    either.biMap(i => i + 1, s => s.tail): WrappedEither[Int, String]
    either.rightMap(s => s.tail): WrappedEither[Int, String]
    either.map(s => s.tail): WrappedEither[Int, String]
    either.leftMap(i => i + 1): WrappedEither[Int, String]
    either.mapError(i => i + 1): WrappedEither[Int, String]
    either.biWiden[Any, Any]: WrappedEither[Any, Any]
    either.rightWiden[CharSequence]: WrappedEither[Int, CharSequence]
    either.widen[CharSequence]: WrappedEither[Int, CharSequence]
    either.leftWiden[Any]: WrappedEither[Any, String]
  }

  it should "compile when the left type is Nothing" in {
    val either: WrappedEither[Nothing, String] = WrappedEither(Right("asdf"))

    either.rightMap(s => s.tail): WrappedEither[Int, String]
    either.map(s => s.tail): WrappedEither[Int, String]
    either.biWiden[Any, Any]: WrappedEither[Any, Any]
    either.rightWiden[CharSequence]: WrappedEither[Int, CharSequence]
    either.widen[CharSequence]: WrappedEither[Int, CharSequence]
    either.leftWiden[Any]: WrappedEither[Any, String]

    either.asExceptionFallible: WrappedEither[Exception, String]
    either.asThrowableFallible: WrappedEither[Throwable, String]
  }

  it should "compile when the right type is Nothing" in {
    val either: WrappedEither[Int, Nothing] = WrappedEither(Left(1))

    either.leftMap(i => i + 1): WrappedEither[Int, String]
    either.mapError(i => i + 1): WrappedEither[Int, String]
    either.biWiden[Any, Any]: WrappedEither[Any, Any]
    either.rightWiden[Any]: WrappedEither[Int, Any]
    either.widen[Any]: WrappedEither[Int, Any]
    either.leftWiden[Any]: WrappedEither[Any, Nothing]
  }

}
