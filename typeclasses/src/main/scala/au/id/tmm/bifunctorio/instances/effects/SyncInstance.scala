package au.id.tmm.bifunctorio.instances.effects

import au.id.tmm.bifunctorio.IO
import au.id.tmm.bifunctorio.typeclasses.effects.Sync

class SyncInstance private[instances]() extends BracketInstance with Sync[IO] {
  override def suspend[E, A](effect: => IO[E, A]): IO[E, A] = IO.sync(effect).flatten
}
