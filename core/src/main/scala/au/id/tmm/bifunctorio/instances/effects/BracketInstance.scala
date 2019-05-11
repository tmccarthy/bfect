package au.id.tmm.bifunctorio.instances.effects

import au.id.tmm.bifunctorio.IO
import au.id.tmm.bifunctorio.instances.BMEInstance
import au.id.tmm.bifunctorio.typeclasses.ExitCase
import au.id.tmm.bifunctorio.typeclasses.effects.Bracket

class BracketInstance private[instances]() extends BMEInstance with Bracket[IO] {

  override def bracketCase[R, E, A](acquire: IO[E, R])(release: (R, ExitCase[E, A]) => IO[Nothing, _])(use: R => IO[E, A]): IO[E, A] =
    IO.bracketCase(acquire)(release)(use)

  override def bracket[R, E, A](acquire: IO[E, R])(release: R => IO[Nothing, _])(use: R => IO[E, A]): IO[E, A] =
    IO.bracket(acquire)(release)(use)

  override def ensure[E, A](fea: IO[E, A])(finalizer: IO[Nothing, _]): IO[E, A] =
    fea.ensure(finalizer)

  override def ensureCase[E, A](fea: IO[E, A])(finalizer: ExitCase[E, A] => IO[Nothing, _]): IO[E, A] =
    fea.ensureCase(finalizer)

}
