package is.solidninja.schemaregistry.client

import org.apache.avro.SchemaBuilder
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class SchemaRegistryErrorTest extends AnyFreeSpec with Matchers {

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
