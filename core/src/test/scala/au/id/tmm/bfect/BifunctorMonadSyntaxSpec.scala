package au.id.tmm.bfect

import au.id.tmm.bfect.syntax.bifunctorMonad._

class BifunctorMonadSyntaxSpec[F[_, _] : BifunctorMonad] {

  private def makeF[L, R]: F[L, R] = ???

  {
    makeF[Int, String].flatMap(s => makeF[Int, Byte]): F[Int, Byte]

    makeF[Int, String] >> (_ => makeF[Int, Byte]): F[Int, Byte]

    lazy val _ = makeF[Int, String].forever

    makeF[Int, F[Int, String]].flatten: F[Int, String]

    makeF[String, Either[CharSequence, Int]].absolve: F[CharSequence, Int]

    makeF[String, Option[Int]].absolveOption(ifNone = "asdf"): F[String, Int]
  }

  {
    makeF[Nothing, String].flatMap(s => makeF[Int, Byte]): F[Int, Byte]

    lazy val _ = makeF[Nothing, String].forever

    makeF[Nothing, Either[String, Int]].absolve: F[String, Int]

    makeF[Nothing, Option[Int]].absolveOption(ifNone = "asdf"): F[String, Int]
  }

  {
    lazy val _ = makeF[Int, Nothing].forever
  }

  {
    lazy val _ = (??? : F[Nothing, Nothing]).forever
  }

}
