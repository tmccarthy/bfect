package au.id.tmm.bfect

package object io {
  type IOFibre[+E, +A] = Fibre[IO, E, A]
}
