[![Build Status](https://travis-ci.com/tmccarthy/bfect.svg?branch=master)](https://travis-ci.com/tmccarthy/bfect)
[![Maven Central](https://img.shields.io/maven-central/v/au.id.tmm.bfect/bfect-core_2.12.svg)](https://repo.maven.apache.org/maven2/au/id/tmm/bfect/bfect-core_2.12/)

# Bfect

A collection of bifunctor effect typeclasses, with instances for ZIO and conversions for cats-effect.

## Project structure

* *`bfect-core`* - A collection of bifunctor effect typeclasses, based loosely around the structure of cats-effect
* *`bfect-testing`* - An implementation of a bifunctor [state monad](https://typelevel.org/cats/datatypes/state.html) along with instances for the `bfect-core` typeclasses
* *`bfect-interop-cats`* - Implicit conversions between the `bfect-core` typeclasses and their analogs in `cats-core` and `cats-effect`
* *`bfect-interop-zio`* - Instances of the `bfect-core` typeclasses for the [ZIO IO monad](https://github.com/zio/zio)
* *`bfect-io`* - A half-finished bifunctor IO monad (don't use this)

Each of these are available through [Maven Central](https://repo.maven.apache.org/maven2/au/id/tmm/bfect/), just add them to your project with your favourite build tool.

## Typeclasses

![](typeclass-hierarchy.svg)

Typeclass | Cats equivalent | Comment |
----------|-----------------|---------|
[`Bifunctor`](core/src/main/scala/au/id/tmm/bfect/Bifunctor.scala) | `cats.Bifunctor` | Functor with `biMap` and its derivations (`map`/`rightMap`, `leftMap`) |
[`BifunctorMonad`](core/src/main/scala/au/id/tmm/bfect/BifunctorMonad.scala) | `cats.Monad` | Monad. Adds `flatMap`, `rightPure` and `leftPure`. |
[`BifunctorMonadError`](core/src/main/scala/au/id/tmm/bfect/BifunctorMonadError.scala) | `cats.MonadError` | Represents the ability to handle errors with `handleErrorWith`. Comes with the alias `BME`. |
[`effects.Bracket`](core/src/main/scala/au/id/tmm/bfect/effects/Bracket.scala) | `cats.effect.Bracket` | Bracket. Represents the pure equivalent of `try`/`finally` |
[`effects.Now`](core/src/main/scala/au/id/tmm/bfect/effects/Now.scala) | `cats.effect.Timer` | Represents the ability to create a timestamp |
[`effects.Timer`](core/src/main/scala/au/id/tmm/bfect/effects/Timer.scala) | `cats.effect.Timer` | Extends `Now` with the ability to delay execution for a period of time |
[`effects.Sync`](core/src/main/scala/au/id/tmm/bfect/effects/Sync.scala) | `cats.effect.Sync` | Extends `Bracket` with the ability to represent synchronous effects |
[`effects.Async`](core/src/main/scala/au/id/tmm/bfect/effects/Async.scala) | `cats.effect.Async` | Extends `Sync` with the ability to register asynchronous effects |
[`effects.Concurrent`](core/src/main/scala/au/id/tmm/bfect/effects/Concurrent.scala) | `cats.effect.Concurrent` | Extends `Async` with the ability to start and cancel tasks |
[`effects.extra.Console`](core/src/main/scala/au/id/tmm/bfect/effects/extra/Console.scala) | | Represents the effect of writing to the console |
[`effects.extra.EnvVars`](core/src/main/scala/au/id/tmm/bfect/effects/extra/EnvVars.scala) | | Represents the effect of accessing environment variables |
[`effects.extra.Resources`](core/src/main/scala/au/id/tmm/bfect/effects/extra/Resources.scala) | | Represents the effect of accessing Java resources |
[`effects.extra.Calendar`](core/src/main/scala/au/id/tmm/bfect/effects/extra/Calendar.scala) | | Extends `Now` with the ability to determine the system timezone, enabling computation of the local date and so on. |


## Usage

Most typeclasses come with an `Ops` implicit class on the companion object. Generally if you are using
a typeclass you will import it and the `Ops` like so:

```scala
import au.id.tmm.bfect.effects.Sync
import au.id.tmm.bfect.effects.Sync.Ops
import au.id.tmm.bfect.ziointerop._

type BIO[+E, +A] = scalaz.zio.IO[E, A]

// Companion object provides static methods. The following are equivalent

Sync[BIO].pure("hello")
Sync.pure("hello")

// Sync.Ops provides instance methods. The following are equivalent

val io = Sync.pure("hello")

Sync[BIO].flatMap(io)(s => Sync.sync(println(s)))
io.flatMap(s => Sync.sync(println(s)))

// Importing Sync.Ops enables for-yield syntax:

for {
  s <- Sync.pure("hello")
  _ <- Sync.sync(println(s))
} yield ()
```
