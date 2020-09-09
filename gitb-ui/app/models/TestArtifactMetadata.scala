package models

class TestArtifactMetadata(
    var shortName: String,
    var fullName: String,
    var description: Option[String],
    var documentation: Option[String]
) {
  var testCases: Map[String, TestArtifactMetadata] = _
}
