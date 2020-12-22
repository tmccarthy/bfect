package au.id.tmm.bfect.interop.cats.instanceimpls.implicitconversions

import au.id.tmm.bfect.Fibre
import au.id.tmm.bfect.effects.{Async, Bracket, Concurrent}

class CatsConcurrentForBfectConcurrent[F[_, _]](
  implicit
  bfectBracket: Bracket[F],
  bfectAsync: Async[F],
  bfectConcurrent: Concurrent[F],
) extends CatsAsyncForBfectAsync
    with cats.effect.Concurrent[F[Throwable, *]] {
  override def start[A](fa: F[Throwable, A]): F[Throwable, cats.effect.Fiber[F[Throwable, *], A]] =
    bfectAsync.map(bfectAsync.asThrowableFallible(bfectConcurrent.start(fa)))(asCatsFiber)

  override def racePair[A, B](
    fa: F[Throwable, A],
    fb: F[Throwable, B],
  ): F[Throwable, Either[(A, cats.effect.Fiber[F[Throwable, *], B]), (cats.effect.Fiber[F[Throwable, *], A], B)]] =
    bfectAsync.map(bfectConcurrent.racePair(fa, fb)) {
      case Left((a, bFiber))  => Left((a, asCatsFiber(bFiber)))
      case Right((aFiber, b)) => Right((asCatsFiber(aFiber), b))
    }

  private def asCatsFiber[A](bfectFibre: Fibre[F, Throwable, A]): cats.effect.Fiber[F[Throwable, *], A] =
    new cats.effect.Fiber[F[Throwable, *], A] {
      override def cancel: cats.effect.CancelToken[F[Throwable, *]] =
        bfectAsync.asThrowableFallible(bfectFibre.cancel)

      override def join: F[Throwable, A] = bfectFibre.join
    }

  override def race[A, B](fa: F[Throwable, A], fb: F[Throwable, B]): F[Throwable, Either[A, B]] =
    bfectConcurrent.race(fa, fb)

  override def cancelable[A](
    k: (Either[Throwable, A] => Unit) => cats.effect.CancelToken[F[Throwable, *]],
  ): F[Throwable, A] = bfectConcurrent.cancelable(k.andThen(FailureHandlingUtils.makeFailureUnchecked(_)))
}

object CatsConcurrentForBfectConcurrent {
  trait ToCatsConcurrent {
    implicit def bfectConcurrentIsCatsConcurrent[
      F[_, _] : Concurrent : Async : Bracket,
    ]: cats.effect.Concurrent[F[Throwable, *]] = new CatsConcurrentForBfectConcurrent[F]()
  }
}
