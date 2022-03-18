package utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.gitb.core.{Documentation, EndpointParameter, TestCaseType, TestRoleEnumeration}
import com.gitb.utils.XMLUtils
import config.Configurations
import managers.{BaseManager, TestSuiteManager}
import models._
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.commons.lang3.{RandomStringUtils, StringUtils}
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import java.io.{File, FileOutputStream, StringWriter}
import java.nio.charset.Charset
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.zip.{ZipEntry, ZipFile}
import javax.inject.{Inject, Singleton}
import javax.xml.transform.stream.StreamSource
import scala.collection.mutable.ListBuffer
import scala.xml.XML

@Singleton
class RepositoryUtils @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

	import dbConfig.profile.api._

	import scala.jdk.CollectionConverters._

	private final val logger = LoggerFactory.getLogger("RepositoryUtils")
	private final val objectMapper = new ObjectMapper()

	private final val TEST_SUITE_ELEMENT_LABEL: String = "testsuite"
	private final val TEST_CASE_ELEMENT_LABEL: String = "testcase"
	private final val FILES_PATH: String = "files"
	private final val FILES_DP_PATH: String = "dp"
	private final val FILES_OP_PATH: String = "op"
	private final val FILES_SP_PATH: String = "sp"
	private final val FILES_EP_PATH: String = "ep"
	private final val DATA_PATH: String = "data"
	private final val DATA_PATH_IN: String = "in"
	private final val DATA_PATH_PROCESSED: String = "processed"
	private final val DATA_PATH_LOCK: String = "data.lock"
	private final val STATUS_UPDATES_PATH: String = "status-updates"

	def getFilesRootFolder(): File = {
		Paths.get(Configurations.TEST_CASE_REPOSITORY_PATH, FILES_PATH).toFile
	}

	def getDomainParametersFolder(domainId: Long): File = {
		Paths.get(Configurations.TEST_CASE_REPOSITORY_PATH, FILES_PATH, FILES_DP_PATH, domainId.toString).toFile
	}

	def getDomainParameterFile(domainId: Long, parameterId: Long): File = {
		Paths.get(getDomainParametersFolder(domainId).getAbsolutePath, parameterId.toString).toFile
	}

	def deleteDomainParameterFolder(domainId: Long): Unit = {
		val folder = getDomainParametersFolder(domainId)
		if (folder.exists()) {
			FileUtils.deleteQuietly(folder)
		}
	}

	def deleteDomainParameterFile(domainId: Long, parameterId: Long): Unit = {
		_deleteFile(getDomainParameterFile(domainId, parameterId))
	}

	def setDomainParameterFile(domainId: Long, parameterId: Long, newFile: File): Unit = {
		setDomainParameterFile(domainId, parameterId, newFile, copy = false)
	}

	def setDomainParameterFile(domainId: Long, parameterId: Long, newFile: File, copy: Boolean): Unit = {
		_setFile(getDomainParameterFile(domainId, parameterId), newFile, copy)
	}

	def getOrganisationPropertiesFolder(parameterId: Long): File = {
		Paths.get(Configurations.TEST_CASE_REPOSITORY_PATH, FILES_PATH, FILES_OP_PATH, parameterId.toString).toFile
	}

	def getOrganisationPropertyFile(parameterId: Long, organisationId: Long): File = {
		Paths.get(getOrganisationPropertiesFolder(parameterId).getAbsolutePath, s"${organisationId}_${parameterId}").toFile
	}

	def deleteOrganisationPropertiesFolder(parameterId: Long): Unit = {
		val folder = getOrganisationPropertiesFolder(parameterId)
		if (folder.exists()) {
			FileUtils.deleteQuietly(folder)
		}
	}

	def deleteOrganisationPropertyFile(parameterId: Long, organisationId: Long): Unit = {
		_deleteFile(getOrganisationPropertyFile(parameterId, organisationId))
	}

	def setOrganisationPropertyFile(parameterId: Long, organisationId: Long, newFile: File): Unit = {
		setOrganisationPropertyFile(parameterId, organisationId, newFile, copy = false)
	}

	def setOrganisationPropertyFile(parameterId: Long, organisationId: Long, newFile: File, copy: Boolean): Unit = {
		_setFile(getOrganisationPropertyFile(parameterId, organisationId), newFile, copy)
	}

	def getSystemPropertiesFolder(parameterId: Long): File = {
		Paths.get(Configurations.TEST_CASE_REPOSITORY_PATH, FILES_PATH, FILES_SP_PATH, parameterId.toString).toFile
	}

	def getSystemPropertyFile(parameterId: Long, systemId: Long): File = {
		Paths.get(getSystemPropertiesFolder(parameterId).getAbsolutePath, s"${systemId}_${parameterId}").toFile
	}

	def deleteSystemPropertiesFolder(parameterId: Long): Unit = {
		val folder = getSystemPropertiesFolder(parameterId)
		if (folder.exists()) {
			FileUtils.deleteQuietly(folder)
		}
	}

	def deleteSystemPropertyFile(parameterId: Long, systemId: Long): Unit = {
		_deleteFile(getSystemPropertyFile(parameterId, systemId))
	}

	def setSystemPropertyFile(parameterId: Long, systemId: Long, newFile: File): Unit = {
		setSystemPropertyFile(parameterId: Long, systemId: Long, newFile: File, copy = false)
	}

	def setSystemPropertyFile(parameterId: Long, systemId: Long, newFile: File, copy: Boolean): Unit = {
		_setFile(getSystemPropertyFile(parameterId, systemId), newFile, copy)
	}

	def getStatementParametersFolder(parameterId: Long): File = {
		Paths.get(Configurations.TEST_CASE_REPOSITORY_PATH, FILES_PATH, FILES_EP_PATH, parameterId.toString).toFile
	}

	def getStatementParameterFile(parameterId: Long, systemId: Long): File = {
		Paths.get(getStatementParametersFolder(parameterId).getAbsolutePath, s"${systemId}_${parameterId}").toFile
	}

	def deleteStatementParametersFolder(parameterId: Long): Unit = {
		val folder = getStatementParametersFolder(parameterId)
		if (folder.exists()) {
			FileUtils.deleteQuietly(folder)
		}
	}

	def deleteStatementParameterFile(parameterId: Long, systemId: Long): Unit = {
		_deleteFile(getStatementParameterFile(parameterId, systemId))
	}

	def setStatementParameterFile(parameterId: Long, systemId: Long, newFile: File): Unit = {
		setStatementParameterFile(parameterId, systemId, newFile, copy = false)
	}

	def setStatementParameterFile(parameterId: Long, systemId: Long, newFile: File, copy: Boolean): Unit = {
		_setFile(getStatementParameterFile(parameterId, systemId), newFile, copy)
	}

	private def _setFile(target: File, newFile: File, copy: Boolean): Unit = {
		if (newFile.exists()) {
			Files.createDirectories(target.getParentFile.toPath)
			if (copy) {
				Files.copy(newFile.toPath, target.toPath, StandardCopyOption.REPLACE_EXISTING)
			} else {
				Files.move(newFile.toPath, target.toPath, StandardCopyOption.REPLACE_EXISTING)
			}
		}
	}

	private def _deleteFile(target: File): Unit = {
		if (target.exists()) {
			FileUtils.deleteQuietly(target)
		}
		// Delete the parameter folder if empty.
		val parentContents = target.getParentFile.list()
		if (parentContents == null || parentContents.isEmpty) {
			FileUtils.deleteQuietly(target.getParentFile)
		}
	}

	def getStatusUpdatesFolder(): File = {
		new File(Configurations.TEST_CASE_REPOSITORY_PATH, STATUS_UPDATES_PATH)
	}

	def getTempFolder(): File = {
		new File("/tmp")
	}

	def getTempReportFolder(): File = {
		new File(getTempFolder(), "reports")
	}

	def getTempArchivedSessionWorkspaceFolder(): File = {
		new File(getTempFolder(), "session_archive")
	}

	def getPendingFolder(): File = {
		new File(getTempFolder(), "pending")
	}

	def getTmpValidationFolder(): File = {
		new File(getTempFolder(), "ts_validation")
	}

	def getDataRootFolder(): File = {
		val path = Paths.get(
			Configurations.TEST_CASE_REPOSITORY_PATH, DATA_PATH
		)
		path.toFile
	}

	def createDataLockFile(): Boolean = {
		val lockFile = getDataLockFile()
		if (!lockFile.exists()) {
			lockFile.getParentFile.mkdirs()
			lockFile.createNewFile()
		} else {
			false
		}
	}

	def getDataLockFile(): File = {
		getDataRootFolder().toPath.resolve(DATA_PATH_LOCK).toFile
	}

	def getDataInFolder(): File = {
		getDataRootFolder().toPath.resolve(DATA_PATH_IN).toFile
	}

	def getDataProcessedFolder(): File = {
		getDataRootFolder().toPath.resolve(DATA_PATH_PROCESSED).toFile
	}

	def getTestSuitesRootFolder(): File = {
		val path = Paths.get(
			Configurations.TEST_CASE_REPOSITORY_PATH,
			TestSuiteManager.TEST_SUITES_PATH
		)
		path.toFile
	}

	def getDomainTestSuitesPath(domainId: Long): File = {
		val path = Paths.get(
			getTestSuitesRootFolder().getAbsolutePath,
			String.valueOf(domainId)
		)
		path.toFile
	}

	def getTestSuitesPath(domainId: Long, specificationId: Long): File = {
		val path = Paths.get(
			getDomainTestSuitesPath(domainId).getAbsolutePath,
			String.valueOf(specificationId)
		)
		path.toFile
	}

	def getTestSuitesResource(specId: Long, domainId: Long, resourcePath: String, pathToAlsoCheck: Option[String]): File = {
		var file = new File(getTestSuitesPath(domainId, specId), resourcePath)
		if(!file.exists()) {
			if (pathToAlsoCheck.isDefined) {
				file = new File(getTestSuitesPath(domainId, specId), pathToAlsoCheck.get)
			}
			if (!file.exists()) {
				// Backwards compatibility: Lookup directly under the test-suites folder
				file = new File(getTestSuitesRootFolder(), resourcePath)
			}
		}
		file
	}

	def getTestSuitesResource(spec: Specifications, resourcePath: String, pathToAlsoCheck: Option[String]): File = {
		getTestSuitesResource(spec.id, spec.domain, resourcePath, pathToAlsoCheck)
	}

	/**
		* Extracts the test suite resources in the <code>file</code> into the <code>targetFolder</code>
		* @param targetFolder
		* @param file
		* @return id->path maps for the test case files
		*/
	def extractTestSuiteFilesFromZipToFolder(targetFolder: File, file: File): Map[String, String] = {
		val testCasePaths = collection.mutable.HashMap[String, String]()

		if(!targetFolder.exists) {
			throw new Exception("Target folder does not exists")
		}

		if(!targetFolder.isDirectory) {
			throw new Exception("Target folder is not a directory")
		}

		if(!file.exists) {
			throw new Exception("Zip file does not exists")
		}

		val zip = new ZipFile(file)
		try {
			zip.entries().asScala.foreach {
				zipEntry =>
					val newFile = new File(targetFolder, zipEntry.getName)

					if(zipEntry.isDirectory) {
						logger.debug("Creating folder ["+newFile+"]")
						newFile.mkdirs()
					} else {
						if(!newFile.exists) {
							newFile.getParentFile.mkdirs()
							newFile.createNewFile()
							logger.debug("Creating new file ["+newFile+"]")

							if(isTestCase(zip, zipEntry)) {
								val testCase: com.gitb.tdl.TestCase = getTestCase(zip, zipEntry)

								logger.debug("File ["+newFile+"] is a test case file")

								testCasePaths.update(testCase.getId, targetFolder.getParentFile.toURI.relativize(newFile.toURI).getPath)
							}

							val fos = new FileOutputStream(newFile, false)

							fos.write(IOUtils.toByteArray(zip.getInputStream(zipEntry)))

							fos.close()
							logger.debug("Wrote ["+newFile+"]")
						} else {
							logger.debug("File ["+newFile+"] is already exist")
						}
					}
			}
		} finally {
			zip.close()
		}

		testCasePaths.iterator.toMap
	}

	def generateTestSuiteFileName(): String = {
		val fileName = "ts_"+RandomStringUtils.random(10, false, true)
		fileName
	}

	private def getDocumentation(testSuiteId: String, documentation: Documentation, testSuiteArchive: ZipFile, specification: Long, domain: Option[Long]): Option[String] = {
		var documentationText: Option[String] = None
		if (documentation != null) {
			if (documentation.getValue != null && !documentation.getValue.isBlank) {
				documentationText = Some(documentation.getValue.trim)
			} else if (documentation.getImport != null) {
				var documentationBytes: Option[Array[Byte]] = None
				if (StringUtils.isBlank(documentation.getFrom) || documentation.getFrom.equals(testSuiteId)) {
					// Look up from current test suite.
					var referencedEntry = testSuiteArchive.getEntry(documentation.getImport)
					if (referencedEntry == null) {
						// It might be prefixed with the testSuiteId.
						val testSuiteIdPath = testSuiteId+"/"
						if (documentation.getImport.startsWith(testSuiteIdPath) && documentation.getImport.length() > testSuiteIdPath.length()) {
							referencedEntry = testSuiteArchive.getEntry(documentation.getImport.substring(testSuiteIdPath.length()))
						}
					}
					if (referencedEntry != null) {
						documentationBytes = Some(IOUtils.toByteArray(testSuiteArchive.getInputStream(referencedEntry)))
					}
				} else if (documentation.getFrom != null) {
					// Look up from another test suite in the domain.
					var domainId: Long = -1
					if (domain.isDefined) {
						domainId = domain.get
					} else {
						domainId = domainIdOfSpecification(specification)
					}
					val testSuite = findTestSuiteByIdentifier(documentation.getFrom, Some(domainId), specification)
					if (testSuite.isDefined) {
						var filePathToLookup = documentation.getImport
						var filePathToAlsoCheck: Option[String] = null
						if (!documentation.getImport.startsWith(testSuite.get.identifier) && !documentation.getImport.startsWith("/"+testSuite.get.identifier)) {
							filePathToLookup = testSuite.get.filename + "/" + filePathToLookup
							filePathToAlsoCheck = None
						} else {
							filePathToAlsoCheck = Some(testSuite.get.filename + "/" + filePathToLookup)
							filePathToLookup = StringUtils.replaceOnce(filePathToLookup, testSuite.get.identifier, testSuite.get.filename)
						}
						val testSuiteFolder = getTestSuitesResource(testSuite.get.specification, domainId, testSuite.get.filename, None)
						val file = getTestSuitesResource(testSuite.get.specification, domainId, filePathToLookup, filePathToAlsoCheck)
						if (file.exists() && file.toPath.normalize().startsWith(testSuiteFolder.toPath.normalize())) {
							documentationBytes = Some(FileUtils.readFileToByteArray(file))
						}
					}
				}
				if (documentationBytes.isEmpty) {
					logger.warn("Documentation import resource ["+documentation.getImport+"] was not found.")
				} else {
					var encoding = Charset.defaultCharset()
					if (documentation.getEncoding != null && !documentation.getEncoding.isBlank) {
						encoding = Charset.forName(documentation.getEncoding)
					}
					documentationText = Some(new String(documentationBytes.get, encoding))
				}
			}
		}
		if (documentationText.isDefined) {
			Some(HtmlUtil.sanitizeEditorContent(documentationText.get))
		} else {
			None
		}
	}

	def getTestSuiteFromZip(specification: Long, file: File): Option[TestSuite] = {
		getTestSuiteFromZip(specification, file, completeParse = true)
	}

	private def containsExternalDocumentation(tdlTestSuite: com.gitb.tdl.TestSuite, tdlTestCases: List[com.gitb.tdl.TestCase]): Boolean = {
		if (tdlTestSuite.getMetadata != null
			&& tdlTestSuite.getMetadata.getDocumentation != null
			&& tdlTestSuite.getMetadata.getDocumentation.getFrom != null
			&& !tdlTestSuite.getMetadata.getDocumentation.getFrom.equals(tdlTestSuite.getId)) {
			return true
		}
		tdlTestCases.foreach { tc =>
			if (tc.getMetadata != null
				&& tc.getMetadata.getDocumentation != null
				&& tc.getMetadata.getDocumentation.getFrom != null
				&& !tc.getMetadata.getDocumentation.getFrom.equals(tdlTestSuite.getId)) {
				return true
			}
		}
		false
	}

	private def toActorList(actors: com.gitb.core.Actors): List[com.gitb.core.Actor] = {
		val results = new ListBuffer[com.gitb.core.Actor]()
		if (actors != null) {
			actors.getActor.forEach { actor =>
				results += actor
			}
		}
		results.toList
	}

	def getTestSuiteFromZip(specification :Long, file: File, completeParse: Boolean): Option[TestSuite] = {
		var result: Option[TestSuite] = None
		if(file.exists) {
			var zip: ZipFile = null
			try {
				zip = new ZipFile(file)
				val testSuiteEntries = zip.entries().asScala.filter(isTestSuite(zip, _))
				if(testSuiteEntries.hasNext) {
					val tdlTestCases = zip.entries().asScala.filter(isTestCase(zip, _)).map(getTestCase(zip, _)).toList
					val testSuiteEntry = testSuiteEntries.next()
					val testSuite = {
						val tdlTestSuite: com.gitb.tdl.TestSuite = getTestSuite(zip, testSuiteEntry)
						val identifier: String = tdlTestSuite.getId
						val name: String = tdlTestSuite.getMetadata.getName
						val version: String = tdlTestSuite.getMetadata.getVersion
						val authors: String = tdlTestSuite.getMetadata.getAuthors
						val originalDate: String = tdlTestSuite.getMetadata.getPublished
						val modificationDate: String = tdlTestSuite.getMetadata.getLastModified
						val description: String = tdlTestSuite.getMetadata.getDescription
						val tdlActors = toActorList(tdlTestSuite.getActors)
						val tdlTestCaseEntries = tdlTestSuite.getTestcase.asScala
						val fileName = generateTestSuiteFileName()
						var documentation: Option[String] = None
						var domainId: Option[Long] = None
						if (containsExternalDocumentation(tdlTestSuite, tdlTestCases)) {
							domainId = Some(domainIdOfSpecification(specification))
						}
						if (completeParse && tdlTestSuite.getMetadata.getDocumentation != null) {
							documentation = getDocumentation(tdlTestSuite.getId, tdlTestSuite.getMetadata.getDocumentation, zip, specification, domainId)
						}
						val caseObject = TestSuites(0L, name, name, version, Option(authors), Option(originalDate), Option(modificationDate), Option(description), None, specification, fileName, documentation.isDefined, documentation, identifier, tdlTestCaseEntries.isEmpty)
						val actors = tdlActors.map { tdlActor =>
							val endpoints = tdlActor.getEndpoint.asScala.map { tdlEndpoint => // construct actor endpoints
								val parameters = tdlEndpoint.getConfig.asScala.map { tdlParameter =>
									var dependsOn = Option(tdlParameter.getDependsOn)
									var dependsOnValue = Option(tdlParameter.getDependsOnValue)
									if (dependsOn.isDefined && dependsOn.get.trim.equals("")) {
										dependsOn = None
									}
									if (dependsOn.isEmpty || (dependsOnValue.isDefined && dependsOnValue.get.trim.equals(""))) {
										dependsOnValue = None
									}
									val allowedValues = getAllowedValuesStr(tdlParameter)
									models.Parameters(0L, tdlParameter.getName, Option(tdlParameter.getDesc), tdlParameter.getUse.value(), tdlParameter.getKind.value(), tdlParameter.isAdminOnly, tdlParameter.isNotForTests, tdlParameter.isHidden, allowedValues, 0, dependsOn, dependsOnValue, 0L)
								}.toList
								new Endpoint(Endpoints(0L, tdlEndpoint.getName, Option(tdlEndpoint.getDesc), 0L), parameters)
							}.toList
							var displayOrder: Option[Short] = None
							if (tdlActor.getDisplayOrder != null) {
								displayOrder = Some(tdlActor.getDisplayOrder)
							}
							new Actor(0L, tdlActor.getId, tdlActor.getName, Option(tdlActor.getDesc), Option(tdlActor.isDefault), tdlActor.isHidden, displayOrder, None, Some(endpoints), None, None)
						}

						var testCases: Option[List[TestCases]] = None
						if (completeParse) {
							var testCaseCounter = 0
							testCases = Some(tdlTestCaseEntries.map {
								entry =>
									testCaseCounter += 1
									val tdlTestCase = tdlTestCases.find(_.getId == entry.getId).get
									val actorString = new StringBuilder
									tdlTestCase.getActors.getActor.asScala.foreach(role => {
										actorString.append(role.getId)
										if (role.getRole == TestRoleEnumeration.SUT) {
											actorString.append("[SUT]")
										}
										actorString.append(',')
									})
									actorString.deleteCharAt(actorString.length - 1)

									var testCaseType = TestCaseType.CONFORMANCE
									if (Option(tdlTestCase.getMetadata.getType).isDefined) {
										testCaseType = tdlTestCase.getMetadata.getType
									}
									var documentation: Option[String] = None
									if (tdlTestCase.getMetadata.getDocumentation != null) {
										documentation = getDocumentation(tdlTestSuite.getId, tdlTestCase.getMetadata.getDocumentation, zip, specification, domainId)
									}
									TestCases(
										0L, tdlTestCase.getMetadata.getName, tdlTestCase.getMetadata.getName, tdlTestCase.getMetadata.getVersion,
										Option(tdlTestCase.getMetadata.getAuthors), Option(tdlTestCase.getMetadata.getPublished),
										Option(tdlTestCase.getMetadata.getLastModified), Option(tdlTestCase.getMetadata.getDescription),
										None, testCaseType.ordinal().toShort, null, specification, Some(actorString.toString()), None,
										testCaseCounter.toShort, documentation.isDefined, documentation, tdlTestCase.getId
									)
							}.toList)
						}
						new TestSuite(caseObject, Some(actors), testCases)
					}
					result = Some(testSuite)
				}
			} finally {
				if (zip != null) {
					zip.close()
				}
			}

		}
		result
	}

	private def getAllowedValuesStr(parameter: EndpointParameter): Option[String] = {
		var result: Option[String] = None
		if (parameter != null && parameter.getAllowedValues != null) {
			val values = StringUtils.split(parameter.getAllowedValues, ',').map(s => s.trim)
			var labels: Array[String] = null
			if (parameter.getAllowedValueLabels == null || parameter.getAllowedValueLabels.isBlank) {
				labels = values
			} else {
				labels = StringUtils.split(parameter.getAllowedValueLabels, ',').map(s => s.trim)
			}
			if (values.length == labels.length) {
				var counter = 0
				val pairs = ListBuffer[ValueWithLabel]()
				values.foreach { value =>
					pairs += new ValueWithLabel(value, labels(counter))
					counter += 1
				}
				val writer = new StringWriter()
				try {
					objectMapper.writeValue(writer, pairs.toArray)
					result = Some(writer.toString)
				} catch {
					case e: Exception =>
						logger.warn("Failed to generate allowed values as JSON", e)
						result = None
				}
			}
		}
		result
	}

	private def getTestSuite(zip: ZipFile, entry: ZipEntry): com.gitb.tdl.TestSuite = {
		val stream = zip.getInputStream(entry)

		val testSuite = XMLUtils.unmarshal(classOf[com.gitb.tdl.TestSuite], new StreamSource(stream))
		testSuite
	}

	private def getTestCase(zip: ZipFile, entry: ZipEntry): com.gitb.tdl.TestCase = {
		val stream = zip.getInputStream(entry)

		val testCase = XMLUtils.unmarshal(classOf[com.gitb.tdl.TestCase], new StreamSource(stream))
		testCase
	}

	private def isTestSuite(zip: ZipFile, entry: ZipEntry): Boolean = {
		testXMLElementTagInZipEntry(zip, entry, TEST_SUITE_ELEMENT_LABEL)
	}

	private def isTestCase(zip: ZipFile, entry: ZipEntry): Boolean = {
		testXMLElementTagInZipEntry(zip, entry, TEST_CASE_ELEMENT_LABEL)
	}

	private def testXMLElementTagInZipEntry(zip: ZipFile, entry: ZipEntry, tag: String): Boolean = {
		if(!entry.isDirectory) {
			try {
				val stream = zip.getInputStream(entry)
				val xml = XML.load(stream)

				xml.label == tag
			} catch {
				case _: Exception => false
			}
		} else {
			false
		}
	}

	/**
		* Extracts the test suite resources in the <code>file</code> into the <code>targetFolderName</code> folder.
		* A folder named <code>targetFolderName</code> is created if it does not exist.
		* @param targetFolder
		* @param tempFolder
		* @return id->path maps for the test case files
		*/
	def extractTestSuiteFilesFromZipToFolder(specification: Long, targetFolder: File, tempFolder: File): Map[String, String] = {
		//target folder needs to be deleted due to an unknown exception thrown
		if(targetFolder.exists()){
			FileUtils.forceDelete(targetFolder);
		}

		logger.info("Creating folder ["+targetFolder+"]")
		targetFolder.mkdirs()

		extractTestSuiteFilesFromZipToFolder(targetFolder, tempFolder)
	}

	def deleteDomainTestSuiteFolder(domainId: Long): Unit = {
		val targetFolder = getDomainTestSuitesPath(domainId)
		FileUtils.deleteDirectory(targetFolder)
	}

	def undeployTestSuite(spec: Specifications, testSuiteName: String): Unit = {
		val targetFolder = getTestSuitesResource(spec, testSuiteName, None)
		FileUtils.deleteDirectory(targetFolder)
	}

	def getTestSuitesPath(spec: Specifications): File = {
		getTestSuitesPath(spec.domain, spec.id)
	}

	def deleteSpecificationTestSuiteFolder(spec: Specifications): Unit = {
		val targetFolder = getTestSuitesPath(spec)
		FileUtils.deleteDirectory(targetFolder)
	}

	private def domainIdOfSpecification(specification: Long): Long = {
		exec(PersistenceSchema.specifications.filter(_.id === specification).map(x => x.domain).result).head
	}

	def findTestSuiteByIdentifier(identifier: String, domain: Option[Long], specificationToPrioritise: Long): Option[TestSuites] = {
		var domainId: Long = -1
		if (domain.isDefined) {
			domainId = domain.get
		} else {
			domainId = domainIdOfSpecification(specificationToPrioritise)
		}

		var result: Option[TestSuites] = None
		val testSuites = exec(PersistenceSchema.testSuites
			.join(PersistenceSchema.specifications).on(_.specification === _.id)
			.filter(_._1.identifier === identifier)
			.filter(_._2.domain === domainId)
			.map(x => x._1)
			.result
		)
		if (testSuites.nonEmpty) {
			result = testSuites.find { t =>
				t.specification == specificationToPrioritise
			}
			if (result.isEmpty) {
				result = Some(testSuites.head)
			}
		}
		result
	}

	def getPathForTestSessionData(folderInfo: SessionFolderInfo): Path = {
		getPathForTestSessionData(folderInfo.path)
	}

	def getPathForTestSessionData(sessionFolder: Path): Path = {
		Path.of(sessionFolder.toString, "data")
	}

	def getPathForTestSession(sessionId: String, isExpected: Boolean): SessionFolderInfo = {
		val testResult = exec(PersistenceSchema.testResults.filter(_.testSessionId === sessionId).result.headOption)
		var startTime: Option[Timestamp] = None
		if (testResult.isDefined) {
			startTime = Some(testResult.get.startTime)
		}
		getPathForTestSessionObj(sessionId, startTime, isExpected)
	}

	def getPathForTestSessionObj(sessionId: String, sessionStartTime: Option[Timestamp], isExpected: Boolean): SessionFolderInfo = {
		var startTime: LocalDateTime = null
		if (sessionStartTime.isDefined) {
			startTime = sessionStartTime.get.toLocalDateTime
		} else {
			// We have no DB entry only in the case of preliminary steps.
			startTime = LocalDateTime.now()
		}
		val statusUpdateFolderPath = getStatusUpdatesFolder().getAbsolutePath
		val path = Paths.get(
			statusUpdateFolderPath,
			String.valueOf(startTime.getYear),
			String.valueOf(startTime.getMonthValue),
			String.valueOf(startTime.getDayOfMonth),
			sessionId
		)
		if (isExpected && !Files.exists(path)) {
			// For backwards compatibility. Lookup session folder directly under status-updates folder
			val otherPath = Paths.get(statusUpdateFolderPath, sessionId)
			if (Files.exists(otherPath)) {
				SessionFolderInfo(otherPath, archived = false)
			} else {
				// Test sessions may be archived if old. Check to see if we have such an archive.
				val archivePath = Paths.get(
					statusUpdateFolderPath,
					String.valueOf(startTime.getYear),
					String.valueOf(startTime.getMonthValue)+".zip"
				)
				if (Files.exists(archivePath)) {
					// Unzip session folder from ZIP archive into temp folder.
					val pathFromArchive:Option[Path] = None
					val zipFs = FileSystems.newFileSystem(archivePath, null)
					val pathInArchive = zipFs.getPath("/", String.valueOf(startTime.getDayOfMonth), sessionId)
					val targetPath = Path.of(getTempArchivedSessionWorkspaceFolder().getAbsolutePath, sessionId+"_"+System.currentTimeMillis().toString)
					if (Files.exists(pathInArchive)) {
						Files.walkFileTree(pathInArchive, new SimpleFileVisitor[Path]() {
							override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
								// Conserve hierarchy.
								val relativePathInZip = pathInArchive.relativize(file)
								val extractPath = targetPath.resolve(relativePathInZip.toString)
								Files.createDirectories(extractPath.getParent)
								// Extract file.
								Files.copy(file, extractPath)
								FileVisitResult.CONTINUE
							}
						})
						SessionFolderInfo(targetPath, archived = true)
					} else {
						// This is for test sessions that have no report.
						SessionFolderInfo(path, archived = false)
					}
				} else {
					// This is for test sessions that have no report.
					SessionFolderInfo(path, archived = false)
				}
			}
		} else {
			SessionFolderInfo(path, archived = false)
		}
	}

}