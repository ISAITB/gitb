package utils

import java.io.{File, FileOutputStream}
import java.nio.charset.Charset
import java.nio.file.Paths
import java.util.zip.{ZipEntry, ZipFile}

import com.gitb.core.{Documentation, TestCaseType, TestRoleEnumeration}
import com.gitb.utils.XMLUtils
import config.Configurations
import javax.xml.transform.stream.StreamSource
import managers.TestSuiteManager
import models._
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.xml.XML

object RepositoryUtils {

	private final val logger = LoggerFactory.getLogger("RepositoryUtils")

	private final val TEST_SUITE_ELEMENT_LABEL: String = "testsuite"
	private final val TEST_CASE_ELEMENT_LABEL: String = "testcase"
	private final val DATA_PATH: String = "data"
	private final val DATA_PATH_IN: String = "in"
	private final val DATA_PATH_PROCESSED: String = "processed"
	private final val DATA_PATH_LOCK: String = "data.lock"

	def getDataRootFolder(): File = {
		val path = Paths.get(
			Configurations.TEST_CASE_REPOSITORY_PATH, DATA_PATH
		)
		path.toFile
	}

	def createDataLockFile(): Boolean = {
		val lockFile = RepositoryUtils.getDataLockFile()
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

	def getTestSuitesResource(spec: Specifications, resourcePath: String, pathToAlsoCheck: Option[String]): File = {
		var file = new File(getTestSuitesPath(spec), resourcePath)
		if(!file.exists()) {
			if (pathToAlsoCheck.isDefined) {
				file = new File(getTestSuitesPath(spec), pathToAlsoCheck.get)
			}
			if (!file.exists()) {
				// Backwards compatibility: Lookup directly under the test-suites folder
				file = new File(getTestSuitesRootFolder(), resourcePath)
			}
		}
		file
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

		testCasePaths.toMap
	}

	def generateTestSuiteFileName(): String = {
		val fileName = "ts_"+RandomStringUtils.random(10, false, true)
		fileName
	}

	private def getDocumentation(testSuiteId: String, documentation: Documentation, testSuiteArchive: ZipFile): Option[String] = {
		var documentationText: Option[String] = None
		if (documentation != null) {
			if (documentation.getValue != null && !documentation.getValue.isBlank) {
				documentationText = Some(documentation.getValue.trim)
			} else if (documentation.getImport != null) {
				var referencedEntry = testSuiteArchive.getEntry(documentation.getImport)
				if (referencedEntry == null) {
					// It might be prefixed with the testSuiteId.
					val testSuiteIdPath = testSuiteId+"/"
					if (documentation.getImport.startsWith(testSuiteIdPath) && documentation.getImport.length() > testSuiteIdPath.length()) {
						referencedEntry = testSuiteArchive.getEntry(documentation.getImport.substring(testSuiteIdPath.length()))
					}
				}
				if (referencedEntry == null) {
					logger.warn("Documentation import resource ["+documentation.getImport+"] was not found in the test suite archive. This should have been caught at validation time.")
				} else {
					val documentationBytes = IOUtils.toByteArray(testSuiteArchive.getInputStream(referencedEntry))
					var encoding = Charset.defaultCharset()
					if (documentation.getEncoding != null && !documentation.getEncoding.isBlank) {
						encoding = Charset.forName(documentation.getEncoding)
					}
					documentationText = Some(new String(documentationBytes, encoding))
				}
			}
		}
		if (documentationText.isDefined) {
			Some(HtmlUtil.sanitizeEditorContent(documentationText.get))
		} else {
			None
		}
	}

	def getTestSuiteFromZip(specification:Option[Long], file: File): Option[TestSuite] = {
		getTestSuiteFromZip(specification, file, completeParse = true)
	}

	def getTestSuiteFromZip(specification:Option[Long], file: File, completeParse: Boolean): Option[TestSuite] = {
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
						val tdlActors = tdlTestSuite.getActors.getActor.asScala
						val tdlTestCaseEntries = tdlTestSuite.getTestcase.asScala
						val fileName = generateTestSuiteFileName()
						var documentation: Option[String] = None
						if (completeParse && tdlTestSuite.getMetadata.getDocumentation != null) {
							documentation = getDocumentation(tdlTestSuite.getId, tdlTestSuite.getMetadata.getDocumentation, zip)
						}
						val caseObject = TestSuites(0L, name, name, version, Option(authors), Option(originalDate), Option(modificationDate), Option(description), None, specification.getOrElse(0L), fileName, documentation.isDefined, documentation, identifier)
						val actors = tdlActors.map { tdlActor =>
							val endpoints = tdlActor.getEndpoint.asScala.map { tdlEndpoint => // construct actor endpoints
								val parameters = tdlEndpoint.getConfig.asScala
									.map(tdlParameter => Parameters(0L, tdlParameter.getName, Option(tdlParameter.getDesc), tdlParameter.getUse.value(), tdlParameter.getKind.value(), tdlParameter.isAdminOnly, tdlParameter.isNotForTests, 0l))
									.toList
								new Endpoint(Endpoints(0L, tdlEndpoint.getName, Option(tdlEndpoint.getDesc), 0L), parameters)
							}.toList
							var displayOrder: Option[Short] = None
							if (tdlActor.getDisplayOrder != null) {
								displayOrder = Some(tdlActor.getDisplayOrder)
							}
							new Actor(Actors(0L, tdlActor.getId, tdlActor.getName, Option(tdlActor.getDesc), Option(tdlActor.isDefault), tdlActor.isHidden, displayOrder,  0L), endpoints)
						}.toList

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
										documentation = getDocumentation(tdlTestSuite.getId, tdlTestCase.getMetadata.getDocumentation, zip)
									}
									TestCases(
										0L, tdlTestCase.getMetadata.getName, tdlTestCase.getMetadata.getName, tdlTestCase.getMetadata.getVersion,
										Option(tdlTestCase.getMetadata.getAuthors), Option(tdlTestCase.getMetadata.getPublished),
										Option(tdlTestCase.getMetadata.getLastModified), Option(tdlTestCase.getMetadata.getDescription),
										None, testCaseType.ordinal().toShort, null, specification.getOrElse(0L), Some(actorString.toString()), None,
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

}