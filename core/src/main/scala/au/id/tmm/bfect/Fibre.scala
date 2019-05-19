package au.id.tmm.bfect

trait Fibre[F[+_, +_], +E, +A] {

  def cancel: F[Nothing, Unit]

  def join: F[E, A]

}
