package models

case class TestCases(
											id: Long,
	                    shortname: String,
	                    fullname: String,
	                    version: String,
	                    authors: Option[String],
	                    originalDate: Option[String],
	                    modificationDate: Option[String],
	                    description: Option[String],
	                    keywords: Option[String],
	                    testCaseType: Short,
	                    path: String,
											targetActors: Option[String] = None,
											targetOptions: Option[String] = None,
											testSuiteOrder: Short,
											hasDocumentation: Boolean,
											documentation: Option[String],
											identifier: String,
											isOptional: Boolean,
											isDisabled: Boolean,
											tags: Option[String] = None,
											specReference: Option[String] = None,
											specDescription: Option[String] = None,
											specLink: Option[String] = None,
											group: Option[Long] = None
	                    ) {

	def withPathAndGroup(path: String, group: Option[Long]): TestCases = {
		TestCases(this.id, this.shortname, this.fullname, this.version, this.authors, this.originalDate,
			this.modificationDate, this.description, this.keywords, this.testCaseType, path, this.targetActors, this.targetOptions, this.testSuiteOrder, this.hasDocumentation, this.documentation, this.identifier, this.isOptional, this.isDisabled, this.tags,
			this.specReference, this.specDescription, this.specLink, group)
	}

}


class TestCase(
								_id: Long,
	              _sname: String,
	              _fname: String,
	              _version: String,
	              _authors: Option[List[String]],
	              _originalDate: Option[String],
	              _modificationDate: Option[String],
	              _description: Option[String],
	              _keywords: Option[List[String]],
	              _testCaseType: Short,
	              _path: String,
	              _targetActors: Option[List[Actors]],
	              _targetOptions: Option[List[models.Options]],
	              _testSuiteOrder: Short,
								_hasDocumentation: Boolean,
							  _documentation: Option[String],
								_identifier: String,
								_isOptional: Boolean,
								_isDisabled: Boolean,
								_specReference: Option[String],
								_specDescription: Option[String],
								_specLink: Option[String]
) {
	var id: Long = _id
	var shortname: String = _sname
	var fullname: String = _fname
	var version: String = _version
	var authors: Option[List[String]] = _authors
	var originalDate: Option[String] = _originalDate
	var modificationDate: Option[String] = _modificationDate
	var description: Option[String] = _description
	var keywords: Option[List[String]] = _keywords
	var testCaseType: Short = _testCaseType
	var path: String = _path
	var targetActors: Option[List[Actors]] = _targetActors
	var targetOptions: Option[List[models.Options]] = _targetOptions
	var testSuiteOrder: Short = _testSuiteOrder
	var hasDocumentation: Boolean = _hasDocumentation
	var documentation: Option[String] = _documentation
	var identifier: String = _identifier
	var isOptional: Boolean = _isOptional
	var isDisabled: Boolean = _isDisabled
	var specReference: Option[String] = _specReference
	var specDescription: Option[String] = _specDescription
	var specLink: Option[String] = _specLink

	def this(_case: TestCases, targetActors: Option[List[Actors]], targetOptions: Option[List[models.Options]]) = {
		this(_case.id, _case.shortname, _case.fullname, _case.version,
			if (_case.authors.isDefined) Some(_case.authors.get.split(",").toList) else None,
			_case.originalDate, _case.modificationDate, _case.description,
			if (_case.keywords.isDefined) Some(_case.keywords.get.split(",").toList) else None,
			_case.testCaseType, _case.path, targetActors, targetOptions, _case.testSuiteOrder, _case.hasDocumentation, _case.documentation, _case.identifier,
			_case.isOptional, _case.isDisabled, _case.specReference, _case.specDescription, _case.specLink)
	}

	def this(_case: TestCases) = {
		this(_case, None,	None)
	}

}
