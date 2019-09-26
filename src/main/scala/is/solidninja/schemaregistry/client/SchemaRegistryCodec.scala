package is
package solidninja
package schemaregistry
package client

import scala.util.Try

import io.circe._
import io.circe.generic.semiauto._
import io.circe.generic.extras.semiauto.deriveUnwrappedCodec
import org.apache.avro.{Schema => AvroSchema}

final case class SchemaIdContainer private[client] (id: Int)
final case class SchemaCompatibilityContainer private[client] (is_compatible: Boolean)
final case class SchemaCompatibility private[client] (compatibility: SchemaCompatibilityLevel.SchemaCompatibilityLevel)

private[schemaregistry] trait SchemaRegistryCodec {

  // Avro types
  implicit val avroSchemaEncoder: Encoder[AvroSchema] = Encoder[String].contramap(_.toString())
  implicit val avroSchemaDecoder: Decoder[AvroSchema] = Decoder[String].emapTry(
    s =>
      Try {
        val parser = new AvroSchema.Parser()
        parser.parse(s)
      }
  )

  // Model types
  implicit val schemaIdCodec: Codec[SchemaId] = deriveUnwrappedCodec
  implicit val schemaCodec: Codec[Schema] = deriveCodec
  implicit val schemaCompatibilityLevelCodec: Codec[SchemaCompatibilityLevel.SchemaCompatibilityLevel] =
    Codec.codecForEnumeration(SchemaCompatibilityLevel)
  implicit val schemaCompatibilityCodec: Codec[SchemaCompatibility] = deriveCodec
  implicit val schemaWithVersionCodec: Codec[VersionedSchema] = deriveCodec

  implicit val schemaCompatibilityContainer: Codec[SchemaCompatibilityContainer] = deriveCodec
  implicit val schemaIdContainerCodec: Codec[SchemaIdContainer] = deriveCodec

}

private[schemaregistry] object SchemaRegistryCodec extends SchemaRegistryCodec
