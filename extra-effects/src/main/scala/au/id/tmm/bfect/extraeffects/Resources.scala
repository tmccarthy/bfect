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
package au.id.tmm.bfect.extraeffects

import java.io.IOException
import java.nio.charset.Charset

import au.id.tmm.bfect.effects.Sync
import org.apache.commons.io.IOUtils

trait Resources[F[+_, +_]] {
  def resourceAsString(resourceName: String, charset: Charset = Charset.forName("UTF-8")): F[IOException, Option[String]]
}

object Resources {

  def apply[F[+_, +_] : Resources]: Resources[F] = implicitly[Resources[F]]

  trait SyncInstance {
    implicit def resourcesSyncInstance[F[+_, +_] : Sync]: Resources[F] = new Resources[F] {
      override def resourceAsString(resourceName: String, charset: Charset): F[IOException, Option[String]] = {
        val syncInstance = Sync[F]

        import syncInstance._

        bracket(sync(Option(Resources.getClass.getResourceAsStream(resourceName))))(is => sync(is.foreach(_.close()))) { maybeInputStream =>
          syncCatch {
            maybeInputStream.map(IOUtils.toString(_, charset))
          } {
            case ioException: IOException => ioException
          }
        }

      }
    }
  }

  object SyncInstance extends SyncInstance

}
