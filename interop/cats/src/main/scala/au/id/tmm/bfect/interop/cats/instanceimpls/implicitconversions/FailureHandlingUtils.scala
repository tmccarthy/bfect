package au.id.tmm.bfect.interop.cats.instanceimpls.implicitconversions

import au.id.tmm.bfect.effects.Sync

private[implicitconversions] object FailureHandlingUtils {

  def makeFailureUnchecked[F[_, _], E, A](fea: F[E, A])(implicit syncInstance: Sync[F]): F[Nothing, A] =
    syncInstance.handleErrorWith[E, A, Nothing](fea) {
      case t: Throwable => syncInstance.sync(throw t)
      case e            => syncInstance.sync(throw FailureInCancellationToken(e))
    }

  final case class FailureInCancellationToken[E](e: E) extends Exception

}
