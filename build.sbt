import Dependencies._
import build._

Global / onChangedBuildSource := ReloadOnSourceChanges

// Enable pinentry
Global / useGpgPinentry := true

lazy val root = Project(
  id = "sttp-client",
  base = file(".")
).configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    commonSettings,
    publishSettings,
    Seq(
      libraryDependencies ++= avro ++ circe ++ sttp ++ scalatest ++ testSttp ++ runtimeLogging
    )
  )
