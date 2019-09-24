package is
package solidninja
package schemaregistry
package client

import scala.concurrent.ExecutionContext

import cats.data.EitherT
import cats.effect.{ContextShift, IO}
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import org.apache.avro.{Schema => AvroSchema}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, Inside, Matchers}


class SchemaRegistryClientIntegrationTest extends FreeSpec with Matchers with ScalaFutures with Inside {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(implicitly)

  implicit val sttpBackend: SttpBackend[IO, Nothing] = AsyncHttpClientCatsBackend[IO]()

  val schemaRegistryHost = sys.env.getOrElse("SCHEMA_REGISTRY_HOST", "localhost:38081")

  "A schema registry client should be able to publish a new schema and retrieve it" in {
    val client = SchemaRegistryClient[IO](schemaRegistryHost)
    val testSubject = "ittest-40a7b34e-2081-450d-8d10-83304a9bb2a3"

    val schema = Schema(new AvroSchema.Parser().parse(
      """{
        "type": "record",
        "name": "test",
        "fields": [
          {
            "type": "string",
            "name": "field1"
          },
          {
            "type": "int",
            "name": "field2"
          }
        ]
      }"""))

    val resF = for {
      id <- EitherT(client.register(testSubject, schema))
      got <- EitherT(client.get(id))
      _ <- EitherT(client.delete(testSubject))
    } yield (id, got)

    val res = resF.value.unsafeRunSync()

    inside(res) {
      case Right((schemaId, gotSchema)) =>
        schemaId should be >= 1
        gotSchema should equal (schema)
    }
  }
}
