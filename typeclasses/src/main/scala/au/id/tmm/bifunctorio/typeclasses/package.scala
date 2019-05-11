package au.id.tmm.bifunctorio

package object typeclasses {

  type BME[F[+_, +_]] = BifunctorMonadError[F]
  val BME: BifunctorMonadError.type = BifunctorMonadError

}
