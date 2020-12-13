package au.id.tmm.bfect

import au.id.tmm.bfect.syntax.biFunctionK.≈>
import au.id.tmm.bfect.syntax.biInvariantK._

/**
  * A thin wrapper around `Either` that exposes no methods. We use this in the syntax tests
  * to ensure that (for example) when we call `.map` we are confident that we are invoking the method provided by the
  * syntax import, rather than the `.map` provided by `Either`.
  */
final case class WrappedEither[+L, +R](either: Either[L, R]) extends AnyVal

object WrappedEither {
  implicit val bmeInstance: BME[WrappedEither] = {
    val wrap: Either ≈> WrappedEither = new (Either ≈> WrappedEither) {
      override def apply[L, R](flr: Either[L, R]): WrappedEither[L, R] = WrappedEither(flr)
    }

    val unwrap: WrappedEither ≈> Either = new (WrappedEither ≈> Either) {
      override def apply[L, R](flr: WrappedEither[L, R]): Either[L, R] = flr.either
    }

    EitherInstances.bmeInstance.biImapK[WrappedEither](wrap)(unwrap)
  }
}
