name := """GITB"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

scalaVersion := "2.11.12"
val akkaVersion = "2.5.31"

libraryDependencies ++= Seq(
  guice,
  ehcache,
  cacheApi,
  "eu.europa.ec.itb" % "gitb-types" % "1.9.1",
  "com.gitb" % "gitb-core" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-lib" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-reports" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-validator-tdl" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-xml-resources" % "1.0-SNAPSHOT",
  "mysql" % "mysql-connector-java" % "5.1.49",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.play" %% "play-slick" % "3.0.4",
  "com.typesafe.play" %% "play-json" % "2.6.14",
  "org.pac4j" %% "play-pac4j" % "6.1.0",
  "org.pac4j" % "pac4j-cas" % "3.2.0",
  "org.slf4j" % "slf4j-nop" % "1.7.30",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.apache.commons" % "commons-lang3" % "3.11",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.1",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.11.1",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.11.1",
  "com.fasterxml.jackson.module" % "jackson-module-jaxb-annotations" % "2.11.1",
  "org.mindrot"  % "jbcrypt" % "0.4",  // For password encryption
  "net.debasishg" %% "redisclient" % "3.30", // although this is the best one, maybe it could be changed into play-redis-plugin when sedis compiled for scala 2.11
  "org.apache.cxf" % "cxf-rt-frontend-jaxws" % "3.3.7",     //for calling jax-ws services
  "org.apache.cxf" % "cxf-rt-transports-http" % "3.3.7", //for calling jax-ws services
  "org.apache.cxf" % "cxf-rt-transports-http-jetty" % "3.3.7", //exporting jax-ws services
  "org.apache.tika" % "tika-core" % "1.24.1",
  "org.webjars" %% "webjars-play" % "2.6.3",
  "org.webjars" % "jquery" % "3.5.1",
  "org.webjars" % "jquery-cookie" % "1.4.1-1" exclude("org.webjars", "jquery"),
  "org.webjars" % "lodash" % "2.4.1-6",
  "org.webjars" % "bootstrap" % "3.3.7-1" exclude("org.webjars", "jquery"),
  "org.webjars" % "angularjs" % "1.7.9" exclude("org.webjars", "jquery"),
  "org.webjars" % "angular-ui-bootstrap" % "2.5.0",
  "org.webjars" % "angular-ui-router" % "1.0.20",
  "org.webjars" % "font-awesome" % "4.7.0" excludeAll(
      ExclusionRule(organization="org.webjars", name="jquery"),
      ExclusionRule(organization="org.webjars", name="bootstrap")
  ),
  "org.webjars" % "angular-file-upload" % "1.6.12",
  "org.webjars" % "codemirror" % "4.8",
  "org.webjars" % "tinymce" % "4.7.9",
  "javax.mail" % "mail" % "1.4.7",
  "javax.activation" % "activation" % "1.1.1",
  "javax.xml.ws" % "jaxws-api" % "2.3.1",
  "javax.jws" % "javax.jws-api" % "1.1",
  "javax.xml.bind" % "jaxb-api" % "2.3.1",
  "org.glassfish.jaxb" % "jaxb-runtime" % "2.3.3",
  "javax.xml.soap" % "javax.xml.soap-api" % "1.4.0",
  "com.sun.xml.messaging.saaj" % "saaj-impl" % "1.5.2",
  "com.sun.org.apache.xml.internal" % "resolver" % "20050927",
  "com.sun.xml.stream.buffer" % "streambuffer" % "1.5.9",
  "com.sun.xml.ws" % "policy" % "2.7.10",
  "org.glassfish.gmbal" % "gmbal-api-only" % "4.0.2",
  "org.bouncycastle" % "bcmail-jdk15on" % "1.66",
  "org.apache.pdfbox" % "pdfbox" % "2.0.20",
  "org.jasypt" % "jasypt" % "1.9.3",
  "org.apache.httpcomponents" % "httpclient" % "4.5.12",
  "org.flywaydb" %% "flyway-play" % "5.2.0",
  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "20200713.1",
  "net.lingala.zip4j" % "zip4j" % "2.6.1"
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

routesGenerator := InjectedRoutesGenerator
// routesImport += "extensions.Binders._"