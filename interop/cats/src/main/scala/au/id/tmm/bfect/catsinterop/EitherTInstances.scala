package au.id.tmm.bfect.catsinterop

import au.id.tmm.bfect
import au.id.tmm.bfect._
import au.id.tmm.bfect.effects.Die
import cats.{Functor, Monad, MonadError}
import cats.data.EitherT

import scala.concurrent.CancellationException

trait EitherTInstances {

  class EitherTBifunctor[F[_] : Functor] extends Bifunctor[EitherT[F, *, *]] {
    override def biMap[L1, R1, L2, R2](f: EitherT[F, L1, R1])(leftF: L1 => L2, rightF: R1 => R2): EitherT[F, L2, R2] =
      f.bimap(leftF, rightF)
  }

  class EitherTBifunctorMonadError[F[_] : Monad]
      extends EitherTBifunctor[F]
      with BifunctorMonadError[EitherT[F, *, *]] {
    override def rightPure[E, A](a: A): EitherT[F, E, A] = EitherT.rightT(a)

    override def leftPure[E, A](e: E): EitherT[F, E, A] = EitherT.leftT(e)

    override def flatMap[E1, E2 >: E1, A, B](
      fe1a: EitherT[F, E1, A],
    )(
      fafe2b: A => EitherT[F, E2, B],
    ): EitherT[F, E2, B] =
      fe1a.flatMap[E2, B](fafe2b)

    override def tailRecM[E, A, A1](a: A)(f: A => EitherT[F, E, Either[A, A1]]): EitherT[F, E, A1] =
      Monad[EitherT[F, E, *]].tailRecM(a)(f)

    override def handleErrorWith[E1, A, E2](fea: EitherT[F, E1, A])(f: E1 => EitherT[F, E2, A]): EitherT[F, E2, A] =
      EitherT {
        Monad[F].flatMap(fea.value) {
          case Right(a) => Monad[F].pure(Right(a))
          case Left(e)  => f(e).value
        }
      }
  }

  class EitherTBfectDie[F[_]](implicit F: MonadError[F, Throwable])
      extends EitherTBifunctorMonadError[F]
      with Die[EitherT[F, *, *]] {
    override def failUnchecked(t: Throwable): EitherT[F, Nothing, Nothing] =
      EitherT.liftF[F, Nothing, Nothing](F.raiseError[Nothing](t))
  }

  class EitherTBfectBracket[F[_]](implicit F: cats.effect.Bracket[F, Throwable])
      extends EitherTBfectDie[F]
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

}
