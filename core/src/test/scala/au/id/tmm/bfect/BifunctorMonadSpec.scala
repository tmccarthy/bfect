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
package au.id.tmm.bfect

import au.id.tmm.bfect.BifunctorMonadSpec._
import org.scalatest.FlatSpec

class BifunctorMonadSpec extends FlatSpec {

  import BifunctorMonad._
  import EitherInstances.biFunctorMonadInstance

  "absolve" should "absolve when the outer error type is Nothing" in {
    val nestedEither: Either[Nothing, Either[ChildError, Unit]] = Right(Right(()))

    val absolved: Either[ChildError, Unit] = nestedEither.absolve

    assert(absolved === Right(()))
  }

  it should "absolve when the outer error type is a parent of the inner error type" in {
    val nestedEither: Either[ParentError, Either[ChildError, Unit]] = Right(Right(()))

    val absolved: Either[ParentError, Unit] = nestedEither.absolve

    assert(absolved === Right(()))
  }

  "absolveOption" should "absolve when the outer error type is Nothing" in {
    val eitherOption: Either[Nothing, Option[Unit]] = Right(Some(()))

    val absolved: Either[ChildError, Unit] = eitherOption.absolveOption(ChildError.Instance)

    assert(absolved === Right(()))
  }

  it should "absolve when the outer error type is a parent of the supplied error type" in {
    val eitherOption: Either[ParentError, Option[Unit]] = Right(Some(()))

    val absolved: Either[ParentError, Unit] = eitherOption.absolveOption(ChildError.Instance)

    assert(absolved === Right(()))
  }

  it should "absolve when the outer error type is a child of the supplied error type" in {
    val eitherOption: Either[ChildError, Option[Unit]] = Right(Some(()))

    val absolved: Either[ParentError, Unit] = eitherOption.absolveOption(ParentError.Instance)

    assert(absolved === Right(()))
  }

}

object BifunctorMonadSpec {
  sealed trait ParentError

  object ParentError {
    case object Instance extends ParentError
  }

  sealed trait ChildError extends ParentError

  object ChildError {
    case object Instance extends ChildError
  }
}
