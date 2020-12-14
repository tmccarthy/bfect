package au.id.tmm.bfect

object implicits
    extends AnyRef
    with BiFunctionK.Syntax
    with BiInvariantK.ToBiInvariantKOps
    with BiInvariant.ToBiInvariantOps
    with Bifunctor.ToBifunctorOps
    with BifunctorMonad.ToBifunctorMonadOps
    with BifunctorMonadError.ToBifunctorMonadErrorOps
    with effects.Die.ToDieOps
    with effects.Timer.ToTimerOps
    with effects.Sync.ToSyncOps
    with effects.Bracket.ToBracketOps
    with effects.Async.ToAsyncOps
    with effects.Concurrent.ToConcurrentOps
    with instances.EitherInstances
