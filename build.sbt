name := "auto-posting"

version := "0.1"

scalaVersion := "2.13.1"

val izumiVersion = "0.10.0"
val circeVersion = "0.12.2"
val http4sVersion = "0.21.0-M6"

val misc = Seq(
  "org.typelevel"   %%  "simulacrum"        % "1.0.0",
  "com.propensive"  %%  "magnolia"          % "0.12.5",
  "com.chuusai"     %%  "shapeless"         % "2.3.3",
  "ru.tinkoff"      %%  "tofu"              % "0.6.3",
  "com.vk.api"      %   "sdk"               % "1.0.6",
  "org.rocksdb"     %   "rocksdbjni"        % "6.5.3",

  "org.scalatest"   %% "scalatest"          % "3.1.0"     % Test,
)

val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-generic-extras",
).map(_ % circeVersion)

val http4s = Seq(
  "org.http4s" %% "http4s-dsl",
  "org.http4s" %% "http4s-blaze-server",
  "org.http4s" %% "http4s-blaze-client",
).map(_ % http4sVersion)

val izumi = Seq(
  "io.7mind.izumi"  %%  "distage-extension-config",
  "io.7mind.izumi"  %%  "distage-extension-plugins",
  "io.7mind.izumi"  %%  "distage-framework",
  "io.7mind.izumi"  %%  "distage-core",
  "io.7mind.izumi"  %%  "logstage-adapter-slf4j",
  "io.7mind.izumi"  %%  "logstage-core",
).map(_ % izumiVersion)

libraryDependencies ++= misc ++ circe ++ http4s ++ izumi

scalacOptions += "-Ymacro-annotations"

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)