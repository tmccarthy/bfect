package au.id.tmm.bifunctorio.typeclasses

trait Fibre[F[+_, +_], +E, +A] {

  def cancel: F[Nothing, Unit]

  def join: F[E, A]

}
