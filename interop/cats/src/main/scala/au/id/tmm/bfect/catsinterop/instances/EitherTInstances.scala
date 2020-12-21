package au.id.tmm.bfect.catsinterop.instances

import au.id.tmm.bfect
import cats.data.EitherT
import cats.{Functor, Monad}

trait EitherTInstanceTraits0 extends EitherTInstanceTraits1 {
  implicit def bfectExtraEffectInstanceForCatsEitherT[F[_] : cats.effect.Sync]: AnyRef
    with bfect.effects.extra.Calendar[EitherT[F, *, *]]
    with bfect.effects.extra.Console[EitherT[F, *, *]]
    with bfect.effects.extra.EnvVars[EitherT[F, *, *]]
    with bfect.effects.extra.Resources[EitherT[F, *, *]] = new ExtraEffectsInstances[F]

  implicit def bfectTimerInstanceForCatsEitherT[
    F[_] : cats.effect.Timer : Monad,
  ]: bfect.effects.Timer[EitherT[F, *, *]] =
    new TimerInstance[F]

  implicit def bfectBracketInstanceForCatsEitherT[F[_]](
    implicit
    bracket: cats.effect.Bracket[F, Throwable],
  ): bfect.effects.Bracket[EitherT[F, *, *]] = new BracketInstance[F]

  implicit def bfectConcurrentInstanceForCatsEitherT[
    F[_] : cats.effect.Concurrent,
  ]: bfect.effects.Concurrent[EitherT[F, *, *]] =
    new ConcurrentInstance[F]

  implicit def bfectAsyncInstanceForCatsEitherT[F[_] : cats.effect.Async]: bfect.effects.Async[EitherT[F, *, *]] =
    new AsyncInstance[F]

}

private[instances] trait EitherTInstanceTraits1 extends EitherTInstanceTraits2 {

  implicit def bfectNowInstanceForCatsEitherT[F[_] : cats.effect.Clock : Functor]: bfect.effects.Now[EitherT[F, *, *]] =
    new NowInstance[F]

  implicit def bfectSyncInstanceForCatsEitherT[F[_] : cats.effect.Sync]: bfect.effects.Sync[EitherT[F, *, *]] =
    new SyncInstance[F]

}

private[instances] trait EitherTInstanceTraits2 extends EitherTInstanceTraits3 {

  implicit def bfectDieInstanceForCatsEitherT[F[_]](
    implicit
    monadError: cats.MonadError[F, Throwable],
  ): bfect.effects.Die[EitherT[F, *, *]] = new DieInstance[F]

}

private[instances] trait EitherTInstanceTraits3 extends EitherTInstanceTraits4 {

  implicit def bfectBmeInstanceForCatsEitherT[F[_] : Monad]: bfect.BifunctorMonadError[EitherT[F, *, *]] =
    new BMEInstance[F]

}

private[instances] trait EitherTInstanceTraits4 {

  implicit def bfectBifunctorInstanceForCatsEitherT[F[_] : Functor]: bfect.Bifunctor[EitherT[F, *, *]] =
    new BifunctorInstance[F]

}
