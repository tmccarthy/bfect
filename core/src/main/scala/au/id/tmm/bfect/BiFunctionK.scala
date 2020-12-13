package au.id.tmm.bfect

trait BiFunctionK[F[_, _], G[_, _]] {
  def apply[L, R](flr: F[L, R]): G[L, R]
}

object BiFunctionK {
}
