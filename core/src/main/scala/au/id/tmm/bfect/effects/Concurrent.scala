package au.id.tmm.bfect.effects

import au.id.tmm.bfect.Fibre

trait Concurrent[F[+_, +_]] extends Async[F] {

  def start[E, A](fea: F[E, A]): F[Nothing, Fibre[F, E, A]]

  def racePair[E, A, B](fea: F[E, A], feb: F[E, B]): F[E, Either[(A, Fibre[F, E, B]), (Fibre[F, E, A], B)]]

  def race[E, A, B](fea: F[E, A], feb: F[E, B]): F[E, Either[A, B]] =
  /*_*/
    flatMap(racePair(fea, feb)) {
      case Left((a, fibreForB)) => map(fibreForB.cancel)(_ => Left(a))
      case Right((fibreForA, b)) => map(fibreForA.cancel)(_ => Right(b))
    }
  /*_*/

  def cancelable[E, A](k: (Either[E, A] => Unit) => F[Nothing, _]): F[E, A]

}

object Concurrent {
  def apply[F[+_, +_] : Concurrent]: Concurrent[F] = implicitly[Concurrent[F]]
}

