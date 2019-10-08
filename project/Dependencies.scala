import sbt._

object Dependencies {

  object Versions {
    val avro = "1.9.1"
    val circe = "0.12.1"
    val sttp = "1.6.8"
    val scalatest = "3.0.8"
  }

  val avro = Seq(
    "org.apache.avro" % "avro" % Versions.avro % Provided
  )

  val circe = Seq(
    "io.circe" %% "circe-generic" % Versions.circe,
    "io.circe" %% "circe-generic-extras" % Versions.circe,
    "io.circe" %% "circe-jawn" % Versions.circe,
    "io.circe" %% "circe-literal" % Versions.circe
  )

  val scalatest = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalatest % "it,test"
  )

  val sttp = Seq(
    "com.softwaremill.sttp" %% "core" % Versions.sttp,
    "com.softwaremill.sttp" %% "circe" % Versions.sttp
  )

  val testSttp = Seq(
    "com.softwaremill.sttp" %% "async-http-client-backend-cats" % Versions.sttp % "it,test"
  )

  val runtimeLogging = Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime"
  )
}
