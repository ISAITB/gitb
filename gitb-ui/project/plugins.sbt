resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
   url("https://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
       Resolver.ivyStylePatterns)

// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.9")

// web plugins

addSbtPlugin("com.github.sbt" % "sbt-web" % "1.5.8")

addSbtPlugin("com.github.sbt" % "sbt-js-engine" % "1.3.9")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.2.0")

// Run with "sbt dependencyTree"
addDependencyTreePlugin

// Run manually with "sbt dependencyCheck".
// This is not currently used as it doesn't support the latest NVD API.
// The alternative is to generate a distribution JAR, extract it and then use the ODC via CLI to validate the library JARs.
// addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "5.1.0")

// Run "sbt dumpLicenseReport" or "sbt licenseCheck".
addSbtPlugin("com.github.sbt" % "sbt-license-report" % "1.7.0")