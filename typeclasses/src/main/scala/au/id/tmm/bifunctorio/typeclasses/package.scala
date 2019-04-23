package au.id.tmm.bifunctorio

package object typeclasses {

  type BME[F[+_, +_]] = BiFunctorMonadError[F]
  val BME: BiFunctorMonadError.type = BiFunctorMonadError

}
