This directory contains JAR files obtained as part of the [Xerces binary distribution](https://xerces.apache.org/mirrors.cgi#binary)
that supports XML Schema 1.1 validation. The Xerces distribution is not published on Maven Central (or another repository)
whereas the other JARs included in this folder are either not available, or not available at the specifically required 
versions.

To avoid incompatibilities for Xerces these libraries are installed manually in the local Maven repository as part of the
current project's `validate` phase.