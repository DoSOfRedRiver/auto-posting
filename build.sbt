name := "auto-posting"

version := "0.1"

scalaVersion := "2.13.1"

val izumiVersion = "0.10.7"
val circeVersion = "0.12.2"
val typedSchemaVersion = "0.12.5.1"
val swaggerUIVersion = "3.26.0"

val misc = Seq(
  "org.typelevel"       %%  "simulacrum"        % "1.0.0",
  "com.propensive"      %%  "magnolia"          % "0.12.5",
  "com.chuusai"         %%  "shapeless"         % "2.3.3",
  "ru.tinkoff"          %%  "tofu"              % "0.7.3",
  "ru.tinkoff"          %%  "tofu-optics-macro" % "0.7.3",
  "com.lihaoyi"         %%  "scalatags"         % "0.9.1",
  "org.manatki"         %%  "derevo-circe"      % "0.11.4",

  "com.vk.api"          %   "sdk"               % "1.0.6",
  "org.rocksdb"         %   "rocksdbjni"        % "6.5.3",
  "org.apache.commons"  %   "commons-lang3"     % "3.10",

  "org.webjars.npm"     %   "swagger-ui-dist"   % swaggerUIVersion,

  "org.scalatest"   %% "scalatest"          % "3.1.0"     % Test,
)

val typedSchema = List(
  "ru.tinkoff" %% "typed-schema-swagger",
  "ru.tinkoff" %% "typed-schema-finagle-env",
  "ru.tinkoff" %% "typed-schema-finagle-custom",
) map (_ % typedSchemaVersion)

val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-generic-extras",
).map(_ % circeVersion)

val izumi = Seq(
  "io.7mind.izumi"  %%  "distage-extension-config",
  "io.7mind.izumi"  %%  "distage-extension-plugins",
  "io.7mind.izumi"  %%  "distage-testkit-scalatest",
  "io.7mind.izumi"  %%  "distage-framework",
  "io.7mind.izumi"  %%  "distage-core",
  "io.7mind.izumi"  %%  "logstage-adapter-slf4j",
  "io.7mind.izumi"  %%  "logstage-core",
).map(_ % izumiVersion)

libraryDependencies ++= misc ++ circe ++ izumi ++ typedSchema

scalacOptions += "-Ymacro-annotations"

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](
  ("swaggerUIVersion", swaggerUIVersion)
)

mainClass := Some("dorr.Main")
