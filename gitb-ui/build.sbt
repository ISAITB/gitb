name := """GITB"""
version := "1.0-SNAPSHOT"
maintainer := "DIGIT-ITB@ec.europa.eu"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)
  .settings(dependencyCheckOSSIndexWarnOnlyOnRemoteErrors := Some(true))
  .settings(dependencyCheckFailBuildOnCVSS := 0)
  .settings(dependencyCheckSuppressionFile := Some(file("project/owasp-suppressions.xml")))

scalaVersion := "2.13.12"
val akkaVersion = "2.6.21" // Keep to the 2.6.* version for the Apache 2.0 Licence (also, this needs to match the version in Play).
val jacksonVersion = "2.15.3"
val cxfVersion = "4.0.3"
val commonsTextVersion = "1.11.0"
val gitbTypesVersion = "1.22.0-SNAPSHOT"
val jettyVersion = "11.0.18"
val bouncyCastleVersion = "1.77"

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
  "com.gitb" % "gitb-core" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-lib" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-reports" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-validator-tdl" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-xml-resources" % "1.0-SNAPSHOT",
  "com.mysql" % "mysql-connector-j" % "8.2.0" exclude("com.google.protobuf", "protobuf-java"), // Exclude protobuf as we don't need the X DevAPI.
  // Setting the Jetty version explicitly to v11.0.18 to resolve CVE-2023-44487. Once
  // org.apache.cxf:cxf-rt-transports-http-jetty is updated to a non-vulnerable version of Jetty this block can be removed. START ...
  "org.eclipse.jetty" % "jetty-http" % jettyVersion,
  "org.eclipse.jetty" % "jetty-io" % jettyVersion,
  "org.eclipse.jetty" % "jetty-security" % jettyVersion,
  "org.eclipse.jetty" % "jetty-server" % jettyVersion,
  "org.eclipse.jetty" % "jetty-util" % jettyVersion,
  // ... END.
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "com.typesafe.play" %% "play-slick" % "5.2.0",
  "org.pac4j" %% "play-pac4j" % "12.0.0-PLAY2.9",
  "org.pac4j" % "pac4j-cas" % "6.0.0",
  "org.apache.commons" % "commons-lang3" % "3.14.0",
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
  "org.apache.tika" % "tika-core" % "2.9.1",
  "org.webjars" %% "webjars-play" % "2.9.0",
  "org.webjars" % "jquery" % "3.7.1",
  "org.webjars" % "bootstrap" % "5.3.2",
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
  "org.bouncycastle" % "bcmail-jdk18on" % bouncyCastleVersion,
  "org.bouncycastle" % "bcpkix-jdk18on" % bouncyCastleVersion,
  "org.apache.pdfbox" % "pdfbox" % "2.0.30",
  "org.jasypt" % "jasypt" % "1.9.3",
  "org.apache.httpcomponents" % "httpclient" % "4.5.14",
  "org.flywaydb" %% "flyway-play" % "8.0.1",
  "org.flywaydb" % "flyway-mysql" % "9.16.0",
  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "20220608.1",
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