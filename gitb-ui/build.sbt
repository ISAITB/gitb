import sbtlicensereport.license.{LicenseCategory, LicenseInfo}

scalaVersion := "2.13.14"
val pekkoVersion = "1.1.3"
val jacksonVersion = "2.18.2"
val cxfVersion = "4.1.0"
val gitbTypesVersion = "1.26.0-SNAPSHOT"
val bouncyCastleVersion = "1.80"
val commonsTextVersion = "1.13.0"

name := """GITB"""
version := gitbTypesVersion
maintainer := "DIGIT-ITB@ec.europa.eu"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

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
  "com.gitb" % "gitb-core" % gitbTypesVersion exclude("eu.europa.ec.itb", "gitb-types-jakarta") exclude("eu.europa.ec.itb", "gitb-types-specs"),
  "com.gitb" % "gitb-lib" % gitbTypesVersion exclude("eu.europa.ec.itb", "gitb-types-jakarta") exclude("eu.europa.ec.itb", "gitb-types-specs"),
  "com.gitb" % "gitb-reports" % gitbTypesVersion exclude("eu.europa.ec.itb", "gitb-types-jakarta") exclude("eu.europa.ec.itb", "gitb-types-specs"),
  "com.gitb" % "gitb-validator-tdl" % gitbTypesVersion exclude("eu.europa.ec.itb", "gitb-types-jakarta") exclude("eu.europa.ec.itb", "gitb-types-specs"),
  "com.gitb" % "gitb-xml-resources" % gitbTypesVersion exclude("eu.europa.ec.itb", "gitb-types-jakarta") exclude("eu.europa.ec.itb", "gitb-types-specs"),
  "com.mysql" % "mysql-connector-j" % "9.2.0" exclude("com.google.protobuf", "protobuf-java"), // Exclude protobuf as we don't need the X DevAPI.
  "org.apache.pekko" %% "pekko-actor" % pekkoVersion,
  "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
  "org.apache.pekko" %% "pekko-remote" % pekkoVersion,
  "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
  "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
  "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
  "org.playframework" %% "play-slick" % "6.1.1",
  "org.pac4j" %% "play-pac4j" % "12.0.2-PLAY3.0",
  "org.pac4j" % "pac4j-cas" % "6.1.1" exclude("org.bouncycastle", "bcpkix-jdk15on"),
  "org.apache.commons" % "commons-lang3" % "3.17.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.module" % "jackson-module-jakarta-xmlbind-annotations" % jacksonVersion,
  "com.password4j"  % "password4j" % "1.8.2",
  "net.debasishg" %% "redisclient" % "3.42",
  // For calling and exporting JAX-WS services.
  "org.apache.cxf" % "cxf-rt-frontend-jaxws" % cxfVersion,
  "org.apache.cxf" % "cxf-rt-transports-http" % cxfVersion,
  "org.apache.cxf" % "cxf-rt-transports-http-jetty" % cxfVersion,
  // ---
  "org.apache.tika" % "tika-core" % "3.1.0",
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
  "org.flywaydb" % "flyway-mysql" % "11.3.1",
  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "20240325.1",
  "net.lingala.zip4j" % "zip4j" % "2.11.5",
  "org.apache.commons" % "commons-text" % commonsTextVersion
)

// Produce GITB.zip as the final artifact name
Universal / packageBin := {
  val originalFileName = (Universal / packageBin).value
  val (base, ext) = originalFileName.baseAndExt
  val newFileName = file(originalFileName.getParent) / ("gitb-ui." + ext)
  IO.move(originalFileName, newFileName)
  newFileName
}

// Deactivate repeatable builds to speed up via parallelization
ThisBuild / assemblyRepeatableBuild := false

// Add assets build folder to clean task
cleanFiles += baseDirectory.value / "app" / "assets" / "build"

// Exclude sources and documentation
Compile / doc / sources := Seq.empty
Compile / packageDoc / publishArtifact := false

resolvers += Resolver.mavenLocal

routesGenerator := InjectedRoutesGenerator

/*
 * Licence reporting - START
 */
licenseCheckAllow := Seq(
  LicenseCategory.Apache,
  LicenseCategory.BouncyCastle,
  LicenseCategory.BSD,
  LicenseCategory.CC0,
  LicenseCategory.EPL,
  LicenseCategory.MIT,
  LicenseCategory.Mozilla,
  LicenseCategory.PublicDomain,
  LicenseCategory.JSON,
  LicenseCategory.Unicode,
  LicenseCategory.IBM_IPLA,
  LicenseCategory.LGPL,
  LicenseCategory.GPLClasspath
)
licenseOverrides := {
  case DepModuleInfo("jakarta.annotation", "jakarta.annotation-api", _) => LicenseInfo(LicenseCategory.GPLClasspath, LicenseCategory.GPLClasspath.name, "")
  case DepModuleInfo("jakarta.servlet", "jakarta.servlet-api", _) => LicenseInfo(LicenseCategory.GPLClasspath, LicenseCategory.GPLClasspath.name, "")
  case DepModuleInfo("commons-collections", "commons-collections", "3.2.2") => LicenseInfo(LicenseCategory.Apache, LicenseCategory.Apache.name, "")
  case DepModuleInfo("commons-digester", "commons-digester", "2.1") => LicenseInfo(LicenseCategory.Apache, LicenseCategory.Apache.name, "")
  case DepModuleInfo("org.apache.commons", "commons-exec", "1.3") => LicenseInfo(LicenseCategory.Apache, LicenseCategory.Apache.name, "")
  case DepModuleInfo("org.apereo.cas.client", "cas-client-core", "4.0.4") => LicenseInfo(LicenseCategory.Apache, LicenseCategory.Apache.name, "")
  case DepModuleInfo("org.apereo.cas.client", "cas-client-support-saml", "4.0.4") => LicenseInfo(LicenseCategory.Apache, LicenseCategory.Apache.name, "")
  case DepModuleInfo("org.hamcrest", "hamcrest-core", "1.3") => LicenseInfo(LicenseCategory.BSD, LicenseCategory.BSD.name, "")
  case DepModuleInfo("xml-resolver", "xml-resolver", "1.2") => LicenseInfo(LicenseCategory.Apache, LicenseCategory.Apache.name, "")
  case DepModuleInfo("com.google.code.findbugs", "findbugs-annotations", "3.0.1") => LicenseInfo(LicenseCategory.LGPL, LicenseCategory.LGPL.name, "")
}
licenseDepExclusions := {
  case DepModuleInfo("com.gitb", _, _) => true
  case DepModuleInfo("eu.europa.ec.itb", _, _) => true
  case DepModuleInfo("org.hamcrest", _, _) => true
  case DepModuleInfo("org.scala-sbt", "test-interface", _) => true
  case DepModuleInfo("org.jline", "jline", _) => true
  case DepModuleInfo("com.github.sbt", "junit-interface", _) => true
  case DepModuleInfo("commons-io", "commons-io", "2.17.0") => true // This is evicted but appears in the licence report
}
licenseCheckExclusions := {
  case DepModuleInfo("com.mysql", "mysql-connector-j", "9.2.0") => true
  case DepModuleInfo("wsdl4j", "wsdl4j", "1.6.3") => true
}
licenseReportNotes := {
  case DepModuleInfo("com.mysql", "mysql-connector-j", "9.2.0") => "The Universal FOSS Exception allows its usage as it is used unchanged."
  case DepModuleInfo("wsdl4j", "wsdl4j", "1.6.3") => "Used transitively by CXF, see (https://www.apache.org/legal/resolved.html#category-b)."
}
licenseConfigurations := Set("compile", "provided")
licenseReportTitle := "THIRD_PARTY_LICENCES"
/*
 * Licence reporting - END
 */

/*
 * Dependency checking is disabled given that the sbt-dependency-check is not updated for the NVP 9+ API.
 * To run the dependency check the simplest and fastest approach is to do a sbt dist and then extract the
 * libraries from the produced archive and pass them to the ODC CLI. The settings to include for a ODC run
 * with the sbt-dependency-check would be as follows.
 *
 *   .settings(dependencyCheckOSSIndexWarnOnlyOnRemoteErrors := Some(true))
 *   .settings(dependencyCheckFailBuildOnCVSS := 0)
 *   .settings(dependencyCheckSuppressionFile := Some(file("project/owasp-suppressions.xml")))
 */
