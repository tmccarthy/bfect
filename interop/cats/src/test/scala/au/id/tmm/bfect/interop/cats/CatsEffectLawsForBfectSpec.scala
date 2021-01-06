package au.id.tmm.bfect.interop.cats

import au.id.tmm.bfect.interop.cats.implicits._
import cats.data.EitherT
import cats.effect.IO._
import cats.effect._
import cats.effect.laws.discipline.{AsyncTests, SyncTests}
import cats.instances.all._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.FunSuiteDiscipline
import cats.effect.laws.discipline.arbitrary._
import cats.effect.laws.util.{TestContext, TestInstances}
import cats.laws.discipline.arbitrary._

class CatsEffectLawsForBfectSpec extends AnyFunSuite with FunSuiteDiscipline with Checkers with TestInstances {
  implicit val testContext: TestContext = TestContext()
  type F[A] = EitherT[IO, Throwable, A]
  checkAll("EitherT.SyncTests", SyncTests[F].sync[Int, Boolean, String])
  checkAll("EitherT.AsyncTests", AsyncTests[F].async[Int, Boolean, String])
}
