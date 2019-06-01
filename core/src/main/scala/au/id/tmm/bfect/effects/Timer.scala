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

import au.id.tmm.bfect.effects.Timer.convertScalaDurationToJavaDuration
import au.id.tmm.bfect.{BifunctorMonad, BifunctorMonadStaticOps}

import scala.concurrent.duration.{Duration => ScalaDuration, FiniteDuration => FiniteScalaDuration}

trait Timer[F[+_, +_]] extends BifunctorMonad[F] with Now[F] {

  def sleep(duration: Duration): F[Nothing, Unit]

  def sleep(scalaDuration: ScalaDuration): F[Nothing, Unit] =
    sleep(convertScalaDurationToJavaDuration(scalaDuration))

  def repeatFixedDelay[E](fea: F[E, _])(delay: Duration): F[E, Nothing] =
    forever(flatMap(fea)(_ => sleep(delay)))

  def repeatFixedDelay[E](fea: F[E, _], delayAsScalaDuration: ScalaDuration): F[E, Nothing] =
    repeatFixedDelay(fea)(convertScalaDurationToJavaDuration(delayAsScalaDuration))

  def repeatFixedRate[E](fea: F[E, _])(period: Duration): F[E, Nothing] = {
    def repeatFixedRateStartingAt(t0: Long, period: Long): F[E, Nothing] = flatMap(fea) { _ =>
      flatMap(now) { instantCompleted =>
        val tCompleted = instantCompleted.toEpochMilli
        val nextStart = Instant.ofEpochMilli(    (period - ((tCompleted - t0) % period)) + tCompleted      )
        val sleepDuration = Duration.between(instantCompleted, nextStart)
        flatMap(sleep(sleepDuration)) { _ =>
          repeatFixedRateStartingAt(t0, period)
        }
      }
    }

    flatMap(now) { instantStarted =>
      repeatFixedRateStartingAt(instantStarted.toEpochMilli, period.toMillis)
    }
  }

  def repeatFixedRate[E](fea: F[E, _], periodAsScalaDuration: ScalaDuration): F[E, Nothing] =
    repeatFixedRate(fea)(convertScalaDurationToJavaDuration(periodAsScalaDuration))

}

object Timer extends TimerStaticOps {

  def apply[F[+_, +_] : Timer]: Timer[F] = implicitly[Timer[F]]

  implicit class Ops[F[+_, +_], E, A](protected val fea: F[E, A])(implicit protected val timerInstance: Timer[F])
    extends BifunctorMonad.Ops[F, E, A](fea)
      with TimerOps[F, E, A]

  private[effects] def convertScalaDurationToJavaDuration(scalaDuration: ScalaDuration): Duration = scalaDuration match {
    case ScalaDuration.MinusInf => Duration.ofSeconds(Long.MinValue, 0)
    case _: ScalaDuration.Infinite => Duration.ofSeconds(Long.MaxValue, 999999999)
    case finiteDuration: FiniteScalaDuration => Duration.ofNanos(finiteDuration.toNanos)
  }

}

trait TimerOps[F[+_, +_], E, A] {
  protected def fea: F[E, A]
  protected def timerInstance: Timer[F]

  def repeatFixedDelay(delay: Duration): F[E, Nothing] = timerInstance.repeatFixedDelay(fea)(delay)
  def repeatFixedDelay(delayAsScalaDuration: ScalaDuration): F[E, Nothing] = timerInstance.repeatFixedDelay(fea, delayAsScalaDuration)
  def repeatFixedRate(period: Duration): F[E, Nothing] = timerInstance.repeatFixedRate(fea)(period)
  def repeatFixedRate(periodAsScalaDuration: ScalaDuration): F[E, Nothing] = timerInstance.repeatFixedRate(fea, periodAsScalaDuration)
}

trait TimerStaticOps extends BifunctorMonadStaticOps with NowStaticOps {
  def sleep[F[+_, +_] : Timer](duration: Duration): F[Nothing, Unit] = Timer[F].sleep(duration)
  def sleep[F[+_, +_] : Timer](scalaDuration: ScalaDuration): F[Nothing, Unit] = Timer[F].sleep(scalaDuration)
}