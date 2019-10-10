package is.solidninja.schemaregistry.client

import org.scalatest.{FreeSpec, Matchers}
import org.apache.avro.SchemaBuilder

class SchemaRegistryErrorTest extends FreeSpec with Matchers {

  val sampleSchema = SchemaBuilder
    .record("Test")
    .fields()
    .nullableInt("id", -1)
    .endRecord()

  "A SchemaNotFoundError" - {
    "should have a message" in {
      SchemaNotFound("name").getMessage should equal(
        "Schema with subject=name, version=latest, fingerprint=? not found"
      )
      SchemaNotFound("name", Some(5)).getMessage should equal(
        "Schema with subject=name, version=5, fingerprint=? not found"
      )
      SchemaNotFound("name", sampleSchema).getMessage should equal(
        "Schema with subject=name, version=latest, fingerprint=9126331578744814407 not found"
      )
      SchemaNotFound(SchemaId(42)).getMessage should equal("Schema with id=42 not found")
    }
  }

}
