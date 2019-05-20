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
package au.id.tmm.bfect.io

import au.id.tmm.bfect.{ExitCase, Failure}

sealed trait IO[+E, +A] {

  def map[A2](f: A => A2): IO[E, A2] = this match {
    case IO.Pure(a)           => IO.Pure(f(a))
    case self: IO.Fail[E]  => self
    case self: IO[E, A]       => IO.FlatMap(self, (a: A) => IO.Pure(f(a)))
  }

  def leftMap[E2](f: E => E2): IO[E2, A] =
    foldM(f.andThen(IO.leftPure), IO.Pure(_))

  def biMap[E2, A2](leftF: E => E2, rightF: A => A2): IO[E2, A2] =
    foldM(leftF.andThen(IO.leftPure), rightF.andThen(IO.Pure(_)))

  def fold[A2](leftF: E => A2, rightF: A => A2): IO[Nothing, A2] =
    foldM(leftF.andThen(IO.Pure(_)), rightF.andThen(IO.Pure(_)))

  def foldM[E2, A2](leftF: E => IO[E2, A2], rightF: A => IO[E2, A2]): IO[E2, A2] =
    foldCauseM(
      leftF = {
        case Failure.Checked(e)             => leftF(e)
        case Failure.Interrupted            => IO.Fail(Failure.Interrupted)
        case cause @ Failure.Unchecked(_)   => IO.Fail(cause)
      },
      rightF,
    )

  def foldCauseM[E2, A2](leftF: Failure[E] => IO[E2, A2], rightF: A => IO[E2, A2]): IO[E2, A2] =
    IO.FoldM(this, leftF, rightF)

  def flatMap[E2 >: E, A2](f: A => IO[E2, A2]): IO[E2, A2] = this match {
    case IO.FlatMap(io, f2) => IO.FlatMap(io, f2.andThen(_.flatMap(f)))
    case self: IO.Fail[E]   => self
    case self: IO[E, A]     => IO.FlatMap(self, f)
  }

  def flatten[E2 >: E, A1](implicit e: A <:< IO[E2, A1]): IO[E2, A1] = flatMap(io => io)

  def ensure(finalizer: IO[Nothing, _]): IO[E, A] =
    ensureCase(_ => finalizer)

  def ensureCase(finalizer: ExitCase[E, A] => IO[Nothing, _]): IO[E, A] =
    this match {
      case IO.Ensure(io, finalizer1) => IO.Ensure(io, (exit: ExitCase[E, A]) => finalizer1(exit).flatMap(_ => finalizer(exit)))
      case self                      => IO.Ensure(self, finalizer)
    }

  def fork: IO[Nothing, IOFibre[E, A]] = ???

}

object IO {

  def pure[A](a: A): IO[Nothing, A] = Pure(a)

  val unit: IO[Nothing, Unit] = pure(Unit)

  def leftPure[E](e: E): IO[E, Nothing] = Fail(Failure.Checked(e))

  def sync[A](block: => A): IO[Nothing, A] = Effect(() => block)

  def bracket[R, E, A](acquire: IO[E, R])(release: R => IO[Nothing, _])(use: R => IO[E, A]): IO[E, A] =
    for {
      resource <- acquire
      result <- use(resource).ensure(release(resource))
    } yield result

  def bracketCase[R, E, A](acquire: IO[E, R])(release: (R, ExitCase[E, A]) => IO[Nothing, _])(use: R => IO[E, A]): IO[E, A] =
    for {
      resource <- acquire
      result <- use(resource).ensureCase(exitCase => release(resource, exitCase))
    } yield result

  def racePair[E, A, B](left: IO[E, A], right: IO[E, B]): IO[E, Either[(A, IOFibre[E, B]), (IOFibre[E, A], B)]] = ???

  final case class Pure[A](a: A) extends IO[Nothing, A]
  final case class Fail[E](cause: Failure[E]) extends IO[E, Nothing]
  final case class FlatMap[E, A, A2](io: IO[E, A], f: A => IO[E, A2]) extends IO[E, A2]
  final case class FoldM[E, E2, A, A2](io: IO[E, A], leftF: Failure[E] => IO[E2, A2], rightF: A => IO[E2, A2]) extends IO[E2, A2]
  final case class Effect[A](block: () => A) extends IO[Nothing, A]
  final case class Ensure[E, A](io: IO[E, A], finalizer: ExitCase[E, A] => IO[Nothing, _]) extends IO[E, A]

}
