name := """GITB"""
version := "1.0-SNAPSHOT"
maintainer := "DIGIT-ITB@ec.europa.eu"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)
  .settings(dependencyCheckOSSIndexWarnOnlyOnRemoteErrors := Some(true))
  .settings(dependencyCheckFailBuildOnCVSS := 0)
  .settings(dependencyCheckSuppressionFile := Some(file("project/owasp-suppressions.xml")))

scalaVersion := "2.13.10"
val akkaVersion = "2.6.21" // Keep to the 2.6.* version for the Apache 2.0 Licence (also, this needs to match the version in Play).
val jacksonVersion = "2.15.2"
val cxfVersion = "4.0.3"
val guiceVersion = "5.1.0" // Keep the 5.1.0 version as for Play 2.8.19 we need to base injection on javax.injection annotations and not jakarta.injection annotations.
val commonsTextVersion = "1.10.0"
val jjwtVersion = "0.11.5"
val gitbTypesVersion = "1.21.0"

useCoursier := false

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

libraryDependencies ++= Seq(
  guice,
  ehcache,
  cacheApi,
  ws,
  "com.google.inject" % "guice" % guiceVersion,
  "com.google.inject.extensions" % "guice-assistedinject" % guiceVersion,
  "eu.europa.ec.itb" % "gitb-types-jakarta" % gitbTypesVersion,
  "com.gitb" % "gitb-core" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-lib" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-reports" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-validator-tdl" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-xml-resources" % "1.0-SNAPSHOT",
  "com.mysql" % "mysql-connector-j" % "8.0.33" exclude("com.google.protobuf", "protobuf-java"), // Exclude protobuf as we don't need the X DevAPI.
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "com.typesafe.play" %% "play-slick" % "5.1.0",
  "com.typesafe.play" %% "play-json" % "2.9.4",
  "org.pac4j" %% "play-pac4j" % "11.1.0-PLAY2.8",
  "org.pac4j" % "pac4j-cas-clientv4" % "5.7.1",
  "ch.qos.logback" % "logback-classic" % "1.4.7", // When upgrading to Play 2.9.0 this could be removed (Play 2.9.0 upgrades to 1.4.11).
  "org.apache.commons" % "commons-lang3" % "3.12.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.module" % "jackson-module-jakarta-xmlbind-annotations" % jacksonVersion,
  "org.mindrot"  % "jbcrypt" % "0.4",  // For password encryption
  "net.debasishg" %% "redisclient" % "3.42",
  // For calling and exporting JAX-WS services.
  "org.apache.cxf" % "cxf-rt-frontend-jaxws" % cxfVersion,
  "org.apache.cxf" % "cxf-rt-transports-http" % cxfVersion,
  "org.apache.cxf" % "cxf-rt-transports-http-jetty" % cxfVersion,
  // ---
  "org.apache.tika" % "tika-core" % "2.8.0",
  "org.webjars" %% "webjars-play" % "2.8.18",
  "org.webjars" % "jquery" % "3.6.4",
  "org.webjars" % "bootstrap" % "3.4.1" exclude("org.webjars", "jquery"),
  "com.sun.mail" % "jakarta.mail" % "2.0.1",
  "jakarta.activation" % "jakarta.activation-api" % "2.1.2",
  "jakarta.xml.ws" % "jakarta.xml.ws-api" % "4.0.0",
  "jakarta.jws" % "jakarta.jws-api" % "3.0.0",
  "jakarta.xml.bind" % "jakarta.xml.bind-api" % "4.0.0",
  "com.sun.xml.bind" % "jaxb-impl" % "4.0.2",
  "jakarta.xml.soap" % "jakarta.xml.soap-api" % "3.0.0",
  "com.sun.xml.messaging.saaj" % "saaj-impl" % "3.0.2",
  "com.sun.org.apache.xml.internal" % "resolver" % "20050927",
  "com.sun.xml.stream.buffer" % "streambuffer" % "2.1.0",
  "com.sun.xml.ws" % "policy" % "4.0.1",
  "org.glassfish.gmbal" % "gmbal-api-only" % "4.0.3",
  "org.bouncycastle" % "bcmail-jdk15on" % "1.70",
  "org.apache.pdfbox" % "pdfbox" % "2.0.28",
  "org.jasypt" % "jasypt" % "1.9.3",
  "org.apache.httpcomponents" % "httpclient" % "4.5.14",
  "org.flywaydb" %% "flyway-play" % "7.41.0",
  "org.flywaydb" % "flyway-mysql" % "9.16.0",
  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "20220608.1",
  "net.lingala.zip4j" % "zip4j" % "2.11.5",
  // Specific version overrides (to be removed if no longer needed)
  "org.apache.commons" % "commons-text" % commonsTextVersion, // Set explicitly to resolve CVE-2022-42889
  // Override JJWT that is built-in to Play framework. This is needed for Play 2.8.19 (it contains a hard dependency to old the JAXB API) but when upgrading to Play 2.9 this should be removed as the JJWT dependency is at the right version. START:
  "io.jsonwebtoken" % "jjwt-api" % jjwtVersion,
  "io.jsonwebtoken" % "jjwt-impl" % jjwtVersion,
  "io.jsonwebtoken" % "jjwt-jackson" % jjwtVersion
  // :END
)

// This exclusion is to be removed when we upgrade to Play 2.9 which will bring JJWT to the correct version.
libraryDependencies ~= { _ map {
  case m if m.organization == "com.typesafe.play" =>
    m.exclude("io.jsonwebtoken", "jjwt")
  case m => m
}}

// Deactivate repeatable builds to speed up via parallelization
ThisBuild / assemblyRepeatableBuild := false

// Add assets build folder to clean task
cleanFiles += baseDirectory.value / "app" / "assets" / "build"

// Exclude sources and documentation
Compile / doc / sources := Seq.empty
Compile / packageDoc / publishArtifact := false

resolvers += Resolver.mavenLocal

routesGenerator := InjectedRoutesGenerator