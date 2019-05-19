package au.id.tmm.bfect

import au.id.tmm.bfect.typeclasses.Fibre

package object io {
  type IOFibre[+E, +A] = Fibre[IO, E, A]
}
