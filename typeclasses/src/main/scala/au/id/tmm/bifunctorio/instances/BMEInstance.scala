package au.id.tmm.bifunctorio.instances

import au.id.tmm.bifunctorio.IO
import au.id.tmm.bifunctorio.typeclasses.BME

class BMEInstance private[instances]() extends BiFunctorMonadInstance with BME[IO] {
  override def handleErrorWith[E1, A, E2](fea: IO[E1, A])(f: E1 => IO[E2, A]): IO[E2, A] = fea.foldM(f, IO.pure)
}
