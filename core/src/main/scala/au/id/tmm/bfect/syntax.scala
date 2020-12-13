package au.id.tmm.bfect

object syntax {

  //noinspection NonAsciiCharacters
  type ≈>[F[_, _], G[_, _]] = BiFunctionK[F, G]

  object biInvariantK extends BiInvariantK.ToBiInvariantKOps
  object biInvariant extends BiInvariant.ToBiInvariantOps

}
