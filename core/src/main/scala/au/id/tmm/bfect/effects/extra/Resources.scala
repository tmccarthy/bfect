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
package au.id.tmm.bfect.effects.extra

import java.io.{IOException, InputStream}
import java.nio.charset.Charset

import au.id.tmm.bfect.effects.Sync
import au.id.tmm.bfect.effects.extra.Resources.ResourceStreamError

import scala.io.Source

trait Resources[F[+_, +_]] extends Sync[F] {

  def getResourceAsStream(resourceName: String): F[Nothing, Option[InputStream]]

  def useResourceAsStream[E, A](resourceName: String)(use: InputStream => F[E, A]): F[ResourceStreamError[E], A] =
    bracket[InputStream, ResourceStreamError[E], A](
      acquire = {
        flatMap(getResourceAsStream(resourceName)) {
          case Some(stream) => pure(stream): F[ResourceStreamError[E], InputStream]
          case None         => leftPure(ResourceStreamError.ResourceNotFound): F[ResourceStreamError[E], InputStream]
        }
      }
    )(
      release = stream => sync(stream.close())
    )(
      use = use.andThen(fea => leftMap(fea)(ResourceStreamError.UseError.apply))
    )

  def resourceAsString(resourceName: String, charset: Charset = Charset.forName("UTF-8")): F[ResourceStreamError[IOException], String] =
    useResourceAsStream(resourceName) { inputStream =>
      syncCatch(Source.fromInputStream(inputStream, charset.toString).mkString) {
        case e: IOException => e
      }
    }

}

object Resources {

  def apply[F[+_, +_] : Resources]: Resources[F] = implicitly[Resources[F]]

  sealed trait ResourceStreamError[+E]

  object ResourceStreamError {
    case object ResourceNotFound extends ResourceStreamError[Nothing]
    final case class UseError[E](cause: E) extends ResourceStreamError[E]
  }

  trait Live[F[+_, +_]] extends Resources[F] { self: Sync[F] =>
    override def getResourceAsStream(resourceName: String): F[Nothing, Option[InputStream]] =
      sync(Option(getClass.getResourceAsStream(resourceName)))
  }

}
