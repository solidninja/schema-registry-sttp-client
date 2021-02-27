import sbt._
import Keys._

object build {

  val manifestSetting = packageOptions += {
    val (title, v, vendor) = (name.value, version.value, organization.value)
    Package.ManifestAttributes(
      "Created-By" -> "Simple Build Tool",
      "Built-By" -> System.getProperty("user.name"),
      "Build-Jdk" -> System.getProperty("java.version"),
      "Specification-Title" -> title,
      "Specification-Version" -> v,
      "Specification-Vendor" -> vendor,
      "Implementation-Title" -> title,
      "Implementation-Version" -> v,
      "Implementation-Vendor-Id" -> vendor,
      "Implementation-Vendor" -> vendor
    )
  }

  val mavenCentralSettings = Seq(
    description := "API client for Confluent Schema Registry",
    homepage := Some(url("https://gitlab.com/solidninja/schema-registry-sttp-client")),
    startYear := Some(2019),
    licenses += "MIT" -> url("https://opensource.org/licenses/mit-license.php"),
    developers := List(
      Developer(
        id = "vladimir-lu",
        name = "Vladimir Lushnikov",
        email = "vladimir@solidninja.is",
        url = url("https://solidninja.is")
      )
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://gitlab.com/solidninja/schema-registry-sttp-client"),
        "scm:git:https://gitlab.com/solidninja/schema-registry-sttp-client.git",
        Some(s"scm:git:git@gitlab.com:solidninja/schema-registry-sttp-client.git")
      )
    )
  )

  val commonSettings = mavenCentralSettings ++ Seq(
    organization := "is.solidninja.schemaregistry",
    version := "0.1.4-SNAPSHOT",
    scalaVersion := "2.13.5",
    crossScalaVersions := Seq("2.12.10", "2.13.5"),
    javacOptions ++= Seq("-target", "1.8", "-source", "1.8"),
    manifestSetting,
    crossVersion := CrossVersion.binary
  )

  val publishSettings = Seq(
    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    ),
    publishMavenStyle := true,
    publishArtifact in Test := false
  )
}
