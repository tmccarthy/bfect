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
package au.id.tmm.bfect.effects

import java.time.{Duration, Instant}

import au.id.tmm.bfect.BMonad
import au.id.tmm.bfect.effects.Timer.convertScalaDurationToJavaDuration

import scala.concurrent.duration.{Duration => ScalaDuration, FiniteDuration => FiniteScalaDuration}

trait Timer[F[_, _]] extends Now[F] {

  def sleep(duration: Duration): F[Nothing, Unit]

  def sleep(scalaDuration: ScalaDuration): F[Nothing, Unit] =
    sleep(convertScalaDurationToJavaDuration(scalaDuration))

  def repeatFixedDelay[E, A](fea: F[E, A])(delay: Duration): F[E, Nothing]

  def repeatFixedDelay[E, A](fea: F[E, A], delayAsScalaDuration: ScalaDuration): F[E, Nothing]

  def repeatFixedRate[E, A](fea: F[E, A])(period: Duration): F[E, Nothing]

  def repeatFixedRate[E, A](fea: F[E, A], periodAsScalaDuration: ScalaDuration): F[E, Nothing] =
    repeatFixedRate(fea)(convertScalaDurationToJavaDuration(periodAsScalaDuration))

}

object Timer extends TimerStaticOps {

  def apply[F[_, _] : Timer]: Timer[F] = implicitly[Timer[F]]

  trait WithBMonad[F[_, _]] extends Timer[F] { self: BMonad[F] =>
    override def repeatFixedDelay[E, A](fea: F[E, A])(delay: Duration): F[E, Nothing] =
      forever(flatMap(fea)(_ => leftWiden(sleep(delay))))

    override def repeatFixedDelay[E, A](fea: F[E, A], delayAsScalaDuration: ScalaDuration): F[E, Nothing] =
      repeatFixedDelay(fea)(convertScalaDurationToJavaDuration(delayAsScalaDuration))

    override def repeatFixedRate[E, A](fea: F[E, A])(period: Duration): F[E, Nothing] = {
      def repeatFixedRateStartingAt(t0: Long, period: Long): F[E, Nothing] = flatMap[E, E, A, Nothing](fea) { _ =>
        flatMap[Nothing, E, Instant, Nothing](now) { instantCompleted =>
          val tCompleted    = instantCompleted.toEpochMilli
          val nextStart     = Instant.ofEpochMilli((period - ((tCompleted - t0) % period)) + tCompleted)
          val sleepDuration = Duration.between(instantCompleted, nextStart)
          flatMap[Nothing, E, Unit, Nothing](sleep(sleepDuration)) { _ =>
            repeatFixedRateStartingAt(t0, period)
          }
        }
      }

      flatMap[Nothing, E, Instant, Nothing](now) { instantStarted =>
        repeatFixedRateStartingAt(instantStarted.toEpochMilli, period.toMillis)
      }
    }
  }

  trait ToTimerOps {
    implicit def toTimerOps[F[_, _], E, A](fea: F[E, A])(implicit timerInstance: Timer[F]): Ops[F, E, A] =
      new Ops[F, E, A](fea)

    implicit def toTimerOpsErrorNothing[F[_, _], A](fea: F[Nothing, A])(implicit timerInstance: Timer[F]): Ops[F, Nothing, A] =
      new Ops[F, Nothing, A](fea)

    implicit def toTimerOpsValueNothing[F[_, _], E](fea: F[E, Nothing])(implicit timerInstance: Timer[F]): Ops[F, E, Nothing] =
      new Ops[F, E, Nothing](fea)

    implicit def toTimerOpsErrorNothingValueNothing[F[_, _]](fea: F[Nothing, Nothing])(implicit timerInstance: Timer[F]): Ops[F, Nothing, Nothing] =
      new Ops[F, Nothing, Nothing](fea)
  }

  final class Ops[F[_, _], E, A](fea: F[E, A])(implicit timerInstance: Timer[F]) {
    def repeatFixedDelay(delay: Duration): F[E, Nothing] = timerInstance.repeatFixedDelay(fea)(delay)
    def repeatFixedDelay(delayAsScalaDuration: ScalaDuration): F[E, Nothing] =
      timerInstance.repeatFixedDelay(fea, delayAsScalaDuration)
    def repeatFixedRate(period: Duration): F[E, Nothing] = timerInstance.repeatFixedRate(fea)(period)
    def repeatFixedRate(periodAsScalaDuration: ScalaDuration): F[E, Nothing] =
      timerInstance.repeatFixedRate(fea, periodAsScalaDuration)
  }

  private[effects] def convertScalaDurationToJavaDuration(scalaDuration: ScalaDuration): Duration =
    scalaDuration match {
      case ScalaDuration.MinusInf              => Duration.ofSeconds(Long.MinValue, 0)
      case _: ScalaDuration.Infinite           => Duration.ofSeconds(Long.MaxValue, 999999999)
      case finiteDuration: FiniteScalaDuration => Duration.ofNanos(finiteDuration.toNanos)
    }

}

trait TimerStaticOps extends NowStaticOps {
  def sleep[F[_, _] : Timer](duration: Duration): F[Nothing, Unit]           = Timer[F].sleep(duration)
  def sleep[F[_, _] : Timer](scalaDuration: ScalaDuration): F[Nothing, Unit] = Timer[F].sleep(scalaDuration)
}
