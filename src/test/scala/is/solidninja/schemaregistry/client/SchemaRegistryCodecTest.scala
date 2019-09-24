package is
package solidninja
package schemaregistry
package client

import io.circe.literal._
import org.apache.avro.{Schema => AvroSchema}
import org.scalatest.{FreeSpec, Matchers}

class SchemaRegistryCodecTest extends FreeSpec with Matchers {
  import SchemaRegistryCodec._

  // TODO increase test coverage

  "A Schema should be parsed from JSON" in {
    val schemaJs = json"""{
      "schema": "{\"type\": \"string\"}"
    }"""

    val expected = Schema(AvroSchema.create(AvroSchema.Type.STRING))

    schemaJs.as[Schema] should equal(Right(expected))
  }

}
