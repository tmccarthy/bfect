package au.id.tmm.bfect.effects

import java.io.IOException
import java.nio.charset.Charset

import au.id.tmm.bfect.typeclasses.effects.Sync
import org.apache.commons.io.IOUtils

trait Resources[F[+_, +_]] {
  def resourceAsString(resourceName: String, charset: Charset = Charset.forName("UTF-8")): F[IOException, Option[String]]
}

object Resources {

  def apply[F[+_, +_] : Resources]: Resources[F] = implicitly[Resources[F]]

  trait SyncInstance {
    implicit def resourcesSyncInstance[F[+_, +_] : Sync]: Resources[F] = new Resources[F] {
      override def resourceAsString(resourceName: String, charset: Charset): F[IOException, Option[String]] = {
        val syncInstance = Sync[F]

        import syncInstance._

        bracketCloseable(sync(Option(getClass.getResourceAsStream(resourceName)))) { maybeInputStream =>
          syncCatch {
            maybeInputStream.map(IOUtils.toString(_, charset))
          } {
            case ioException: IOException => ioException
          }
        }

      }
    }
  }

  object SyncInstance extends SyncInstance

}
