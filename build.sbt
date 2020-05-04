import sbt.Keys.scalacOptions

lazy val simpleShExScala = project
  .in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      "es.weso"           %% "shex"          % "0.1.59",
      "es.weso"           %% "srdf4j"        % "0.1.66",
      "org.scalatest"     %% "scalatest"     % "3.1.0" % Test,
      "org.eclipse.rdf4j" % "rdf4j-rio-trig" % "3.0.4" % Test
    ),
    scalacOptions ++= Seq(
      "-deprecation", // Emit warning and location for usages of deprecated APIs.,
      "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    ),
    resolvers ++= Seq(
      Resolver.bintrayRepo("labra", "maven")
    ),
    scalaVersion := "2.13.2",
    fork := true
  )
