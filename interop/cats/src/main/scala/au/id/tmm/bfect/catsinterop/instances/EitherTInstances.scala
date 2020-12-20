package au.id.tmm.bfect.catsinterop.instances

import au.id.tmm.bfect.{Bifunctor, BifunctorMonadError}
import cats.data.EitherT
import cats.{Functor, Monad}

trait FirstPriorityEitherTInstances extends SecondPriorityEitherTInstances {
  implicit def bmeInstance[F[_]](implicit monadError: Monad[F]): BifunctorMonadError[EitherT[F, *, *]] =
    new BMEInstance()
}

trait SecondPriorityEitherTInstances extends ThirdPriorityEitherTInstances {
}

trait ThirdPriorityEitherTInstances {
  implicit def biFunctorInstance[F[_]](implicit functor: Functor[F]): Bifunctor[EitherT[F, *, *]] =
    new BifunctorInstance[F]()
}
