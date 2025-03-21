name := """GITB"""
version := "1.0-SNAPSHOT"
maintainer := "DIGIT-ITB@ec.europa.eu"

/*
Dependency checking is disabled given that the sbt-dependency-check is not updated for the NVP 9+ API.
To run the dependency check the simplest and fastest approach is to do a sbt dist and then extract the
libraries from the produced archive and pass them to the ODC CLI. The settings to include for a ODC run
with the sbt-dependency-check would be as follows.

  .settings(dependencyCheckOSSIndexWarnOnlyOnRemoteErrors := Some(true))
  .settings(dependencyCheckFailBuildOnCVSS := 0)
  .settings(dependencyCheckSuppressionFile := Some(file("project/owasp-suppressions.xml")))
*/

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

scalaVersion := "2.13.14"
val pekkoVersion = "1.0.3"
val jacksonVersion = "2.16.2"
val cxfVersion = "4.1.0"
val commonsTextVersion = "1.12.0"
val gitbTypesVersion = "1.25.2"
val bouncyCastleVersion = "1.78.1"

useCoursier := false

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

libraryDependencies ++= Seq(
  guice,
  ehcache,
  cacheApi,
  ws,
  "eu.europa.ec.itb" % "gitb-types-jakarta" % gitbTypesVersion,
  "eu.europa.ec.itb" % "gitb-types-specs" % gitbTypesVersion,
  "com.gitb" % "gitb-core" % "1.0-SNAPSHOT" exclude("eu.europa.ec.itb", "gitb-types-jakarta") exclude("eu.europa.ec.itb", "gitb-types-specs"),
  "com.gitb" % "gitb-lib" % "1.0-SNAPSHOT" exclude("eu.europa.ec.itb", "gitb-types-jakarta") exclude("eu.europa.ec.itb", "gitb-types-specs"),
  "com.gitb" % "gitb-reports" % "1.0-SNAPSHOT" exclude("eu.europa.ec.itb", "gitb-types-jakarta") exclude("eu.europa.ec.itb", "gitb-types-specs"),
  "com.gitb" % "gitb-validator-tdl" % "1.0-SNAPSHOT" exclude("eu.europa.ec.itb", "gitb-types-jakarta") exclude("eu.europa.ec.itb", "gitb-types-specs"),
  "com.gitb" % "gitb-xml-resources" % "1.0-SNAPSHOT" exclude("eu.europa.ec.itb", "gitb-types-jakarta") exclude("eu.europa.ec.itb", "gitb-types-specs"),
  "com.mysql" % "mysql-connector-j" % "8.4.0" exclude("com.google.protobuf", "protobuf-java"), // Exclude protobuf as we don't need the X DevAPI.
  "org.apache.pekko" %% "pekko-actor" % pekkoVersion,
  "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
  "org.apache.pekko" %% "pekko-remote" % pekkoVersion,
  "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
  "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
  "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
  "org.playframework" %% "play-slick" % "6.1.1",
  "org.pac4j" %% "play-pac4j" % "12.0.0-PLAY3.0",
  "org.pac4j" % "pac4j-cas" % "6.0.2" exclude("org.bouncycastle", "bcpkix-jdk15on"), // Needs to stay at 6.0.2 to match Play Jackson version (at 2.16.2)
  "org.apache.commons" % "commons-lang3" % "3.17.0",
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
  "org.apache.tika" % "tika-core" % "2.9.2",
  "org.webjars" % "jquery" % "3.7.1",
  "org.webjars" % "bootstrap" % "5.3.3",
  "com.sun.mail" % "jakarta.mail" % "2.0.1",
  "jakarta.activation" % "jakarta.activation-api" % "2.1.3",
  "jakarta.xml.ws" % "jakarta.xml.ws-api" % "4.0.2",
  "jakarta.jws" % "jakarta.jws-api" % "3.0.0",
  "jakarta.xml.bind" % "jakarta.xml.bind-api" % "4.0.2",
  "com.sun.xml.bind" % "jaxb-impl" % "4.0.5",
  "jakarta.xml.soap" % "jakarta.xml.soap-api" % "3.0.2",
  "com.sun.xml.messaging.saaj" % "saaj-impl" % "3.0.4", // Needed for SOAP exchanges
  "org.bouncycastle" % "bcmail-jdk18on" % bouncyCastleVersion,
  "org.bouncycastle" % "bcpkix-jdk18on" % bouncyCastleVersion,
  "org.apache.pdfbox" % "pdfbox" % "2.0.31",
  "org.jasypt" % "jasypt" % "1.9.3",
  "org.apache.httpcomponents" % "httpclient" % "4.5.14",
  "org.flywaydb" %% "flyway-play" % "9.1.0",
  "org.flywaydb" % "flyway-mysql" % "10.15.2",
  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "20240325.1",
  "net.lingala.zip4j" % "zip4j" % "2.11.5",
  // Specific version overrides (to be removed if no longer needed)
  "org.apache.commons" % "commons-text" % commonsTextVersion, // Set explicitly to resolve CVE-2022-42889
)

// Deactivate repeatable builds to speed up via parallelization
ThisBuild / assemblyRepeatableBuild := false

// Add assets build folder to clean task
cleanFiles += baseDirectory.value / "app" / "assets" / "build"

// Exclude sources and documentation
Compile / doc / sources := Seq.empty
Compile / packageDoc / publishArtifact := false

resolvers += Resolver.mavenLocal

routesGenerator := InjectedRoutesGenerator
