package au.id.tmm.bfect.interop.cats.instanceimpls.eitherT

import au.id.tmm.bfect
import au.id.tmm.bfect._
import cats.data.EitherT

import scala.concurrent.CancellationException

class BracketInstance[F[_]] private[instanceimpls] (implicit F: cats.effect.Bracket[F, Throwable])
    extends DieInstance[F]
    with bfect.effects.Bracket.WithBMonad[EitherT[F, *, *]] {
  override def bracketCase[R, E, A](
    acquire: EitherT[F, E, R],
    release: (R, ExitCase[E, Unit]) => EitherT[F, Nothing, _],
    use: R => EitherT[F, E, A],
  ): EitherT[F, E, A] = EitherT {
    F.bracketCase[Either[E, R], Either[E, A]](
      acquire.value,
    )(
      use = {
        case Left(e)  => F.pure(Left(e))
        case Right(r) => use(r).value
      },
    )(
      release = {
        case (Left(e), _) => F.pure(Left(e))
        case (Right(r), catsExitCase) => {
          val bfectExitCase: ExitCase[E, Unit] = catsExitCase match {
            case cats.effect.ExitCase.Completed => ExitCase.Succeeded(())
            case cats.effect.ExitCase.Error(e)  => ExitCase.Failed(Failure.Unchecked(e))
            case cats.effect.ExitCase.Canceled  => ExitCase.Failed(Failure.Unchecked(new CancellationException))
          }

          F.as(release(r, bfectExitCase).value, ())
        }
      },
    )
  }
}
