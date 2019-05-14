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
		                  targetSpec: Long,
											targetActors: Option[String] = None,
											targetOptions: Option[String] = None,
											testSuiteOrder: Short
	                    ) {

	def withPath(path: String): TestCases = {
		TestCases(this.id, this.shortname, this.fullname, this.version, this.authors, this.originalDate,
			this.modificationDate, this.description, this.keywords, this.testCaseType, path, this.targetSpec, this.targetActors, this.targetOptions, this.testSuiteOrder)
	}

	def withTargets(spec: Long, targetActors: Option[String] = None, targetOptions:Option[String] = None): TestCases = {
		TestCases(this.id, this.shortname, this.fullname, this.version, this.authors, this.originalDate,
			this.modificationDate, this.description, this.keywords, this.testCaseType, this.path, targetSpec, targetActors, targetOptions, this.testSuiteOrder)
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
	              _targetSpec: Long,
	              _targetActors: Option[List[Actors]],
	              _targetOptions: Option[List[models.Options]],
	              _testSuiteOrder: Short
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
	var targetSpec: Long = _targetSpec
	var targetActors: Option[List[Actors]] = _targetActors
	var targetOptions: Option[List[models.Options]] = _targetOptions
	var testSuiteOrder: Short = _testSuiteOrder


	def this(_case: TestCases, targetActors: Option[List[Actors]], targetOptions: Option[List[models.Options]]) = {
		this(_case.id, _case.shortname, _case.fullname, _case.version,
			if (_case.authors.isDefined) Some(_case.authors.get.split(",").toList) else None,
			_case.originalDate, _case.modificationDate, _case.description,
			if (_case.keywords.isDefined) Some(_case.keywords.get.split(",").toList) else None,
			_case.testCaseType, _case.path, _case.targetSpec, targetActors, targetOptions, _case.testSuiteOrder)
	}

	def this(_case: TestCases) = {
		this(_case, None,	None)
	}

	def toCaseObject: TestCases = {
		TestCases(this.id, this.shortname, this.fullname, this.version,
			if (this.authors.isDefined) Some(this.authors.get.mkString(",")) else None,
			this.originalDate, this.modificationDate, this.description,
			if (this.keywords.isDefined) Some(this.keywords.get.mkString(",")) else None,
			this.testCaseType, this.path, this.targetSpec,
			if (this.targetActors.isDefined) Some(this.targetActors.get.map(_.actorId).mkString(",")) else  None,
			if (this.targetOptions.isDefined) Some(this.targetOptions.get.map(_.sname).mkString(",")) else  None,
			this.testSuiteOrder
		)
	}
}
