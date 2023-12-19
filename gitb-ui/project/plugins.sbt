resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
   url("https://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
       Resolver.ivyStylePatterns)

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.0")

// web plugins

addSbtPlugin("com.github.sbt" % "sbt-web" % "1.5.3")

addSbtPlugin("com.github.sbt" % "sbt-js-engine" % "1.3.5")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.5")

// Run with "sbt dependencyTree"
addDependencyTreePlugin

// Run manually with "sbt dependencyCheck".
addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "5.1.0")