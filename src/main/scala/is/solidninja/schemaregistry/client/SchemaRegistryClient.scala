package is
package solidninja
package schemaregistry
package client

import cats.Functor
import cats.syntax.functor._
import com.softwaremill.sttp.circe._
import com.softwaremill.sttp.{SttpBackend, Response => SttpResponse, _}
import is.solidninja.schemaregistry.client.SchemaCompatibilityLevel.SchemaCompatibilityLevel
import is.solidninja.schemaregistry.client.SchemaRegistryClient.{ResponseF, SchemaVersion, SubjectName}

/**
  * Client for the Confluent Schema Registry
  * @see https://docs.confluent.io/current/schema-registry/index.html
  */
trait SchemaRegistryClient[F[_]] {

  /**
    * Get schema compatibility level (globally, or if subject is passed, for that specific subject)
    */
  def compatibilityLevel(subject: Option[SubjectName] = None): ResponseF[F, SchemaCompatibilityLevel]

  /**
    * Remove all schema versions identified by the given subject
    */
  def delete(subject: SubjectName): ResponseF[F, List[SchemaVersion]]

  /**
    * Remove a specific schema version identified by the given subject
    */
  def deleteVersion(subject: SubjectName, version: Option[SchemaVersion]): ResponseF[F, Int]

  /**
    * Check whether a specific schema version exists and if it does, return the version of the schema
    */
  def exists(subject: SubjectName, schema: Schema): ResponseF[F, Option[VersionedSchema]]

  /**
    * Get a specific schema identified by its unique id
    */
  def get(id: SchemaId): ResponseF[F, Schema]

  /**
    * Get a version of a schema for the given subject and (optionally) a version
    */
  def get(subject: SubjectName, version: Option[SchemaVersion]): ResponseF[F, VersionedSchema]

  /**
    * Check whether the passed schema is compatible with the one currently registered for the subject (optionally
    * specifying the version to check compatibility against)
    */
  def isCompatible(subject: SubjectName, schema: Schema, version: Option[SchemaVersion] = None): ResponseF[F, Boolean]

  /**
    * List the versions of a schema for the given subject
    */
  def listVersions(subject: SubjectName): ResponseF[F, List[SchemaVersion]]

  /**
    * List all of the subjects registered
    */
  def listSchemas(): ResponseF[F, List[SubjectName]]

  /**
    * Register a new schema version under the given subject
    */
  def register(subject: SubjectName, schema: Schema): ResponseF[F, SchemaId]

  /**
    * Update the global (or subject-specific) schema compatibility level
    */
  def updateCompatibilityLevel(
      level: SchemaCompatibilityLevel,
      subject: Option[SubjectName] = None
  ): ResponseF[F, SchemaCompatibilityLevel]

}

object SchemaRegistryClient {

  type Response[T] = Either[SchemaRegistryError, T]
  type ResponseF[F[_], T] = F[Response[T]]

  type SchemaVersion = Int
  type SubjectName = String

  import SchemaRegistryCodec._

  def apply[F[_]: Functor](host: String)(implicit backend: SttpBackend[F, Nothing]): SchemaRegistryClient[F] =
    new SchemaRegistryClient[F] {

      override def compatibilityLevel(subject: Option[SubjectName]): ResponseF[F, SchemaCompatibilityLevel] =
        sttp
          .get(subject.fold(uri"$host/config")(s => uri"$host/config/$s"))
          .response(asJson[SchemaCompatibility])
          .send()
          .map(convertResponse)
          .map(_.map(_.compatibility))

      override def delete(subject: SubjectName): ResponseF[F, List[SchemaVersion]] =
        sttp
          .delete(uri"$host/subjects/$subject")
          .response(asJson[List[SchemaVersion]])
          .send()
          .map { r =>
            r.code match {
              case StatusCodes.NotFound => Left(SchemaNotFound(Right((subject, None))))
              case _                    => convertResponse(r)
            }
          }

      override def deleteVersion(subject: SubjectName, version: Option[SchemaVersion]): ResponseF[F, SchemaVersion] =
        sttp
          .delete(uri"$host/subjects/$subject/versions/${versionOrLatest(version)}")
          .response(asJson[Int])
          .send()
          .map(handleSubjectVersionNotFound(subject, version))

      override def exists(subject: SubjectName, schema: Schema): ResponseF[F, Option[VersionedSchema]] =
        sttp
          .post(uri"$host/subjects/$subject")
          .body(schema)
          .response(asJson[VersionedSchema])
          .send()
          .map { r =>
            r.code match {
              case StatusCodes.NotFound => Right(None)
              case _                    => convertResponse(r).map(Option(_))
            }
          }

      override def get(id: SchemaId): ResponseF[F, Schema] =
        sttp
          .get(uri"$host/schemas/ids/${id.id}")
          .response(asJson[Schema])
          .send()
          .map { r =>
            r.code match {
              case StatusCodes.NotFound => Left(SchemaNotFound(Left(id)))
              case _                    => convertResponse(r)
            }
          }

      override def get(subject: SubjectName, version: Option[SchemaVersion]): ResponseF[F, VersionedSchema] =
        sttp
          .get(uri"$host/subjects/$subject/versions/${versionOrLatest(version)}")
          .response(asJson[VersionedSchema])
          .send()
          .map(handleSubjectVersionNotFound(subject, version))

      override def isCompatible(
          subject: SubjectName,
          schema: Schema,
          version: Option[SchemaVersion]
      ): ResponseF[F, Boolean] =
        sttp
          .post(uri"$host/compatibility/subjects/$subject/versions/${versionOrLatest(version)}")
          .response(asJson[SchemaCompatibilityContainer])
          .send()
          .map(handleSubjectVersionNotFound(subject, version))
          .map(_.map(_.is_compatible))

      override def listSchemas(): ResponseF[F, List[SubjectName]] =
        sttp
          .get(uri"$host/subjects")
          .response(asJson[List[SubjectName]])
          .send()
          .map(convertResponse)

      override def listVersions(subject: SubjectName): ResponseF[F, List[SchemaVersion]] =
        sttp
          .get(uri"$host/subjects/$subject/versions")
          .response(asJson[List[SchemaVersion]])
          .send()
          .map(convertResponse)

      override def register(subject: SubjectName, schema: Schema): ResponseF[F, SchemaId] =
        sttp
          .post(uri"$host/subjects/$subject/versions")
          .body(schema)
          .response(asJson[SchemaIdContainer])
          .send()
          .map { r =>
            r.code match {
              case StatusCodes.Conflict            => Left(IncompatibleAvroSchema)
              case StatusCodes.UnprocessableEntity => Left(InvalidAvroSchema)
              case _                               => convertResponse(r)
            }
          }
          .map(_.map(idc => SchemaId(idc.id)))

      override def updateCompatibilityLevel(
          level: SchemaCompatibilityLevel,
          subject: Option[SubjectName]
      ): ResponseF[F, SchemaCompatibilityLevel] =
        sttp
          .post(subject.fold(uri"$host/config")(s => uri"$host/config/$s"))
          .body(SchemaCompatibility(level))
          .response(asJson[SchemaCompatibility])
          .send()
          .map(convertResponse)
          .map(_.map(_.compatibility))

    }

  private def handleSubjectVersionNotFound[A](subject: SubjectName, version: Option[SchemaVersion])(
      r: SttpResponse[Either[DeserializationError[io.circe.Error], A]]
  ): Either[SchemaRegistryError, A] =
    r.code match {
      case StatusCodes.NotFound            => Left(SchemaNotFound(Right((subject, version))))
      case StatusCodes.UnprocessableEntity => Left(InvalidSchemaVersion)
      case _                               => convertResponse(r)
    }

  private def convertResponse[A](
      r: SttpResponse[Either[DeserializationError[io.circe.Error], A]]
  ): Either[SchemaRegistryError, A] =
    r.body.fold(
      errorText => Left(UnknownError(r.code, errorText)),
      body => convertJsonError(body)
    )

  private def convertJsonError[A](e: Either[DeserializationError[io.circe.Error], A]): Either[SchemaRegistryError, A] =
    e.fold(
      error => Left(SchemaDeserializationError(error.message)),
      a => Right(a)
    )

  private def versionOrLatest(version: Option[SchemaVersion]): String = version.map(_.toString).getOrElse("latest")

}
