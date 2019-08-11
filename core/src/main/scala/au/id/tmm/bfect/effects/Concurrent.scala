/**
  *    Copyright 2019 Timothy McCarthy
  *
  *    Licensed under the Apache License, Version 2.0 (the "License");
  *    you may not use this file except in compliance with the License.
  *    You may obtain a copy of the License at
  *
  *        http://www.apache.org/licenses/LICENSE-2.0
  *
  *    Unless required by applicable law or agreed to in writing, software
  *    distributed under the License is distributed on an "AS IS" BASIS,
  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *    See the License for the specific language governing permissions and
  *    limitations under the License.
  */
package au.id.tmm.bfect.effects

import au.id.tmm.bfect._

trait Concurrent[F[+_, +_]] {

  def start[E, A](fea: F[E, A]): F[Nothing, Fibre[F, E, A]]

  def fork[E, A](fea: F[E, A]): F[Nothing, Fibre[F, E, A]] = start(fea)

  def racePair[E, A, B](fea: F[E, A], feb: F[E, B]): F[E, Either[(A, Fibre[F, E, B]), (Fibre[F, E, A], B)]]

  def race[E, A, B](fea: F[E, A], feb: F[E, B]): F[E, Either[A, B]]

  def par[E, A, B](fea: F[E, A], feb: F[E, B]): F[E, (A, B)]

  def cancelable[E, A](k: (Either[E, A] => Unit) => F[Nothing, _]): F[E, A]

}

object Concurrent extends ConcurrentStaticOps with ConcurrentParNs {

  def apply[F[+_, +_] : Concurrent]: Concurrent[F] = implicitly[Concurrent[F]]

  trait WithBMonad[F[+_, +_]] extends Concurrent[F] { self: BMonad[F] =>
    override def race[E, A, B](fea: F[E, A], feb: F[E, B]): F[E, Either[A, B]] =
      /*_*/
      flatMap(racePair(fea, feb)) {
        case Left((a, fibreForB))  => map(fibreForB.cancel)(_ => Left(a))
        case Right((fibreForA, b)) => map(fibreForA.cancel)(_ => Right(b))
      }
    /*_*/

    override def par[E, A, B](fea: F[E, A], feb: F[E, B]): F[E, (A, B)] =
      flatMap(racePair(fea, feb)) {
        case Left((a, bFibre))  => map(bFibre.join)(b => (a, b))
        case Right((aFibre, b)) => map(aFibre.join)(a => (a, b))
      }
  }

  implicit class Ops[F[+_, +_], E, A](protected val fea: F[E, A])(implicit concurrent: Concurrent[F]) {
    def start: F[Nothing, Fibre[F, E, A]] = concurrent.start(fea)
    def fork: F[Nothing, Fibre[F, E, A]]  = concurrent.fork(fea)

    def race[B](feb: F[E, B]): F[E, Either[A, B]] = concurrent.race(fea, feb)
  }

}

trait ConcurrentStaticOps {
  def race[F[+_, +_] : Concurrent, E, A, B](fea: F[E, A], feb: F[E, B]): F[E, Either[A, B]] =
    Concurrent[F].race(fea, feb)
}

trait ConcurrentParNs {
  def par10[F[+_, +_] : Concurrent : BFunctor, E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](
    fetchTally1: F[E, T1],
    fetchTally2: F[E, T2],
    fetchTally3: F[E, T3],
    fetchTally4: F[E, T4],
    fetchTally5: F[E, T5],
    fetchTally6: F[E, T6],
    fetchTally7: F[E, T7],
    fetchTally8: F[E, T8],
    fetchTally9: F[E, T9],
    fetchTally10: F[E, T10],
  ): F[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] = {
    val concurrentInstance = Concurrent[F]

    BFunctor[F].map(
      concurrentInstance.par(
        fetchTally1,
        concurrentInstance.par(
          fetchTally2,
          concurrentInstance.par(
            fetchTally3,
            concurrentInstance.par(
              fetchTally4,
              concurrentInstance.par(
                fetchTally5,
                concurrentInstance.par(
                  fetchTally6,
                  concurrentInstance.par(
                    fetchTally7,
                    concurrentInstance.par(fetchTally8, concurrentInstance.par(fetchTally9, fetchTally10)))),
              ),
            ),
          ),
        ),
      )) {
      case (tally1, (tally2, (tally3, (tally4, (tally5, (tally6, (tally7, (tally8, (tally9, tally10))))))))) =>
        (
          tally1,
          tally2,
          tally3,
          tally4,
          tally5,
          tally6,
          tally7,
          tally8,
          tally9,
          tally10,
        )
    }
  }

  def par9[F[+_, +_] : Concurrent : BFunctor, E, T1, T2, T3, T4, T5, T6, T7, T8, T9](
    fetchTally1: F[E, T1],
    fetchTally2: F[E, T2],
    fetchTally3: F[E, T3],
    fetchTally4: F[E, T4],
    fetchTally5: F[E, T5],
    fetchTally6: F[E, T6],
    fetchTally7: F[E, T7],
    fetchTally8: F[E, T8],
    fetchTally9: F[E, T9],
  ): F[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9)] = {
    val concurrentInstance = Concurrent[F]

    BFunctor[F].map(
      concurrentInstance.par(
        fetchTally1,
        concurrentInstance.par(
          fetchTally2,
          concurrentInstance.par(
            fetchTally3,
            concurrentInstance.par(
              fetchTally4,
              concurrentInstance.par(
                fetchTally5,
                concurrentInstance.par(
                  fetchTally6,
                  concurrentInstance.par(fetchTally7, concurrentInstance.par(fetchTally8, fetchTally9)))),
            ),
          ),
        ),
      )) {
      case (tally1, (tally2, (tally3, (tally4, (tally5, (tally6, (tally7, (tally8, tally9)))))))) =>
        (
          tally1,
          tally2,
          tally3,
          tally4,
          tally5,
          tally6,
          tally7,
          tally8,
          tally9,
        )
    }
  }

  def par8[F[+_, +_] : Concurrent : BFunctor, E, T1, T2, T3, T4, T5, T6, T7, T8](
    fetchTally1: F[E, T1],
    fetchTally2: F[E, T2],
    fetchTally3: F[E, T3],
    fetchTally4: F[E, T4],
    fetchTally5: F[E, T5],
    fetchTally6: F[E, T6],
    fetchTally7: F[E, T7],
    fetchTally8: F[E, T8],
  ): F[E, (T1, T2, T3, T4, T5, T6, T7, T8)] = {
    val concurrentInstance = Concurrent[F]

    BFunctor[F].map(
      concurrentInstance.par(
        fetchTally1,
        concurrentInstance.par(
          fetchTally2,
          concurrentInstance.par(
            fetchTally3,
            concurrentInstance.par(
              fetchTally4,
              concurrentInstance.par(
                fetchTally5,
                concurrentInstance.par(fetchTally6, concurrentInstance.par(fetchTally7, fetchTally8)))),
          ),
        ),
      )) {
      case (tally1, (tally2, (tally3, (tally4, (tally5, (tally6, (tally7, tally8))))))) =>
        (
          tally1,
          tally2,
          tally3,
          tally4,
          tally5,
          tally6,
          tally7,
          tally8,
        )
    }
  }

  def par7[F[+_, +_] : Concurrent : BFunctor, E, T1, T2, T3, T4, T5, T6, T7](
    fetchTally1: F[E, T1],
    fetchTally2: F[E, T2],
    fetchTally3: F[E, T3],
    fetchTally4: F[E, T4],
    fetchTally5: F[E, T5],
    fetchTally6: F[E, T6],
    fetchTally7: F[E, T7],
  ): F[E, (T1, T2, T3, T4, T5, T6, T7)] = {
    val concurrentInstance = Concurrent[F]

    BFunctor[F].map(
      concurrentInstance.par(
        fetchTally1,
        concurrentInstance.par(
          fetchTally2,
          concurrentInstance.par(
            fetchTally3,
            concurrentInstance
              .par(fetchTally4, concurrentInstance.par(fetchTally5, concurrentInstance.par(fetchTally6, fetchTally7)))),
        ),
      )) {
      case (tally1, (tally2, (tally3, (tally4, (tally5, (tally6, tally7)))))) =>
        (
          tally1,
          tally2,
          tally3,
          tally4,
          tally5,
          tally6,
          tally7,
        )
    }
  }

  def par6[F[+_, +_] : Concurrent : BFunctor, E, T1, T2, T3, T4, T5, T6](
    fetchTally1: F[E, T1],
    fetchTally2: F[E, T2],
    fetchTally3: F[E, T3],
    fetchTally4: F[E, T4],
    fetchTally5: F[E, T5],
    fetchTally6: F[E, T6],
  ): F[E, (T1, T2, T3, T4, T5, T6)] = {
    val concurrentInstance = Concurrent[F]

    BFunctor[F].map(
      concurrentInstance.par(
        fetchTally1,
        concurrentInstance.par(
          fetchTally2,
          concurrentInstance
            .par(fetchTally3, concurrentInstance.par(fetchTally4, concurrentInstance.par(fetchTally5, fetchTally6)))),
      )) {
      case (tally1, (tally2, (tally3, (tally4, (tally5, tally6))))) =>
        (
          tally1,
          tally2,
          tally3,
          tally4,
          tally5,
          tally6,
        )
    }
  }

  def par5[F[+_, +_] : Concurrent : BFunctor, E, T1, T2, T3, T4, T5](
    fetchTally1: F[E, T1],
    fetchTally2: F[E, T2],
    fetchTally3: F[E, T3],
    fetchTally4: F[E, T4],
    fetchTally5: F[E, T5],
  ): F[E, (T1, T2, T3, T4, T5)] = {
    val concurrentInstance = Concurrent[F]

    BFunctor[F].map(
      concurrentInstance.par(
        fetchTally1,
        concurrentInstance
          .par(fetchTally2, concurrentInstance.par(fetchTally3, concurrentInstance.par(fetchTally4, fetchTally5))))) {
      case (tally1, (tally2, (tally3, (tally4, tally5)))) =>
        (
          tally1,
          tally2,
          tally3,
          tally4,
          tally5,
        )
    }
  }

  def par4[F[+_, +_] : Concurrent : BFunctor, E, T1, T2, T3, T4](
    fetchTally1: F[E, T1],
    fetchTally2: F[E, T2],
    fetchTally3: F[E, T3],
    fetchTally4: F[E, T4],
  ): F[E, (T1, T2, T3, T4)] = {
    val concurrentInstance = Concurrent[F]

    BFunctor[F].map(
      concurrentInstance
        .par(fetchTally1, concurrentInstance.par(fetchTally2, concurrentInstance.par(fetchTally3, fetchTally4)))) {
      case (tally1, (tally2, (tally3, tally4))) =>
        (
          tally1,
          tally2,
          tally3,
          tally4,
        )
    }
  }

  def par3[F[+_, +_] : Concurrent : BFunctor, E, T1, T2, T3](
    fetchTally1: F[E, T1],
    fetchTally2: F[E, T2],
    fetchTally3: F[E, T3],
  ): F[E, (T1, T2, T3)] = {
    val concurrentInstance = Concurrent[F]

    BFunctor[F].map(concurrentInstance.par(fetchTally1, concurrentInstance.par(fetchTally2, fetchTally3))) {
      case (tally1, (tally2, tally3)) =>
        (
          tally1,
          tally2,
          tally3,
        )
    }
  }

  def par2[F[+_, +_] : Concurrent : BFunctor, E, T1, T2](
    fetchTally1: F[E, T1],
    fetchTally2: F[E, T2],
  ): F[E, (T1, T2)] = {
    val concurrentInstance = Concurrent[F]

    BFunctor[F].map(concurrentInstance.par(fetchTally1, fetchTally2)) {
      case (tally1, tally2) =>
        (
          tally1,
          tally2,
        )
    }
  }

}
