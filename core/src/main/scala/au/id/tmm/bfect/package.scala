package au.id.tmm

package object bfect {

  type BME[F[+_, +_]] = BifunctorMonadError[F]
  val BME: BifunctorMonadError.type = BifunctorMonadError

}
