package is
package solidninja
package schemaregistry
package client

import cats.Show
import cats.instances.int._
import cats.instances.string._
import cats.syntax.show._
import com.softwaremill.sttp.StatusCode
import org.apache.avro.{SchemaNormalization, Schema => AvroSchema}

final case class Schema(schema: AvroSchema)

/**
  * Unique identifier of the schema in the registry
  */
final case class SchemaId(id: Int) extends AnyVal

/**
  * Schema with subject and version
  */
final case class VersionedSchema(id: SchemaId, subject: String, version: Int, schema: AvroSchema)

// Not using enumeratum to avoid extra dependency
object SchemaCompatibilityLevel extends Enumeration {
  type SchemaCompatibilityLevel = Value
  val BACKWARD, BACKWARD_TRANSITIVE, FORWARD, FORWARD_TRANSITIVE, FULL, FULL_TRANSITIVE, NONE = Value
}

sealed abstract class SchemaRegistryError(message: String) extends RuntimeException(message)

object SchemaRegistryError {
  implicit val showForSchemaRegistryError: Show[SchemaRegistryError] = Show.show(_.getMessage)
}

case object IncompatibleAvroSchema extends SchemaRegistryError("Avro schema is incompatible with previous version")
case object InvalidAvroSchema extends SchemaRegistryError("Avro schema is not valid")
case object InvalidSchemaVersion extends SchemaRegistryError("Avro schema version is not valid")

final case class SchemaNotFound private[client] (
    message: String,
    id: Either[SchemaId, (String, Option[Int], Option[Long])]
) extends SchemaRegistryError(message) {
  def schemaId: Option[SchemaId] = id.left.toOption
}

object SchemaNotFound {
  def apply(id: Either[SchemaId, (String, Option[Int], Option[Long])]): SchemaNotFound = {
    val message = id match {
      case Left(SchemaId(id)) => show"Schema with id=$id not found"
      case Right((subject, versionOpt, fingerprintOpt)) =>
        show"Schema with subject=$subject, version=${versionOpt.fold("latest")(_.toString)}, fingerprint=${fingerprintOpt
          .fold("?")(_.toString)} not found"
    }
    new SchemaNotFound(message, id)
  }
  def apply(id: SchemaId): SchemaNotFound = apply(Left(id))
  def apply(name: String, lookupSchema: AvroSchema): SchemaNotFound =
    apply(name, version = None, Some(SchemaNormalization.parsingFingerprint64(lookupSchema)))
  def apply(name: String, version: Option[Int] = None, schemaFingerprint: Option[Long] = None): SchemaNotFound =
    apply(Right((name, version, schemaFingerprint)))
}

final case class SchemaDeserializationError(message: String)
    extends SchemaRegistryError(show"Schema deserialization failed with message: $message")
final case class UnknownError(httpStatus: StatusCode, message: String)
    extends SchemaRegistryError(show"Unknown error code=$httpStatus, message=$message}")
