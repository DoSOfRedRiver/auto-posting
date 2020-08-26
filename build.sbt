name := "auto-posting"

version := "0.1"

scalaVersion := "2.13.1"

val tsecVersion = "0.2.1"
val izumiVersion = "0.10.7"
val circeVersion = "0.12.2"
val typedSchemaVersion = "0.12.5.1"

val misc = Seq(
  "org.typelevel"       %%  "simulacrum"        % "1.0.0",
  "com.propensive"      %%  "magnolia"          % "0.12.5",
  "com.chuusai"         %%  "shapeless"         % "2.3.3",
  "ru.tinkoff"          %%  "tofu"              % "0.7.3",
  "ru.tinkoff"          %%  "tofu-optics-macro" % "0.7.3",
  "org.manatki"         %%  "derevo-circe"      % "0.11.4",

  "com.vk.api"          %   "sdk"               % "1.0.6",
  "org.rocksdb"         %   "rocksdbjni"        % "6.5.3",
  "org.apache.commons"  %   "commons-lang3"     % "3.10",

  "org.scalatest"   %% "scalatest"          % "3.1.0"     % Test,
)

val typedSchema = List(
  "ru.tinkoff" %% "typed-schema-swagger",
  "ru.tinkoff" %% "typed-schema-swagger-ui",
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

val tsec = Seq(
  "io.github.jmcardon" %% "tsec-common" % tsecVersion,
  "io.github.jmcardon" %% "tsec-mac" % tsecVersion,
)

libraryDependencies ++= misc ++ circe ++ izumi ++ typedSchema ++ tsec

scalacOptions += "-Ymacro-annotations"

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")


lazy val runServer = taskKey[Unit]("A custom run task.")
fullRunTask(runServer, Runtime, "dorr.Main", Array("-u", ":publisher"): _*)
