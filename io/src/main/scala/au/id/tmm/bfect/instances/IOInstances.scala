package au.id.tmm.bfect.instances

trait FirstPriorityIOInstances extends SecondPriorityIOInstances {
  implicit val bmeInstance: BMEInstance = new BMEInstance()
}

trait SecondPriorityIOInstances extends ThirdPriorityIOInstances {
  implicit val biFunctorMonadInstance: BifunctorMonadInstance = new BifunctorMonadInstance()
}

trait ThirdPriorityIOInstances {
  implicit val biFunctorInstance: BifunctorInstance = new BifunctorInstance()
}
