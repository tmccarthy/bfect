package au.id.tmm.bfect.typeclasses

trait Fibre[F[+_, +_], +E, +A] {

  def cancel: F[Nothing, Unit]

  def join: F[E, A]

}
