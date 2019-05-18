package au.id.tmm

import au.id.tmm.bfect.typeclasses.Fibre

package object bfect {

  type IOFibre[+E, +A] = Fibre[IO, E, A]

}
