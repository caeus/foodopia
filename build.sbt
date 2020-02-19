val Http4sVersion     = "0.21.1"
val CirceVersion      = "0.13.0"
val Specs2Version     = "4.8.3"
val LogbackVersion    = "1.2.3"
val SttpVersion       = "2.0.0-RC6"
val TapirVersion      = "0.12.21"
val PureconfigVersion = "0.12.1"

lazy val root = (project in file("."))
  .settings(
    organization := "com.github.caeus",
    name := "foodopia",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "org.mindrot"                  % "jbcrypt"                        % "0.4",
      "com.github.pureconfig"        %% "pureconfig"                    % PureconfigVersion,
      "org.reactormonk"              %% "cryptobits"                    % "1.3",
      "com.softwaremill.sttp.tapir"  %% "tapir-core"                    % "0.12.21",
      "dev.zio"                      %% "zio"                           % "1.0.0-RC17",
      "dev.zio"                      %% "zio-interop-cats"              % "2.0.0.0-RC10",
      "com.softwaremill.sttp.tapir"  %% "tapir-core"                    % TapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-json-circe"              % TapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-http4s-server"           % TapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-openapi-docs"            % TapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-openapi-circe-yaml"      % TapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-swagger-ui-http4s"       % TapirVersion,
      "com.softwaremill.sttp.client" %% "circe"                         % SttpVersion,
      "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % SttpVersion,
      "org.http4s"                   %% "http4s-blaze-server"           % Http4sVersion,
      "org.http4s"                   %% "http4s-blaze-client"           % Http4sVersion,
      "org.http4s"                   %% "http4s-circe"                  % Http4sVersion,
      "org.http4s"                   %% "http4s-dsl"                    % Http4sVersion,
      "io.circe"                     %% "circe-generic"                 % CirceVersion,
      "org.specs2"                   %% "specs2-core"                   % Specs2Version % "test",
      "ch.qos.logback"               % "logback-classic"                % LogbackVersion
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0")
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings"
)
