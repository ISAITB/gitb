package models

/**
 * Created by serbay on 10/17/14.
 */
case class TestSuites(
	id: Long,
	shortname: String,
	fullname: String,
	version: String,
	authors: Option[String],
	originalDate: Option[String],
	modificationDate: Option[String],
	description: Option[String],
	keywords: Option[String],
	specification: Long,
	filename: String,
	hasDocumentation: Boolean,
	documentation: Option[String],
	identifier: String
)

class TestSuite(
								 _id: Long,
	               _shortname: String,
	               _fullname: String,
	               _version: String,
	               _authors: Option[List[String]],
	               _originalDate: Option[String],
	               _modificationDate: Option[String],
	               _description: Option[String],
	               _keywords: Option[List[String]],
	               _actors: Option[List[Actor]],
	               _testCases: Option[List[TestCases]],
								 _specification: Long,
							   _filename: String,
							   _hasDocumentation: Boolean,
							   _documentation: Option[String],
							 	 _identifier: String
	               ) {
	var id: Long = _id
	var shortname: String = _shortname
	var fullname: String = _fullname
	var version: String = _version
	var authors: Option[List[String]] = _authors
	var originalDate: Option[String] = _originalDate
	var modificationDate: Option[String] = _modificationDate
	var description: Option[String] = _description
	var keywords: Option[List[String]] = _keywords
	var actors: Option[List[Actor]] = _actors
	var testCases: Option[List[TestCases]] = _testCases
	var specification: Long = _specification
	var filename: String = _filename
	var hasDocumentation: Boolean = _hasDocumentation
	var documentation: Option[String] = _documentation
	var identifier: String = _identifier

	def this(testSuite: TestSuites, actors: Option[List[Actor]], testCases: Option[List[TestCases]]) = {
		this(testSuite.id, testSuite.shortname, testSuite.fullname, testSuite.version,
			if(testSuite.authors.isDefined) Some(testSuite.authors.get.split(",").toList) else None,
			testSuite.originalDate,	testSuite.modificationDate, testSuite.description,
			if(testSuite.keywords.isDefined) Some(testSuite.keywords.get.split(",").toList) else None,
			actors, testCases, testSuite.specification, testSuite.filename, testSuite.hasDocumentation, testSuite.documentation, testSuite.identifier)
	}

	def this(testSuite: TestSuites, testCases: List[TestCases]) = {
		this(testSuite, None, Some(testCases))
	}

	def this(testSuite: TestSuites) = {
		this(testSuite, None, None)
	}

	def toCaseObject = {
		TestSuites(
			this.id, this.shortname, this.fullname, this.version,
			if(this.authors.isDefined) Some(this.authors.get.mkString(",")) else None,
			this.originalDate, this.modificationDate, this.description,
			if(this.keywords.isDefined) Some(this.keywords.get.mkString(",")) else None,
			this.specification, this.filename, this.hasDocumentation, this.documentation, this.identifier
		)
	}
}