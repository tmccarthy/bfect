package au.id.tmm.bfect.io.instances

import au.id.tmm.bfect.io.IO
import au.id.tmm.bfect.typeclasses.BME

class BMEInstance private[instances]() extends BifunctorMonadInstance with BME[IO] {
  override def handleErrorWith[E1, A, E2](fea: IO[E1, A])(f: E1 => IO[E2, A]): IO[E2, A] = fea.foldM(f, IO.pure)
}
