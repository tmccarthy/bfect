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
package au.id.tmm.bfect.testing

import java.time.{Duration, Instant}

import au.id.tmm.bfect.effects.extra.Resources
import au.id.tmm.bfect.effects.{Async, Bracket, Concurrent, Timer}
import au.id.tmm.bfect.{BME, ExitCase, Failure, Fibre}

import scala.util.Random

final case class BState[S, +E, +A](run: S => (S, Either[E, A])) {

  def flatten[E2 >: E, A2](implicit ev: A <:< BState[S, E2, A2]): BState[S, E2, A2] =
    BState(
      run andThen {
        case (state, Right(bState)) => bState.run(state)
        case (state, Left(e))       => (state, Left(e))
      },
    )

  def biMap[E2, A2](leftF: E => E2, rightF: A => A2): BState[S, E2, A2] =
    BState(
      run andThen {
        case (state, Right(a)) => (state, Right(rightF(a)))
        case (state, Left(e))  => (state, Left(leftF(e)))
      },
    )

  def leftMap[E2](f: E => E2): BState[S, E2, A] = biMap(f, identity)

  def map[A2](f: A => A2): BState[S, E, A2] = biMap(identity, f)

  def flatMap[E2 >: E, A2](f: A => BState[S, E2, A2]): BState[S, E2, A2] = map[BState[S, E2, A2]](f).flatten[E2, A2]

  def runS(s0: S): S = run(s0) match { case (s, _) => s }

  def runEither(s0: S): Either[E, A] = run(s0) match { case (_, either) => either }

  def runUnsafe(s0: S): A = runEither(s0) match {
    case Right(a)           => a
    case Left(t: Throwable) => throw t
    case Left(e)            => throw new Exception(s"Run failed due to $e")
  }

  def statelessRun(implicit ev: Unit =:= S): Either[E, A] = run(())._2

  def statelessRunUnsafe(implicit ev: Unit =:= S): A = runUnsafe(())

}

object BState {

  type Stateless[+E, +A] = BState[Unit, E, A]

  def pure[S, A](a: A): BState[S, Nothing, A] = BState(s => (s, Right(a)))

  def leftPure[S, E](e: E): BState[S, E, Nothing] = BState(s => (s, Left(e)))

  def modify[S](f: S => S): BState[S, Nothing, Unit] = BState(s => (f(s), Right(())))

  def inspect[S, E, A](f: S => Either[E, A]): BState[S, E, A] = BState(s => (s, f(s)))

  def rightInspect[S, A](f: S => A): BState[S, Nothing, A] = inspect(f.andThen(Right.apply))

  trait BMEInstance[S] extends BME[BState[S, +*, +*]] {
    override def rightPure[E, A](a: A): BState[S, E, A] = BState.pure(a)

    override def leftPure[E, A](e: E): BState[S, E, A] = BState.leftPure(e)

    override def flatMap[E1, E2 >: E1, A, B](fe1a: BState[S, E1, A])(fafe2b: A => BState[S, E2, B]): BState[S, E2, B] =
      fe1a.flatMap(fafe2b)

    override def tailRecM[E, A, A1](a: A)(f: A => BState[S, E, Either[A, A1]]): BState[S, E, A1] = f(a).flatMap {
      case Right(value) => pure(value)
      case Left(value)  => tailRecM(value)(f)
    }

    override def biMap[L1, R1, L2, R2](f: BState[S, L1, R1])(leftF: L1 => L2, rightF: R1 => R2): BState[S, L2, R2] =
      f.biMap(leftF, rightF)
  }

  trait ConcurrentInstance[S]
      extends BMEInstance[S]
      with Concurrent.WithBMonad[BState[S, +*, +*]]
      with Async[BState[S, +*, +*]]
      with Bracket.WithBMonad[BState[S, +*, +*]]
      with Timer.WithBMonad[BState[S, +*, +*]] {

    private def asFibre[E, A](fea: BState[S, E, A]): Fibre[BState[S, +*, +*], E, A] =
      new Fibre[BState[S, +*, +*], E, A] {
        override def cancel: BState[S, Nothing, Unit] = BState[S, Nothing, Unit](d => (d, Right(())))

        override def join: BState[S, E, A] = fea
      }

    override def start[E, A](fea: BState[S, E, A]): BState[S, Nothing, Fibre[BState[S, +*, +*], E, A]] =
      pure(asFibre(fea))

    override def racePair[E, A, B](
      fea: BState[S, E, A],
      feb: BState[S, E, B],
    ): BState[S, E, Either[(A, Fibre[BState[S, +*, +*], E, B]), (Fibre[BState[S, +*, +*], E, A], B)]] =
      if (Random.nextBoolean()) {
        fea.map(a => Left((a, asFibre(feb))))
      } else {
        feb.map(b => Right((asFibre(fea), b)))
      }

    override def cancelable[E, A](k: (Either[E, A] => Unit) => BState[S, Nothing, _]): BState[S, E, A] = asyncF(k)

    override def asyncF[E, A](k: (Either[E, A] => Unit) => BState[S, Nothing, _]): BState[S, E, A] = {
      var result: Option[Either[E, A]] = None

      k {
        case r @ Right(a) => result = Some(r)
        case l @ Left(e)  => result = Some(l)
      }

      BState[S, Nothing, BState[S, E, A]] { state =>
        result match {
          case Some(Right(a)) => (state, Right(BState.pure(a)))
          case Some(Left(e))  => (state, Right(BState.leftPure[S, E](e)))
          case None           => (state, Right(never))
        }
      }.flatten[E, A]
    }

    override def never: BState[S, Nothing, Nothing] = BState { _ =>
      throw new IllegalStateException("never")
    }

    override def suspend[E, A](effect: => BState[S, E, A]): BState[S, E, A] =
      BState[S, E, BState[S, E, A]](s => (s, Right(effect))).flatten[E, A]

    override def bracketCase[R, E, A](
      acquire: BState[S, E, R],
      release: (R, ExitCase[E, Unit]) => BState[S, Nothing, _],
      use: R => BState[S, E, A],
    ): BState[S, E, A] =
      BState { state =>
        val (stateAfterAcquisition, result) = acquire.run(state)

        result match {
          case Right(acquired) =>
            use(acquired).run(stateAfterAcquisition) match {
              case (stateAfterUse, Right(resultAfterUse)) =>
                release(acquired, ExitCase.Succeeded(())).map(_ => resultAfterUse).run(stateAfterUse)

              case (stateAfterUse, Left(error)) =>
                release(acquired, ExitCase.Failed(Failure.Checked(error)))
                  .flatMap(_ => leftPure(error))
                  .run(stateAfterUse)
            }
          case Left(acquisitionFailure) => (stateAfterAcquisition, Left(acquisitionFailure))
        }
      }

    override def handleErrorWith[E1, A, E2](fea: BState[S, E1, A])(f: E1 => BState[S, E2, A]): BState[S, E2, A] =
      BState[S, E2, A](
        fea.run andThen {
          case (state, result) => {
            result match {
              case Right(a) => (state, Right(a))
              case Left(e)  => f(e).run(state)
            }
          }
        },
      )

    override def sleep(duration: Duration): BState[S, Nothing, Unit] =
      BState(state => (applySleepToState(duration, state), Right(())))

    override def now: BState[S, Nothing, Instant] =
      BState { state =>
        nowFromState(state) match {
          case (newState, instant) => (newState, Right(instant))
        }
      }

    protected def nowFromState(state: S): (S, Instant)

    protected def applySleepToState(sleepDuration: Duration, state: S): S
  }

  trait TimerInstance[S] extends ConcurrentInstance[S] with Timer.WithBMonad[BState[S, +*, +*]] {
    protected def nowFromState(state: S): (S, Instant)

    protected def applySleepToState(sleepDuration: Duration, state: S): S

    override def sleep(duration: Duration): BState[S, Nothing, Unit] =
      BState(state => (applySleepToState(duration, state), Right(())))

    override def now: BState[S, Nothing, Instant] =
      BState { state =>
        nowFromState(state) match {
          case (newState, instant) => (newState, Right(instant))
        }
      }
  }

  /**
    * Includes:
    * - an instance for `Timer` where `now` is always 1970-01-01T00:00:00Z, and sleep does
    *   not alter the state.
    * - an instance for `Resources` that reads the live resources
    */
  trait CompleteConcurrentInstance[S] extends ConcurrentInstance[S] with Resources.Live[BState[S, +*, +*]] {
    override def nowFromState(state: S): (S, Instant)                    = (state, Instant.EPOCH)
    override def applySleepToState(sleepDuration: Duration, state: S): S = state
  }

  implicit def concurrentInstance[S]: Concurrent[BState[S, +*, +*]]
    with Concurrent[BState[S, +*, +*]]
    with Async[BState[S, +*, +*]]
    with Bracket[BState[S, +*, +*]]
    with Timer[BState[S, +*, +*]]
    with Resources[BState[S, +*, +*]] = new CompleteConcurrentInstance[S] {}

}
