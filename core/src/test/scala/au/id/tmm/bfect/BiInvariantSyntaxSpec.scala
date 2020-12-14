package au.id.tmm.bfect

import au.id.tmm.bfect.BiInvariantSyntaxSpec.Wrapped
import au.id.tmm.bfect.syntax.biInvariant._
import org.scalatest.flatspec.AnyFlatSpec

class BiInvariantSyntaxSpec extends AnyFlatSpec {

  "the syntax for biInvariant" should "compile" in {
    val either: WrappedEither[String, Int] = WrappedEither(Right(1))

    either.biImap(s => Wrapped(s), i => Wrapped(i))(ws => ws.unwrap, wi => wi.unwrap): WrappedEither[
      Wrapped[String],
      Wrapped[Int]]
  }

}

object BiInvariantSyntaxSpec {
  final case class Wrapped[A](unwrap: A) extends AnyVal
}
