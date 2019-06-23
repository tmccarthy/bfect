/**
  *    Copyright 2019 Timothy McCarthy
  *
  *    Licensed under the Apache License, Version 2.0 (the "License");
  *    you may not use this file except in compliance with the License.
  *    You may obtain a copy of the License at
  *
  *        http://www.apache.org/licenses/LICENSE-2.0
  *
  *    Unless required by applicable law or agreed to in writing, software
  *    distributed under the License is distributed on an "AS IS" BASIS,
  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *    See the License for the specific language governing permissions and
  *    limitations under the License.
  */
package au.id.tmm.bfect.catsinterop

import au.id.tmm.bfect._
import au.id.tmm.bfect.effects._
import cats.effect.Clock

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

private[catsinterop] object BfectToCatsTypeclassConversionsImpls {

  class CatsBifunctorForBfectBifunctor[F[_, _]](implicit bfectBifunctor: Bifunctor[F]) extends cats.Bifunctor[F] {
    override def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D] = bfectBifunctor.biMap(fab)(f, g)

    override def leftMap[A, B, C](fab: F[A, B])(f: A => C): F[C, B] = bfectBifunctor.leftMap(fab)(f)
  }

  class CatsMonadForBfectBifunctorMonad[F[+_, +_], E](implicit bfectBifunctorMonad: BifunctorMonad[F]) extends cats.Monad[F[E, +?]] {
    override def flatMap[A, A1](fea: F[E, A])(f: A => F[E, A1]): F[E, A1] = bfectBifunctorMonad.flatMap[E, E, A, A1](fea)(f)

    override def tailRecM[A, A1](a: A)(f: A => F[E, Either[A, A1]]): F[E, A1] = bfectBifunctorMonad.tailRecM[E, A, A1](a)(f)

    override def pure[A](a: A): F[E, A] = bfectBifunctorMonad.rightPure(a)
  }

  class CatsMonadErrorForBfectBME[F[+_, +_], E](implicit bfectBme: BME[F]) extends cats.MonadError[F[E, +?], E] {
    override def flatMap[A, A1](fea: F[E, A])(f: A => F[E, A1]): F[E, A1] = bfectBme.flatMap[E, E, A, A1](fea)(f)

    override def tailRecM[A, A1](a: A)(f: A => F[E, Either[A, A1]]): F[E, A1] = bfectBme.tailRecM[E, A, A1](a)(f)

    override def pure[A](a: A): F[E, A] = bfectBme.rightPure(a)

    override def raiseError[A](e: E): F[E, A] = bfectBme.leftPure(e)

    override def handleErrorWith[A](fea: F[E, A])(f: E => F[E, A]): F[E, A] = bfectBme.handleErrorWith(fea)(f)
  }

  class CatsSyncForBfectSync[F[+_, +_]](implicit bfectBracket: Bracket[F], bfectSync: Sync[F]) extends CatsMonadErrorForBfectBME[F, Throwable] with cats.effect.Sync[F[Throwable, +?]] {
    override def suspend[A](thunk: => F[Throwable, A]): F[Throwable, A] = bfectSync.suspend(thunk)

    override def bracketCase[A, B](acquire: F[Throwable, A])(use: A => F[Throwable, B])(release: (A, cats.effect.ExitCase[Throwable]) => F[Throwable, Unit]): F[Throwable, B] = {

      val releaseForBfectBracket: (A, ExitCase[Throwable, B]) => F[Nothing, _] = { case (resource, exitCase) =>
        val catsExitCase: cats.effect.ExitCase[Throwable] = exitCase match {
          case ExitCase.Succeeded(a)                 => cats.effect.ExitCase.Completed
          case ExitCase.Failed(Failure.Interrupted)  => cats.effect.ExitCase.Canceled
          case ExitCase.Failed(Failure.Checked(e))   => cats.effect.ExitCase.Error(e)
          case ExitCase.Failed(Failure.Unchecked(t)) => cats.effect.ExitCase.Error(t)
        }

        bfectSync.handleErrorWith[Throwable, Unit, Nothing](release(resource, catsExitCase))(t => throw t)
      }

      bfectBracket.bracketCase[A, Throwable, B](acquire, releaseForBfectBracket, use)
    }
  }

  class CatsAsyncForBfectAsync[F[+_, +_]](implicit bfectBracket: Bracket[F], bfectAsync: Async[F]) extends CatsSyncForBfectSync[F] with cats.effect.Async[F[Throwable, +?]] {
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[Throwable, A] = bfectAsync.async(k)
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => F[Throwable, Unit]): F[Throwable, A] = bfectAsync.asyncF[Throwable, A](k.andThen(makeFailureUnchecked(_)))
  }

  class CatsConcurrentForBfectConcurrent[F[+_, +_]](implicit bfectBracket: Bracket[F], bfectAsync: Async[F], bfectConcurrent: Concurrent[F]) extends CatsAsyncForBfectAsync with cats.effect.Concurrent[F[Throwable, +?]] {
    override def start[A](fa: F[Throwable, A]): F[Throwable, cats.effect.Fiber[F[Throwable, +?], A]] = bfectAsync.map(bfectConcurrent.start(fa))(asCatsFiber)

    override def racePair[A, B](fa: F[Throwable, A], fb: F[Throwable, B]): F[Throwable, Either[(A, cats.effect.Fiber[F[Throwable, +?], B]), (cats.effect.Fiber[F[Throwable, +?], A], B)]] =
      bfectAsync.map(bfectConcurrent.racePair(fa, fb)) {
        case Left((a, bFiber))  => Left((a, asCatsFiber(bFiber)))
        case Right((aFiber, b)) => Right((asCatsFiber(aFiber), b))
      }

    private def asCatsFiber[A](bfectFibre: Fibre[F, Throwable, A]): cats.effect.Fiber[F[Throwable, +?], A] = new cats.effect.Fiber[F[Throwable, +?], A] {
      override def cancel: cats.effect.CancelToken[F[Throwable, +?]] = bfectFibre.cancel

      override def join: F[Throwable, A] = bfectFibre.join
    }

    override def race[A, B](fa: F[Throwable, A], fb: F[Throwable, B]): F[Throwable, Either[A, B]] = bfectConcurrent.race(fa, fb)

    override def cancelable[A](k: (Either[Throwable, A] => Unit) => cats.effect.CancelToken[F[Throwable, +?]]): F[Throwable, A] = bfectConcurrent.cancelable(k.andThen(makeFailureUnchecked(_)))
  }

  private def makeFailureUnchecked[F[+_, +_], E, A](fea: F[E, A])(implicit syncInstance: Sync[F]): F[Nothing, A] =
    syncInstance.handleErrorWith(fea) {
      case t: Throwable => syncInstance.sync(throw t)
      case e            => syncInstance.sync(throw FailureInCancellationToken(e))
    }

  final case class FailureInCancellationToken[E](e: E) extends Exception

  class CatsClockForBfectNow[F[+_, +_]](implicit bfectNow: Now[F], bFunctor: BFunctor[F]) extends cats.effect.Clock[F[Throwable, +?]] {
    import BFunctor.Ops

    override def realTime(unit: TimeUnit): F[Throwable, Long] = bfectNow.now.map(_.toEpochMilli)

    override def monotonic(unit: TimeUnit): F[Throwable, Long] = bfectNow.now.map(i => i.getEpochSecond * i.getNano)
  }

  class CatsTimerForBfectTimer[F[+_, +_]](implicit bfectTimer: Timer[F], bFunctor: BFunctor[F]) extends CatsClockForBfectNow[F] with cats.effect.Timer[F[Throwable, +?]] {
    override def clock: Clock[F[Throwable, +?]] = this

    override def sleep(duration: FiniteDuration): F[Throwable, Unit] = bfectTimer.sleep(duration)
  }

}

trait BfectToCatsTypeclassConversions {

  import BfectToCatsTypeclassConversionsImpls._

  implicit def bfectBifunctorIsCatsBifunctor[F[_, _] : Bifunctor]: cats.Bifunctor[F] = new CatsBifunctorForBfectBifunctor[F]()
  implicit def bfectBifunctorMonadIsCatsMonad[F[+_, +_] : BifunctorMonad, E]: cats.Monad[F[E, +?]] = new CatsMonadForBfectBifunctorMonad[F, E]()
  implicit def bfectBifunctorMonadErrorIsCatsMonadError[F[+_, +_] : BifunctorMonadError, E]: cats.MonadError[F[E, +?], E] = new CatsMonadErrorForBfectBME[F, E]()
  implicit def bfectSyncIsCatsSync[F[+_, +_] : Sync : Bracket]: cats.effect.Sync[F[Throwable, +?]] = new CatsSyncForBfectSync[F]()
  implicit def bfectAsyncIsCatsAsync[F[+_, +_] : Async : Bracket]: cats.effect.Async[F[Throwable, +?]] = new CatsAsyncForBfectAsync[F]()
  implicit def bfectConcurrentIsCatsConcurrent[F[+_, +_] : Concurrent : Async : Bracket]: cats.effect.Concurrent[F[Throwable, +?]] = new CatsConcurrentForBfectConcurrent[F]()

  implicit def bfectNowIsCatsClock[F[+_, +_] : Now : BFunctor]: cats.effect.Clock[F[Throwable, +?]] = new CatsClockForBfectNow[F]()
  implicit def bfectTimerIsCatsTimer[F[+_, +_] : Timer : BFunctor]: cats.effect.Timer[F[Throwable, +?]] = new CatsTimerForBfectTimer[F]()

}

trait CatsToBfectTypeclassConversions {

  implicit def catsBifunctorIsBfectBifunctor[F[_, _]](implicit catsBifunctor: cats.Bifunctor[F]): Bifunctor[F] = new Bifunctor[F] {
    override def biMap[L1, R1, L2, R2](f: F[L1, R1])(leftF: L1 => L2, rightF: R1 => R2): F[L2, R2] = catsBifunctor.bimap(f)(leftF, rightF)

    override def leftMap[L1, R, L2](f: F[L1, R])(leftF: L1 => L2): F[L2, R] = catsBifunctor.leftMap(f)(leftF)
  }

}

object CatsToBfectTypeclassConversions extends CatsToBfectTypeclassConversions

trait CatsTypeclassConversions extends BfectToCatsTypeclassConversions

object CatsTypeclassConversions extends CatsTypeclassConversions
