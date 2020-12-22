package au.id.tmm.bfect.interop.cats

import au.id.tmm.bfect.interop.cats.instanceimpls.eitherT.EitherTInstanceTraits0
import au.id.tmm.bfect.interop.cats.instanceimpls.implicitconversions._

object instances {
  object eitherT extends EitherTInstanceTraits0

  object bifunctor  extends CatsBifunctorForBfectBifunctor.ToCatsBifunctor
  object monad      extends CatsMonadForBfectBifunctorMonad.ToCatsMonad
  object monadError extends CatsMonadErrorForBfectBME.ToCatsMonadError
  object sync       extends CatsSyncForBfectSync.ToCatsSync
  object async      extends CatsAsyncForBfectAsync.ToCatsAsync
  object concurrent extends CatsConcurrentForBfectConcurrent.ToCatsConcurrent
  object clock      extends CatsClockForBfectNow.ToCatsClock
  object timer      extends CatsTimerForBfectTimer.ToCatsTimer

  object all
      extends AnyRef
      with EitherTInstanceTraits0
      with CatsBifunctorForBfectBifunctor.ToCatsBifunctor
      with CatsMonadForBfectBifunctorMonad.ToCatsMonad
      with CatsMonadErrorForBfectBME.ToCatsMonadError
      with CatsSyncForBfectSync.ToCatsSync
      with CatsAsyncForBfectAsync.ToCatsAsync
      with CatsConcurrentForBfectConcurrent.ToCatsConcurrent
      with CatsClockForBfectNow.ToCatsClock
      with CatsTimerForBfectTimer.ToCatsTimer
}
