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

import au.id.tmm.utilities.testing.ImprovedFlatSpec

class TmmToCatsTypeclassConversionsSpec extends ImprovedFlatSpec {

  "the monad instance" should "be resolved without difficulty" in {
    import au.id.tmm.bfect.EitherInstances._

    cats.Monad[Either[Nothing, ?]]

    succeed
  }

  "the MonadError instance" should "be resolved without difficulty" in {
    import au.id.tmm.bfect.EitherInstances._

    cats.MonadError[Either[Nothing, ?], Nothing]

    succeed
  }

}
