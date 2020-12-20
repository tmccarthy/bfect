package au.id.tmm.bfect.catsinterop.instances

import au.id.tmm.bfect.effects.Async
import cats.data.EitherT

class AsyncInstance[F[_] : cats.effect.Async] private[instances] extends SyncInstance[F] with Async[EitherT[F, *, *]] {
  override def asyncF[E, A](registerForBfect: (Either[E, A] => Unit) => EitherT[F, Nothing, _]): EitherT[F, E, A] = {
    val registerForCats: (Either[Throwable, Either[E, A]] => Unit) => F[Unit] = {
      cbForCats: (Either[Throwable, Either[E, A]] => Unit) =>
        val cbForBfect: Either[E, A] => Unit = either => cbForCats(Right(either))

        cats.effect.Async[F].as(registerForBfect(cbForBfect).value, ())
    }

    EitherT(cats.effect.Async[F].asyncF[Either[E, A]](registerForCats))
  }
}
