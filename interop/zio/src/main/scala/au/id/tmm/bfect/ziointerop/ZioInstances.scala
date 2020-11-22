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
package au.id.tmm.bfect.ziointerop

import java.time._
import java.util.concurrent.TimeUnit

import au.id.tmm.bfect._
import au.id.tmm.bfect.effects._
import au.id.tmm.bfect.effects.extra.{Calendar, Console, EnvVars, Resources}
import zio.clock.Clock
import zio.duration.{Duration => ZioDuration}
import zio.{Exit, Fiber, IO}

import scala.concurrent.duration.{Duration => ScalaDuration}

object ZioInstanceMixins {

  trait ZioBifunctor extends Bifunctor[IO] {
    override def biMap[L1, R1, L2, R2](f: IO[L1, R1])(leftF: L1 => L2, rightF: R1 => R2): IO[L2, R2] =
      f.bimap(leftF, rightF)
    override def rightMap[L, R1, R2](f: IO[L, R1])(rightF: R1 => R2): IO[L, R2] = f.map(rightF)
    override def leftMap[L1, R, L2](f: IO[L1, R])(leftF: L1 => L2): IO[L2, R]   = f.mapError(leftF)
  }

  trait ZioBMonad extends BifunctorMonad[IO] { self: BFunctor[IO] =>
    override def rightPure[A](a: A): IO[Nothing, A]                                              = IO.succeed(a)
    override def leftPure[E](e: E): IO[E, Nothing]                                               = IO.fail(e)
    override def flatMap[E1, E2 >: E1, A, B](fe1a: IO[E1, A])(fafe2b: A => IO[E2, B]): IO[E2, B] = fe1a.flatMap(fafe2b)
    override def tailRecM[E, A, A1](a: A)(f: A => IO[E, Either[A, A1]]): IO[E, A1] =
      f(a).flatMap {
        case Left(l)  => tailRecM(l)(f)
        case Right(r) => IO.succeed(r)
      }
  }

  trait ZioBME extends BME[IO] { self: BMonad[IO] =>
    override def handleErrorWith[E1, A, E2](fea: IO[E1, A])(f: E1 => IO[E2, A]): IO[E2, A] = fea.catchAll(f)
    override def recoverWith[E1, A, E2 >: E1](fea: IO[E1, A])(catchPf: PartialFunction[E1, IO[E2, A]]): IO[E2, A] =
      fea.catchSome(catchPf)
    override def attempt[E, A](fea: IO[E, A]): IO[Nothing, Either[E, A]] = fea.either
  }

  trait ZioBracket extends Bracket.WithBMonad[IO] { self: BME[IO] =>

    private def bfectExitCaseFrom[E, A](zioExit: zio.Exit[E, A]): ExitCase[E, A] = zioExit match {
      case Exit.Success(value) => ExitCase.Succeeded(value)
      case Exit.Failure(cause) => ExitCase.Failed(DataConversions.zioCauseToBfectFailure(cause))
    }

    override def bracketCase[R, E, A](
      acquire: IO[E, R],
      release: (R, ExitCase[E, A]) => IO[Nothing, _],
      use: R => IO[E, A],
    ): IO[E, A] = {
      val releaseForZioBracket: (R, zio.Exit[E, A]) => IO[Nothing, _] = {
        case (resource, zioExit) =>
          release(resource, bfectExitCaseFrom(zioExit))
      }

      IO.bracketExit[E, R, A](acquire, releaseForZioBracket, use)
    }

    override def bracket[R, E, A](
      acquire: IO[E, R],
      release: R => IO[Nothing, _],
      use: R => IO[E, A],
    ): IO[E, A] =
      IO.bracket[E, R, A](acquire, release, use)

    override def ensure[E, A](fea: IO[E, A])(finalizer: IO[Nothing, _]): IO[E, A] =
      fea.ensuring(finalizer)
  }

  trait ZioDie extends Die[IO] { self: BME[IO] =>
    override def failUnchecked(t: Throwable): IO[Nothing, Nothing] = IO.die(t)

    override def orDie[E, A](fea: IO[E, A])(implicit ev: E <:< Throwable): IO[Nothing, A] = fea.orDie

    override def refineOrDie[E1, A, E2](
      fea: IO[E1, A],
    )(
      refinePf: PartialFunction[E1, E2],
    )(implicit
      ev: E1 <:< Throwable,
    ): IO[E2, A] = fea.refineOrDie(refinePf)
  }

  trait ZioSync extends Sync[IO] with ZioDie { self: BME[IO] =>

    override def suspend[E, A](effect: => IO[E, A]): IO[E, A] = IO.effectSuspendTotal(effect)

    override def sync[A](block: => A): IO[Nothing, A] =
      IO.effectTotal(block)

    //noinspection ConvertibleToMethodValue
    override def syncCatch[E, A](block: => A)(catchPf: PartialFunction[Throwable, E]): IO[E, A] = {
      val catchTotal: Throwable => IO[E, A] = t =>
        catchPf.andThen(IO.fail(_)).applyOrElse(t, (t: Throwable) => IO.die(t))

      IO.effect(block).catchAll(catchTotal)
    }

    override def syncThrowable[A](block: => A): IO[Throwable, A] = IO.effect(block)

    override def failUnchecked(t: Throwable): IO[Nothing, Nothing] = super.failUnchecked(t)
  }

  trait ZioAsync extends Async[IO] { self: Sync[IO] =>
    override def async[E, A](registerForTmm: (Either[E, A] => Unit) => Unit): IO[E, A] = {
      val registerForZio: (IO[E, A] => Unit) => Unit = { cbForZio =>
        val cbForTmm: Either[E, A] => Unit = either => cbForZio(IO.fromEither(either))

        registerForTmm(cbForTmm)
      }

      IO.effectAsync(registerForZio)
    }

    override def asyncF[E, A](registerForTmm: (Either[E, A] => Unit) => IO[Nothing, _]): IO[E, A] = {
      val registerForZio: (IO[E, A] => Unit) => IO[Nothing, _] = { cbForZio =>
        val cbForTmm: Either[E, A] => Unit = either => cbForZio(IO.fromEither(either))

        registerForTmm(cbForTmm)
      }

      IO.effectAsyncM(registerForZio)
    }
  }

  trait ZioTimer extends Timer.WithBMonad[IO] { self: BME[IO] =>
    private val clock = Clock.Service.live

    override def sleep(duration: Duration): IO[Nothing, Unit] = clock.sleep(ZioDuration.fromJava(duration))
    override def sleep(scalaDuration: ScalaDuration): IO[Nothing, Unit] =
      clock.sleep(ZioDuration.fromScala(scalaDuration))
    override def now: IO[Nothing, Instant] =
      clock.currentTime(TimeUnit.NANOSECONDS).map(nanos => Instant.ofEpochSecond(nanos / 1000000000, nanos))
  }

  trait ZioConcurrent extends Concurrent[IO] { self: BMonad[IO] =>
    def bfectFibreFrom[E, A](zioFiber: Fiber[E, A]): Fibre[IO, E, A] = new Fibre[IO, E, A] {
      override def cancel: IO[Nothing, Unit] = zioFiber.interrupt.unit
      override def join: IO[E, A]            = zioFiber.join
    }

    override def start[E, A](fea: IO[E, A]): IO[Nothing, Fibre[IO, E, A]] = fea.fork.map(bfectFibreFrom)

    override def racePair[E, A, B](
      left: IO[E, A],
      right: IO[E, B],
    ): IO[E, Either[(A, Fibre[IO, E, B]), (Fibre[IO, E, A], B)]] =
      left.raceWith(right)(
        leftDone = {
          case (Exit.Failure(cause), rightFiber)     => rightFiber.interrupt.zipRight(IO.halt(cause))
          case (Exit.Success(leftValue), rightFiber) => rightPure(Left((leftValue, bfectFibreFrom(rightFiber))))
        },
        rightDone = {
          case (Exit.Failure(cause), leftFiber)      => leftFiber.interrupt.zipRight(IO.halt(cause))
          case (Exit.Success(rightValue), leftFiber) => rightPure(Right((bfectFibreFrom(leftFiber), rightValue)))
        },
      )

    override def race[E, A, B](fea: IO[E, A], feb: IO[E, B]): IO[E, Either[A, B]] = fea.raceEither(feb)

    override def par[E, A, B](fea: IO[E, A], feb: IO[E, B]): IO[E, (A, B)] = fea.zipPar(feb)

    override def cancelable[E, A](register: (Either[E, A] => Unit) => IO[Nothing, _]): IO[E, A] =
      //noinspection ScalaUnnecessaryParentheses
      IO.effectAsyncInterrupt { (cbForZio: IO[E, A] => Unit) =>
        val cancellationToken: IO[Nothing, _] = register { result =>
          cbForZio(IO.fromEither(result))
        }

        Left(cancellationToken)
      }
  }

}

trait ZioInstances {
  import ziointerop.{ZioInstanceMixins => Mixins}

  implicit val zioInstance: Mixins.ZioBifunctor
    with Mixins.ZioBMonad
    with Mixins.ZioBME
    with Mixins.ZioBracket
    with Mixins.ZioDie
    with Mixins.ZioSync
    with Mixins.ZioAsync
    with Mixins.ZioTimer
    with Mixins.ZioConcurrent
    with Calendar.Live[IO]
    with Console.Live[IO]
    with EnvVars.Live[IO]
    with Resources.Live[IO] =
    new Mixins.ZioBifunctor with Mixins.ZioBMonad with Mixins.ZioBME with Mixins.ZioBracket with Mixins.ZioDie
    with Mixins.ZioSync with Mixins.ZioAsync with Mixins.ZioTimer with Mixins.ZioConcurrent with Calendar.Live[IO]
    with Console.Live[IO] with EnvVars.Live[IO] with Resources.Live[IO]

}
