package au.id.tmm.bfect.catsinterop.instances

import au.id.tmm.bfect.{Bifunctor, BifunctorMonad, BifunctorMonadError}
import cats.data.EitherT
import cats.{Functor, Monad}

trait FirstPriorityEitherTInstances extends SecondPriorityEitherTInstances {
  implicit def bmeInstance[F[_]](implicit monadError: Monad[F]): BifunctorMonadError[EitherT[F, *, *]] =
    new BMEInstance()
}

trait SecondPriorityEitherTInstances extends ThirdPriorityEitherTInstances {
  implicit def biFunctorMonadInstance[F[_]](implicit monad: Monad[F]): BifunctorMonad[EitherT[F, *, *]] =
    new BifunctorMonadInstance[F]()
}

trait ThirdPriorityEitherTInstances {
  implicit def biFunctorInstance[F[_]](implicit functor: Functor[F]): Bifunctor[EitherT[F, *, *]] =
    new BifunctorInstance[F]()
}
