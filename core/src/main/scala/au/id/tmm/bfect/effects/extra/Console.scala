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

import au.id.tmm.bfect.effects.Sync

trait Console[F[+_, +_]] {

  def lineSeparator: String

  def print(string: String): F[Nothing, Unit]
  def println(string: String): F[Nothing, Unit] = print(string + lineSeparator)

  def printStdOut(string: String): F[Nothing, Unit] = print(string)
  def printlnStdOut(string: String): F[Nothing, Unit] = println(string)

  def printStdErr(string: String): F[Nothing, Unit]
  def printlnStdErr(string: String): F[Nothing, Unit] = print(string + lineSeparator)

}

object Console {
  def apply[F[+_, +_] : Console]: Console[F] = implicitly[Console[F]]

  trait Live[F[+_, +_]] extends Console[F] { self: Sync[F] =>
    override val lineSeparator: String = System.lineSeparator()
    override def print(string: String): F[Nothing, Unit]         = sync(scala.Console.print(string))
    override def println(string: String): F[Nothing, Unit]       = sync(scala.Console.println(string))
    override def printStdErr(string: String): F[Nothing, Unit]   = sync(scala.Console.err.print(string))
    override def printlnStdErr(string: String): F[Nothing, Unit] = sync(scala.Console.err.println(string))
  }
}
