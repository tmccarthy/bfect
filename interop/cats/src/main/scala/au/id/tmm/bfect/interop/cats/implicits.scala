package au.id.tmm.bfect.interop.cats

import au.id.tmm.bfect.interop.cats.instanceimpls.eitherT.EitherTInstanceTraits0
import au.id.tmm.bfect.interop.cats.instanceimpls.implicitconversions._

object implicits
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
