resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
   url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
       Resolver.ivyStylePatterns)

addSbtPlugin("com.github.play2war" % "play2-war-plugin" % "1.4.0")

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.11")

// web plugins

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.0.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-webdriver" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.4")

//addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.0.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.12.0")
