package au.id.tmm

import au.id.tmm.bifunctorio.typeclasses.Fibre

package object bifunctorio {

  type IOFibre[+E, +A] = Fibre[IO, E, A]

}
