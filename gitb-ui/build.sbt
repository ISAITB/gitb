name := """GITB"""
version := "1.0-SNAPSHOT"
maintainer := "DIGIT-ITB@ec.europa.eu"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)
  .settings(dependencyCheckFailBuildOnCVSS := 0)
  .settings(dependencyCheckSuppressionFile := Some(file("project/owasp-suppressions.xml")))

scalaVersion := "2.13.6"
val akkaVersion = "2.6.18"
val jacksonVersion = "2.13.1"
val cxfVersion = "3.4.5"
val jettyVersion = "9.4.43.v20210629"

useCoursier := false

libraryDependencies ++= Seq(
  guice,
  ehcache,
  cacheApi,
  "com.google.inject" % "guice" % "5.0.1",
  "com.google.inject.extensions" % "guice-assistedinject" % "5.0.1",
  "eu.europa.ec.itb" % "gitb-types" % "1.15.1",
  "com.gitb" % "gitb-core" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-lib" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-reports" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-validator-tdl" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-xml-resources" % "1.0-SNAPSHOT",
  "mysql" % "mysql-connector-java" % "8.0.28",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "com.typesafe.play" %% "play-slick" % "5.0.0",
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "org.pac4j" %% "play-pac4j" % "11.1.0-PLAY2.8",
  "org.pac4j" % "pac4j-cas" % "5.3.0",
  "ch.qos.logback" % "logback-classic" % "1.2.10",
  "org.apache.commons" % "commons-lang3" % "3.12.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.module" % "jackson-module-jaxb-annotations" % jacksonVersion,
  "org.mindrot"  % "jbcrypt" % "0.4",  // For password encryption
  "net.debasishg" %% "redisclient" % "3.42",
  // For calling and exporting JAX-WS services.
  "org.apache.cxf" % "cxf-rt-frontend-jaxws" % cxfVersion,
  "org.apache.cxf" % "cxf-rt-transports-http" % cxfVersion,
  "org.apache.cxf" % "cxf-rt-transports-http-jetty" % cxfVersion,
  // ---
  // Jetty dependencies set explicitly to address CVEs. If CXF transitive dependencies increase the version to at least 9.4.43 these can be removed.
  "org.eclipse.jetty" % "jetty-continuation" % jettyVersion,
  "org.eclipse.jetty" % "jetty-http" % jettyVersion,
  "org.eclipse.jetty" % "jetty-io" % jettyVersion,
  "org.eclipse.jetty" % "jetty-security" % jettyVersion,
  "org.eclipse.jetty" % "jetty-server" % jettyVersion,
  "org.eclipse.jetty" % "jetty-util" % jettyVersion,
  // ---
  "org.apache.tika" % "tika-core" % "2.1.0",
  "org.webjars" %% "webjars-play" % "2.8.8",
  "org.webjars" % "jquery" % "3.5.1",
  "org.webjars" % "bootstrap" % "3.4.1" exclude("org.webjars", "jquery"),
  "javax.mail" % "mail" % "1.4.7",
  "javax.activation" % "activation" % "1.1.1",
  "javax.xml.ws" % "jaxws-api" % "2.3.1",
  "javax.jws" % "javax.jws-api" % "1.1",
  "javax.xml.bind" % "jaxb-api" % "2.3.1",
  "org.glassfish.jaxb" % "jaxb-runtime" % "2.3.3", // Must match the 2.3.* API
  "javax.xml.soap" % "javax.xml.soap-api" % "1.4.0",
  "com.sun.xml.messaging.saaj" % "saaj-impl" % "1.5.2", // Do not update version
  "com.sun.org.apache.xml.internal" % "resolver" % "20050927",
  "com.sun.xml.stream.buffer" % "streambuffer" % "1.5.9", // Do not update version
  "com.sun.xml.ws" % "policy" % "2.7.10",
  "org.glassfish.gmbal" % "gmbal-api-only" % "4.0.3",
  "org.bouncycastle" % "bcmail-jdk15on" % "1.69",
  "org.apache.pdfbox" % "pdfbox" % "2.0.24",
  "org.jasypt" % "jasypt" % "1.9.3",
  "org.apache.httpcomponents" % "httpclient" % "4.5.13",
  "org.flywaydb" %% "flyway-play" % "7.14.0",
  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "20211018.2",
  "net.lingala.zip4j" % "zip4j" % "2.9.1"
)

// Add assets build folder to clean task
cleanFiles += baseDirectory.value / "app" / "assets" / "build"

// Exclude sources and documentation
sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

resolvers += Resolver.mavenLocal

routesGenerator := InjectedRoutesGenerator