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

import au.id.tmm.bfect.effects.Timer.Ops
import au.id.tmm.bfect.effects.TimerSpec.{TimerTestIO, TimerTestState, dummyTask}
import au.id.tmm.bfect.testing.BState
import org.scalatest.FlatSpec

import scala.concurrent.duration.{Duration => ScalaDuration}

// Ideally this test would be in the tests for bfect-core, but sbt can't handle the cyclic dependency on BState
class TimerSpec extends FlatSpec {

  // Need to bring this into scope to override the default implementation provided by the `Concurrent` instance
  private implicit val timerInstance: Timer[TimerTestIO] = TimerSpec.timerInstanceForTests

  "repeating with a fixed delay" should "repeat with the same delay length if the task takes less than the duration" in {
    val task = dummyTask(Duration.ofSeconds(1), failsAfter = Instant.EPOCH.plusSeconds(10))

    val stateAfterExecution = task
      .repeatFixedDelay(Duration.ofSeconds(5))
      .runS(TimerTestState(Instant.EPOCH))

    val expectedTaskExecutionTimes = List(
      Instant.EPOCH,
      Instant.EPOCH.plusSeconds(6),
      Instant.EPOCH.plusSeconds(12),
    )

    assert(stateAfterExecution.taskExecutionTimes === expectedTaskExecutionTimes)
  }

  it should "repeat with the same delay even if the task takes more than the duration" in {
    val task = dummyTask(Duration.ofSeconds(6), failsAfter = Instant.EPOCH.plusSeconds(20))

    val stateAfterExecution = task
      .repeatFixedDelay(Duration.ofSeconds(5))
      .runS(TimerTestState(Instant.EPOCH))

    val expectedTaskExecutionTimes = List(
      Instant.EPOCH,
      Instant.EPOCH.plusSeconds(11),
      Instant.EPOCH.plusSeconds(22),
    )

    assert(stateAfterExecution.taskExecutionTimes === expectedTaskExecutionTimes)
  }

  "repeating with a fixed rate" should "repeat with the same period if the task takes less than the period" in {
    val task = dummyTask(Duration.ofSeconds(1), failsAfter = Instant.EPOCH.plusSeconds(10))

    val stateAfterExecution = task
      .repeatFixedRate(Duration.ofSeconds(5))
      .runS(TimerTestState(Instant.EPOCH))

    val expectedTaskExecutionTimes = List(
      Instant.EPOCH,
      Instant.EPOCH.plusSeconds(5),
      Instant.EPOCH.plusSeconds(10),
      Instant.EPOCH.plusSeconds(15),
    )

    assert(stateAfterExecution.taskExecutionTimes === expectedTaskExecutionTimes)
  }

  it should "repeat with the same period if the initial time is nonzero" in {
    val t0 = Instant.EPOCH.plusSeconds(42)

    val task = dummyTask(Duration.ofSeconds(1), failsAfter = t0.plusSeconds(10))

    val stateAfterExecution = task
      .repeatFixedRate(Duration.ofSeconds(5))
      .runS(TimerTestState(t0))

    val expectedTaskExecutionTimes = List(
      t0,
      t0.plusSeconds(5),
      t0.plusSeconds(10),
      t0.plusSeconds(15),
    )

    assert(stateAfterExecution.taskExecutionTimes === expectedTaskExecutionTimes)
  }

  it should "skip executions if the task takes more than the period" in {
    val task = dummyTask(Duration.ofSeconds(6), failsAfter = Instant.EPOCH.plusSeconds(20))

    val stateAfterExecution = task
      .repeatFixedRate(Duration.ofSeconds(5))
      .runS(TimerTestState(Instant.EPOCH))

    val expectedTaskExecutionTimes = List(
      Instant.EPOCH,
      Instant.EPOCH.plusSeconds(10),
      Instant.EPOCH.plusSeconds(20),
      Instant.EPOCH.plusSeconds(30),
    )

    assert(stateAfterExecution.taskExecutionTimes === expectedTaskExecutionTimes)
  }

  "the conversion of scala durations to java durations" should "convert an infinite scala duration to the maximum java duration" in {
    val javaDuration = Timer.convertScalaDurationToJavaDuration(ScalaDuration.Inf)

    assert(javaDuration.getSeconds === Long.MaxValue)
    assert(javaDuration.getNano === 999999999)
  }

  it should "convert an undefined scala duration to the maximum java duration" in {
    val javaDuration = Timer.convertScalaDurationToJavaDuration(ScalaDuration.Undefined)

    assert(javaDuration.getSeconds === Long.MaxValue)
    assert(javaDuration.getNano === 999999999)
  }

  it should "convert a negative infinite scala duration to the minimum java duration" in {
    val javaDuration = Timer.convertScalaDurationToJavaDuration(ScalaDuration.MinusInf)

    assert(javaDuration.getSeconds === Long.MinValue)
    assert(javaDuration.getNano === 0)
  }

  it should "convert a zero scala duration to the zero java duration" in {
    val javaDuration = Timer.convertScalaDurationToJavaDuration(ScalaDuration.Zero)

    assert(javaDuration.getSeconds === 0)
    assert(javaDuration.getNano === 0)
  }

}

object TimerSpec {

  private final case class TimerTestState(now: Instant, taskExecutionTimes: List[Instant] = List.empty) {
    def proceedBy(duration: Duration): TimerTestState = TimerTestState(now.plus(duration), taskExecutionTimes)
    def executeTask: TimerTestState = TimerTestState(now, taskExecutionTimes = taskExecutionTimes :+ now)
  }

  private type TimerTestIO[+E, +A] = BState[TimerTestState, E, A]

  private implicit val timerInstanceForTests: Timer[TimerTestIO] = new BState.TimerInstance[TimerTestState] {
    override def nowFromState(state: TimerTestState): (TimerTestState, Instant) = (state, state.now)

    override def applySleepToState(sleepDuration: Duration, state: TimerTestState): TimerTestState =
      state.proceedBy(sleepDuration)
  }

  private def dummyTask(duration: Duration, failsAfter: Instant): TimerTestIO[Unit, Unit] =
    BState { oldState =>
      if (oldState.now.isAfter(failsAfter)) {
        (oldState.executeTask, Left(()))
      } else {
        (oldState.executeTask.proceedBy(duration), Right(()))
      }
    }

}
