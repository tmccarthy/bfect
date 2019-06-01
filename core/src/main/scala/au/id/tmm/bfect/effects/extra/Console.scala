package au.id.tmm.bfect.effects.extra

import au.id.tmm.bfect.BifunctorMonad

trait Console[F[+_, +_]] extends BifunctorMonad[F] {

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
}
