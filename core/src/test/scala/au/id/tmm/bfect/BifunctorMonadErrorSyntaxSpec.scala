package au.id.tmm.bfect

import au.id.tmm.bfect.syntax.bifunctorMonadError._

class BifunctorMonadErrorSyntaxSpec[F[_, _] : BME] {
  private def makeF[L, R]: F[L, R] = ???

  {
    makeF[String, Int].attempt: F[Nothing, Either[String, Int]]
    makeF[String, Int].handleErrorWith(e => makeF[CharSequence, Int]): F[CharSequence, Int]
    makeF[String, Int].recoverWith {
      case e: CharSequence => makeF[CharSequence, Int]
    }
    makeF[String, Int].catchLeft {
      case e: CharSequence => makeF[CharSequence, Int]
    }
    makeF[String, Int].onLeft {
      case e: CharSequence => makeF[Nothing, Unit]
    }
  }

  {
    makeF[Nothing, Int].attempt: F[Nothing, Either[Nothing, Int]]
    makeF[Nothing, Int].handleErrorWith(e => makeF[CharSequence, Int]): F[CharSequence, Int]
    makeF[Nothing, Int].recoverWith {
      case e: CharSequence => makeF[CharSequence, Int]
    }
    makeF[Nothing, Int].catchLeft {
      case e: CharSequence => makeF[CharSequence, Int]
    }
    makeF[Nothing, Int].onLeft {
      case e: CharSequence => makeF[Nothing, Unit]
    }
  }

  {
    makeF[String, Nothing].attempt: F[Nothing, Either[String, Nothing]]
    makeF[String, Nothing].handleErrorWith(e => makeF[CharSequence, Nothing]): F[CharSequence, Nothing]
    makeF[String, Nothing].recoverWith {
      case e: CharSequence => makeF[CharSequence, Nothing]
    }
    makeF[String, Nothing].catchLeft {
      case e: CharSequence => makeF[CharSequence, Nothing]
    }
    makeF[String, Nothing].onLeft {
      case e: CharSequence => makeF[Nothing, Unit]
    }
  }

  {
    makeF[Nothing, Nothing].attempt: F[Nothing, Either[Nothing, Nothing]]
    makeF[Nothing, Nothing].handleErrorWith(e => makeF[CharSequence, Nothing]): F[CharSequence, Nothing]
    makeF[Nothing, Nothing].recoverWith {
      case e: CharSequence => makeF[CharSequence, Nothing]
    }
    makeF[Nothing, Nothing].catchLeft {
      case e: CharSequence => makeF[CharSequence, Nothing]
    }
    makeF[Nothing, Nothing].onLeft {
      case e: CharSequence => makeF[Nothing, Unit]
    }
  }

}
