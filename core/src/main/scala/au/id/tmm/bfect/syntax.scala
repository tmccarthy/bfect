package au.id.tmm.bfect

import au.id.tmm.bfect.effects._

object syntax {

  object biFunctionK extends BiFunctionK.Syntax

  object biInvariantK        extends BiInvariantK.ToBiInvariantKOps
  object biInvariant         extends BiInvariant.ToBiInvariantOps
  object bifunctor           extends Bifunctor.ToBifunctorOps
  object bifunctorMonad      extends BifunctorMonad.ToBifunctorMonadOps
  object bifunctorMonadError extends BifunctorMonadError.ToBifunctorMonadErrorOps

  object die        extends Die.ToDieOps
  object timer      extends Timer.ToTimerOps
  object sync       extends Sync.ToSyncOps
  object bracket    extends Bracket.ToBracketOps
  object async      extends Async.ToAsyncOps
  object concurrent extends Concurrent.ToConcurrentOps

  object all
      extends AnyRef
      with BiFunctionK.Syntax
      with BiInvariantK.ToBiInvariantKOps
      with BiInvariant.ToBiInvariantOps
      with Bifunctor.ToBifunctorOps
      with BifunctorMonad.ToBifunctorMonadOps
      with BifunctorMonadError.ToBifunctorMonadErrorOps
      with Die.ToDieOps
      with Timer.ToTimerOps
      with Sync.ToSyncOps
      with Bracket.ToBracketOps
      with Async.ToAsyncOps
      with Concurrent.ToConcurrentOps
}
