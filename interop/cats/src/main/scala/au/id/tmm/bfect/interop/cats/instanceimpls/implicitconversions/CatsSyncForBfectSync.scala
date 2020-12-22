package au.id.tmm.bfect.interop.cats.instanceimpls.implicitconversions

import au.id.tmm.bfect.effects.{Bracket, Sync}
import au.id.tmm.bfect.{ExitCase, Failure}

class CatsSyncForBfectSync[F[_, _]](implicit bfectBracket: Bracket[F], bfectSync: Sync[F])
    extends CatsMonadErrorForBfectBME[F, Throwable]
    with cats.effect.Sync[F[Throwable, ?]] {
  override def suspend[A](thunk: => F[Throwable, A]): F[Throwable, A] = bfectSync.suspend(thunk)

  override def bracketCase[A, B](
    acquire: F[Throwable, A],
  )(
    use: A => F[Throwable, B],
  )(
    release: (A, cats.effect.ExitCase[Throwable]) => F[Throwable, Unit],
  ): F[Throwable, B] = {

    val releaseForBfectBracket: (A, ExitCase[Throwable, Unit]) => F[Nothing, _] = {
      case (resource, exitCase) =>
        val catsExitCase: cats.effect.ExitCase[Throwable] = exitCase match {
          case ExitCase.Succeeded(a)                 => cats.effect.ExitCase.Completed
          case ExitCase.Failed(Failure.Interrupted)  => cats.effect.ExitCase.Canceled
          case ExitCase.Failed(Failure.Checked(e))   => cats.effect.ExitCase.Error(e)
          case ExitCase.Failed(Failure.Unchecked(t)) => cats.effect.ExitCase.Error(t)
        }

        bfectSync.handleErrorWith[Throwable, Unit, Nothing](release(resource, catsExitCase))(t => throw t)
    }

    bfectBracket.bracketCase[A, Throwable, B](acquire, releaseForBfectBracket, use)
  }
}

object CatsSyncForBfectSync {
  trait ToCatsSync {
    implicit def bfectSyncIsCatsSync[F[_, _] : Sync : Bracket]: cats.effect.Sync[F[Throwable, ?]] =
      new CatsSyncForBfectSync[F]()
  }
}
