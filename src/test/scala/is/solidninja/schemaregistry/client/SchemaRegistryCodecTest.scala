package is
package solidninja
package schemaregistry
package client

import io.circe.literal._
import io.circe.syntax._
import org.apache.avro.{Schema => AvroSchema}
import org.scalatest.{FreeSpec, Matchers}

class SchemaRegistryCodecTest extends FreeSpec with Matchers {
  import SchemaRegistryCodec._

  "An Avro schema should be parsed from JSON" in {
    val schemaJs = json"""{
      "schema": "{\"type\": \"string\"}"
    }"""

    val expected = Schema(AvroSchema.create(AvroSchema.Type.STRING))

    schemaJs.as[Schema] should equal(Right(expected))
  }

  "A SchemaIdContainer should be parsed from JSON" in {
    val js = json"""{"id": 1}"""

    js.as[SchemaIdContainer] should equal(Right(SchemaIdContainer(1)))
  }

  "A SchemaCompatibilityContainer should be parsed from JSON" in {
    val js = json"""{"is_compatible": true}"""
    js.as[SchemaCompatibilityContainer] should equal(Right(SchemaCompatibilityContainer(is_compatible = true)))
  }

  "A SchemaCompatibility should be parsed and serialized as JSON" in {
    val js = json"""{
      "compatibility": "FULL_TRANSITIVE"
    }"""

    js.as[SchemaCompatibility] should equal(Right(SchemaCompatibility(SchemaCompatibilityLevel.FULL_TRANSITIVE)))
    SchemaCompatibility(SchemaCompatibilityLevel.FULL_TRANSITIVE).asJson should equal(js)
  }

  "A VersionedSchema should be parsed from JSON" in {
    val js = json"""{
      "subject": "test",
      "id": 1,
      "version": 3,
      "schema": "{\"type\": \"record\", \"name\": \"test\", \"fields\": [{\"type\": \"string\",\"name\": \"field1\"}, {\"type\": \"int\", \"name\": \"field2\"}]}"
    }"""
    val expected = VersionedSchema(
      id = SchemaId(1),
      subject = "test",
      version = 3,
      schema = new AvroSchema.Parser().parse(
        """{"type": "record", "name": "test", "fields": [{"type": "string","name": "field1"}, {"type": "int", "name": "field2"}]}"""
      )
    )

    js.as[VersionedSchema] should equal(Right(expected))
  }
}
