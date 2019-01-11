import com.github.play2war.plugin._

name := """GITB"""

version := "1.0-SNAPSHOT"

Play2WarPlugin.play2WarSettings

Play2WarKeys.servletVersion := "3.1"

Play2WarKeys.targetName := Some("itbui")

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "eu.europa.ec.itb" % "gitb-types" % "1.5.0",
  "com.gitb" % "gitb-core" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-lib" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-reports" % "1.0-SNAPSHOT",
  "mysql" % "mysql-connector-java" % "5.1.35",
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "com.typesafe.akka" %% "akka-remote" % "2.3.4",
  "com.typesafe.play" %% "play-slick" % "1.1.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "ch.qos.logback" % "logback-classic" % "1.0.1",
  "commons-lang" % "commons-lang" % "2.6",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.3.3",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.3.1",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.3.0",
  "com.fasterxml.jackson.module" % "jackson-module-jaxb-annotations" % "2.3.0",
  "org.mindrot"  % "jbcrypt" % "0.3m",  // For password encryption
  "net.debasishg" %% "redisclient" % "3.0", // although this is the best one, maybe it could be changed into play-redis-plugin when sedis compiled for scala 2.11
  "org.apache.cxf" % "cxf-rt-frontend-jaxws" % "2.7.4",     //for calling jax-ws services
  "org.apache.cxf" % "cxf-rt-transports-http" % "2.7.4", //for calling jax-ws services
  "org.apache.cxf" % "cxf-rt-transports-http-jetty" % "2.7.4", //exporting jax-ws services
  "org.apache.tika" % "tika-core" % "1.19",
  "org.webjars" %% "webjars-play" % "2.4.0",
  "org.webjars" % "jquery" % "1.11.1",
  "org.webjars" % "jquery-cookie" % "1.4.0",
  "org.webjars" % "lodash" % "2.4.1-6",
  "org.webjars" % "bootstrap" % "3.2.0-2" exclude("org.webjars", "jquery"),
  "org.webjars" % "angularjs" % "1.2.16-2" exclude("org.webjars", "jquery"),
  "org.webjars" % "angular-ui-bootstrap" % "0.12.0",
  "org.webjars" % "angular-ui-router" % "0.2.18",
  "org.webjars" % "ng-grid" % "2.0.11-2",
  "org.webjars" % "font-awesome" % "4.1.0" exclude("org.webjars", "jquery"),
  "org.webjars" % "angular-file-upload" % "1.6.12",
  "org.webjars" % "codemirror" % "4.8",
  "org.webjars" % "tinymce" % "4.7.9",
  "javax.mail" % "mail" % "1.4.7",
  "javax.activation" % "activation" % "1.1.1",
  "org.bouncycastle" % "bcmail-jdk15on" % "1.60",
  "org.apache.pdfbox" % "pdfbox" % "2.0.9",
  "org.jasypt" % "jasypt" % "1.9.2",
  "commons-httpclient" % "commons-httpclient" % "3.1",
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