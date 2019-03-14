name := """GITB"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

scalaVersion := "2.11.12"

libraryDependencies ++= Seq(
  "eu.europa.ec.itb" % "gitb-types" % "1.5.1-SNAPSHOT",
  "com.gitb" % "gitb-core" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-lib" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-reports" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-validator-tdl" % "1.0-SNAPSHOT",
  "mysql" % "mysql-connector-java" % "5.1.47",
  "com.typesafe.akka" %% "akka-actor" % "2.5.19",
  "com.typesafe.akka" %% "akka-remote" % "2.5.19",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.19",
  "com.typesafe.play" %% "play-slick" % "2.1.1",
  "org.slf4j" % "slf4j-nop" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.apache.commons" % "commons-lang3" % "3.8.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.8",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.9.8",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.9.8",
  "com.fasterxml.jackson.module" % "jackson-module-jaxb-annotations" % "2.9.8",
  "org.mindrot"  % "jbcrypt" % "0.4",  // For password encryption
  "net.debasishg" %% "redisclient" % "3.9", // although this is the best one, maybe it could be changed into play-redis-plugin when sedis compiled for scala 2.11
  "org.apache.cxf" % "cxf-rt-frontend-jaxws" % "3.3.0",     //for calling jax-ws services
  "org.apache.cxf" % "cxf-rt-transports-http" % "3.3.0", //for calling jax-ws services
  "org.apache.cxf" % "cxf-rt-transports-http-jetty" % "3.3.0", //exporting jax-ws services
  "org.apache.tika" % "tika-core" % "1.20",
  "org.webjars" %% "webjars-play" % "2.5.0",
  "org.webjars" % "jquery" % "2.2.4",
  "org.webjars" % "jquery-cookie" % "1.4.1-1",
  "org.webjars" % "lodash" % "2.4.1-6",
  "org.webjars" % "bootstrap" % "3.2.0-2" exclude("org.webjars", "jquery"),
  "org.webjars" % "angularjs" % "1.7.6" exclude("org.webjars", "jquery"),
  "org.webjars" % "angular-ui-bootstrap" % "2.5.0",
  "org.webjars" % "angular-ui-router" % "1.0.20",
  "org.webjars" % "font-awesome" % "4.1.0" exclude("org.webjars", "jquery"),
  "org.webjars" % "angular-file-upload" % "1.6.12",
  "org.webjars" % "codemirror" % "4.8",
  "org.webjars" % "tinymce" % "4.7.9",
  "javax.mail" % "mail" % "1.4.7",
  "javax.activation" % "activation" % "1.1.1",
  "org.bouncycastle" % "bcmail-jdk15on" % "1.60",
  "org.apache.pdfbox" % "pdfbox" % "2.0.13",
  "org.jasypt" % "jasypt" % "1.9.2",
  "org.apache.httpcomponents" % "httpclient" % "4.5.7",
  "org.flywaydb" %% "flyway-play" % "3.2.0"
)

//JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

mainClass in assembly := Some("play.core.server.NettyServer")

fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

// Exclude commons-logging because it conflicts with the jcl-over-slf4j
libraryDependencies ~= { _ map {
	case m if m.organization == "com.typesafe.play" =>
		m.exclude("commons-logging", "commons-logging")
	case m => m
}}

// Take the first ServerWithStop because it's packaged into two jars
assemblyMergeStrategy in assembly := {
	case "play/core/server/ServerWithStop.class" => MergeStrategy.first
	case other => (assemblyMergeStrategy in assembly).value(other)
}

resolvers += Resolver.mavenLocal

routesImport += "extensions.Binders._"