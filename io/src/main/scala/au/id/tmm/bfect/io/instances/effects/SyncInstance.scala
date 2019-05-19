package au.id.tmm.bfect.io.instances.effects

import au.id.tmm.bfect.io.IO
import au.id.tmm.bfect.typeclasses.effects.Sync

class SyncInstance private[instances]() extends BracketInstance with Sync[IO] {
  override def suspend[E, A](effect: => IO[E, A]): IO[E, A] = IO.sync(effect).flatten
}
