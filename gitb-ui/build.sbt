name := """GITB"""
version := "1.0-SNAPSHOT"
maintainer := "DIGIT-ITB@ec.europa.eu"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

scalaVersion := "2.12.12"
val akkaVersion = "2.6.8"
val jacksonVersion = "2.10.5"
val cxfVersion = "3.4.0"

useCoursier := false

libraryDependencies ++= Seq(
  guice,
  ehcache,
  cacheApi,
  "eu.europa.ec.itb" % "gitb-types" % "1.11.0",
  "com.gitb" % "gitb-core" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-lib" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-reports" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-validator-tdl" % "1.0-SNAPSHOT",
  "com.gitb" % "gitb-xml-resources" % "1.0-SNAPSHOT",
  "mysql" % "mysql-connector-java" % "5.1.49",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "com.typesafe.play" %% "play-slick" % "5.0.0",
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "org.pac4j" %% "play-pac4j" % "10.0.1",
  "org.pac4j" % "pac4j-cas" % "4.0.3",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.apache.commons" % "commons-lang3" % "3.11",
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.module" % "jackson-module-jaxb-annotations" % jacksonVersion,
  "org.mindrot"  % "jbcrypt" % "0.4",  // For password encryption
  "net.debasishg" %% "redisclient" % "3.30", // although this is the best one, maybe it could be changed into play-redis-plugin when sedis compiled for scala 2.11
  "org.apache.cxf" % "cxf-rt-frontend-jaxws" % cxfVersion,     //for calling jax-ws services
  "org.apache.cxf" % "cxf-rt-transports-http" % cxfVersion, //for calling jax-ws services
  "org.apache.cxf" % "cxf-rt-transports-http-jetty" % cxfVersion, //exporting jax-ws services
  "org.apache.tika" % "tika-core" % "1.24.1",
  "org.webjars" %% "webjars-play" % "2.8.0-1",
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
  "org.flywaydb" %% "flyway-play" % "6.0.0",
  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "20200713.1",
  "net.lingala.zip4j" % "zip4j" % "2.6.1"
)

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

// Exclude sources and documentation
sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

resolvers += Resolver.mavenLocal

routesGenerator := InjectedRoutesGenerator