name := "cards"

version := "0.1"

scalaVersion := "2.13.6"

idePackagePrefix := Some("com.evolution")

scalacOptions ++= Seq(
  "-Xfatal-warnings"
)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic"     % "1.2.3",
  "io.circe"      %% "circe-generic"       % "0.14.1",
  "io.circe"      %% "circe-parser"        % "0.14.1",
  "org.typelevel" %% "cats-effect"         % "3.2.1",
  "co.fs2"        %% "fs2-core"            % "3.0.6",
  "org.http4s"    %% "http4s-dsl"          % "0.23.0",
  "org.http4s"    %% "http4s-blaze-server" % "0.23.0",
  "org.http4s"    %% "http4s-circe"        % "0.23.0"
)
