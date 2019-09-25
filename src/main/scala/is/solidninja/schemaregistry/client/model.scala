package is
package solidninja
package schemaregistry
package client

import cats.Show
import cats.instances.int._
import cats.instances.option._
import cats.instances.string._
import cats.syntax.show._
import com.softwaremill.sttp.StatusCode
import org.apache.avro.{Schema => AvroSchema}

final case class Schema(schema: AvroSchema)

final case class VersionedSchema(schema: AvroSchema, subject: String, version: Int)

// Not using enumeratum to avoid extra dependency
object SchemaCompatibilityLevel extends Enumeration {
  type SchemaCompatibilityLevel = Value
  val BACKWARD, BACKWARD_TRANSITIVE, FORWARD, FORWARD_TRANSITIVE, FULL, FULL_TRANSITIVE, NONE = Value
}

sealed abstract class SchemaRegistryError extends RuntimeException

object SchemaRegistryError {

  implicit val showForSchemaRegistryError: Show[SchemaRegistryError] = Show.show {
    case IncompatibleAvroSchema              => "Avro schema is incompatible with previous version"
    case InvalidAvroSchema                   => "Avro schema is not valid"
    case InvalidSchemaVersion                => "Avro schema version is not valid"
    case SchemaDeserializationError(message) => show"Schema deserialization failed with message: $message"
    case SchemaNotFound(Left(id))            => show"Schema with id=$id not found"
    case SchemaNotFound(Right((subject, versionOpt))) =>
      show"Schema with subject=$subject, version=${versionOpt} not found"
    case UnknownError(status, message) => show"Unknown error code=$status, message=$message}"
  }
}

case object IncompatibleAvroSchema extends SchemaRegistryError

case object InvalidAvroSchema extends SchemaRegistryError
case object InvalidSchemaVersion extends SchemaRegistryError

final case class SchemaNotFound(id: Either[Int, (String, Option[Int])]) extends SchemaRegistryError
final case class SchemaDeserializationError(message: String) extends SchemaRegistryError
final case class UnknownError(httpStatus: StatusCode, message: String) extends SchemaRegistryError
