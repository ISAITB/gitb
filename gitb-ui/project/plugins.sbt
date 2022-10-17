resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
   url("https://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
       Resolver.ivyStylePatterns)

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.17")

// web plugins

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.2.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.0.0-RC1")

addDependencyTreePlugin

// Run manually with "sbt dependencyCheck".
addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "4.1.0")