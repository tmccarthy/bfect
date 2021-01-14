package au.id.tmm.bfect.interop.cats

import au.id.tmm.bfect.WrappedEither
import cats.Eq
import cats.laws.discipline.{FunctorTests, MonadErrorTests, MonadTests}
import org.scalatestplus.scalacheck.Checkers
import org.scalacheck.ScalacheckShapeless._
import cats.instances.all._
import org.typelevel.discipline.scalatest.FunSuiteDiscipline
import au.id.tmm.bfect.interop.cats.implicits._
import org.scalatest.funsuite.AnyFunSuite

class CatsLawsForBfectSpec extends AnyFunSuite with FunSuiteDiscipline with Checkers {
  type F[A] = WrappedEither[String, A]
  private implicit def eqF[A]: Eq[F[A]] = Eq.fromUniversalEquals[F[A]]
  checkAll("WrappedEither.FunctorLaws", FunctorTests[F].functor[Int, Boolean, String])
  checkAll("WrappedEither.MonadLaws", MonadTests[F].monad[Int, Boolean, String])
  checkAll("WrappedEither.MonadErrorLaws", MonadErrorTests[F, String].monadError[Int, Boolean, String])
}
