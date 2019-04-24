package au.id.tmm.bifunctorio.instances

trait FirstPriorityIOInstances extends SecondPriorityIOInstances {
  implicit val bmeInstance: BMEInstance = new BMEInstance()
}

trait SecondPriorityIOInstances extends ThirdPriorityIOInstances {
  implicit val biFunctorMonadInstance: BiFunctorMonadInstance = new BiFunctorMonadInstance()
}

trait ThirdPriorityIOInstances {
  implicit val biFunctorInstance: BiFunctorInstance = new BiFunctorInstance()
}
