/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package models

import com.gitb.core.Update

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
	filename: String,
	hasDocumentation: Boolean,
	documentation: Option[String],
	identifier: String,
	hidden: Boolean,
	shared: Boolean,
	domain: Long,
	definitionPath: Option[String],
	specReference: Option[String],
	specDescription: Option[String],
	specLink: Option[String]
) {

	def withId(id: Long): TestSuites = {
		TestSuites(id, this.shortname, this.fullname, this.version, this.authors, this.originalDate, this.modificationDate,
			this.description, this.keywords, this.filename, this.hasDocumentation, this.documentation, this.identifier, this.hidden, this.shared, this.domain, this.definitionPath,
			this.specReference, this.specDescription, this.specLink
		)
	}

	def withFileName(filename: String): TestSuites = {
		TestSuites(this.id, this.shortname, this.fullname, this.version, this.authors, this.originalDate, this.modificationDate,
			this.description, this.keywords, filename, this.hasDocumentation, this.documentation, this.identifier, this.hidden, this.shared, this.domain, this.definitionPath,
			this.specReference, this.specDescription, this.specLink
		)
	}

	def withDefinitionPath(definitionPath: Option[String]): TestSuites = {
		TestSuites(this.id, this.shortname, this.fullname, this.version, this.authors, this.originalDate, this.modificationDate,
			this.description, this.keywords, this.filename, this.hasDocumentation, this.documentation, this.identifier, this.hidden, this.shared, this.domain, definitionPath,
			this.specReference, this.specDescription, this.specLink
		)
	}
}

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
								 _testCaseUpdateApproach: Option[Map[String, Update]],
							   _filename: String,
							   _hasDocumentation: Boolean,
							   _documentation: Option[String],
							 	 _identifier: String,
  						   _hidden: Boolean,
								 _shared: Boolean,
							 	_domain: Long,
							  _specifications: Option[List[Long]] = None,
							  _definitionPath: Option[String],
							 	_updateApproach: Option[Update],
								_specReference: Option[String],
								_specDescription: Option[String],
								_specLink: Option[String],
							  _testCaseGroups: Option[List[TestCaseGroup]]
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
	var testCaseUpdateApproach: Option[Map[String, Update]] = _testCaseUpdateApproach
	var filename: String = _filename
	var hasDocumentation: Boolean = _hasDocumentation
	var documentation: Option[String] = _documentation
	var identifier: String = _identifier
	var hidden: Boolean = _hidden
	var shared: Boolean = _shared
	var domain: Long = _domain
	var specifications: Option[List[Long]] = _specifications
	var definitionPath: Option[String] = _definitionPath
	var updateApproach: Option[Update] = _updateApproach
	var specReference: Option[String] = _specReference
	var specDescription: Option[String] = _specDescription
	var specLink: Option[String] = _specLink
	var testCaseGroups: Option[List[TestCaseGroup]] = _testCaseGroups

	def this(testSuite: TestSuites, actors: Option[List[Actor]], testCases: Option[List[TestCases]], testCaseGroups: Option[List[TestCaseGroup]]) = {
		this(testSuite.id, testSuite.shortname, testSuite.fullname, testSuite.version,
			if(testSuite.authors.isDefined) Some(testSuite.authors.get.split(",").toList) else None,
			testSuite.originalDate,	testSuite.modificationDate, testSuite.description,
			if(testSuite.keywords.isDefined) Some(testSuite.keywords.get.split(",").toList) else None,
			actors, testCases, None, testSuite.filename, testSuite.hasDocumentation, testSuite.documentation, testSuite.identifier, testSuite.hidden, testSuite.shared, testSuite.domain, None, testSuite.definitionPath,
			None, testSuite.specReference, testSuite.specDescription, testSuite.specLink, testCaseGroups
		)
	}

	def this(testSuite: TestSuites, testCases: List[TestCases]) = {
		this(testSuite, None, Some(testCases), None)
	}

	def this(testSuite: TestSuites) = {
		this(testSuite, None, None, None)
	}

	def toCaseObject: TestSuites = {
		TestSuites(
			this.id, this.shortname, this.fullname, this.version,
			if(this.authors.isDefined) Some(this.authors.get.mkString(",")) else None,
			this.originalDate, this.modificationDate, this.description,
			if(this.keywords.isDefined) Some(this.keywords.get.mkString(",")) else None,
			this.filename, this.hasDocumentation, this.documentation, this.identifier, this.hidden, this.shared, this.domain, this.definitionPath,
			this.specReference, this.specDescription, this.specLink
		)
	}
}