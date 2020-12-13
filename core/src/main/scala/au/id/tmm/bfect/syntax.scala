package au.id.tmm.bfect

import au.id.tmm.bfect.effects._

object syntax {

  //noinspection NonAsciiCharacters
  // TODO should move
  type ≈>[F[_, _], G[_, _]] = BiFunctionK[F, G]

  object biInvariantK   extends BiInvariantK.ToBiInvariantKOps
  object biInvariant    extends BiInvariant.ToBiInvariantOps
  object bifunctor      extends Bifunctor.ToBifunctorOps
  object bifunctorMonad extends BifunctorMonad.ToBifunctorMonadOps
  object bifunctorMonadError extends BifunctorMonadError.ToBifunctorMonadErrorOps

  object die extends Die.ToDieOps
  object timer extends Timer.ToTimerOps

}
