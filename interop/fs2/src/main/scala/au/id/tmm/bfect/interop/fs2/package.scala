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
package au.id.tmm.bfect.interop

import _root_.fs2.Stream
import au.id.tmm.bfect.effects.{Bracket, Sync}

package object fs2 {

  type Fs2Compiler[F[+_, +_]] = Stream.Compiler[F[Throwable, +*], F[Throwable, +*]]

  implicit def fs2CompilerForBfect[F[+_, +_] : Sync : Bracket]: Fs2Compiler[F] =
    Stream.Compiler.syncInstance[F[Throwable, +*]](cats.instances.sync.bfectSyncIsCatsSync)

}
