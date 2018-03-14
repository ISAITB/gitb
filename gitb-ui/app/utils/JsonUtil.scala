package utils

import java.util

import models.Enums.TestResultStatus
import models._
import play.api.libs.json._
import scala.collection.JavaConverters._
object JsonUtil {

	def jsTestSuite(suite: TestSuites): JsObject = {
    val json = Json.obj(
      "id"                -> suite.id,
      "sname"             -> suite.shortname,
      "fname"             -> suite.fullname,
      "version"           -> suite.version,
      "specification"     -> suite.specification,
      "authors"           -> (if(suite.authors.isDefined) suite.authors.get else JsNull),
      "description"       -> (if(suite.description.isDefined) suite.description.get else JsNull),
      "keywords"          -> (if(suite.keywords.isDefined) suite.keywords.get else JsNull),
      "modificationDate"  -> (if(suite.modificationDate.isDefined) suite.modificationDate.get else JsNull),
      "originalDate"      -> (if(suite.originalDate.isDefined) suite.originalDate.get else JsNull)
    )
    json
  }

  def jsTestSuitesList(list: List[TestSuites]) = {
    var json = Json.arr()
    list.foreach { testSuite =>
      json = json.append(jsTestSuite(testSuite))
    }
    json
  }

  def jsTestSuite(testSuite: TestSuite): JsObject = {
    var jTestSuite: JsObject = jsTestSuite(testSuite.toCaseObject)
    if (testSuite.testCases.isDefined) {
      jTestSuite = jTestSuite ++ Json.obj("testCases" -> jsTestCasesList(testSuite.testCases.get))
    } else {
      jTestSuite = jTestSuite ++ Json.obj("testCases" -> JsNull)
    }
    jTestSuite
  }

  def jsTestSuiteList(testSuites: List[TestSuite]): JsArray = {
    var json = Json.arr()
    testSuites.foreach { testSuite =>
      json = json.append(jsTestSuite(testSuite))
    }
    json
  }

  def jsEndpoint(endpoint: Endpoint) = {
		val json = Json.obj(
			"id" -> endpoint.id,
			"name" -> endpoint.name,
			"description" -> (if(endpoint.desc.isDefined) endpoint.desc.get else JsNull),
			"actor" -> (if(endpoint.actor.isDefined) jsActor(endpoint.actor.get) else JsNull),
			"parameters" -> {
				var json = Json.arr()
				endpoint.parameters match {
					case Some(parameters) => parameters.foreach { parameter =>
						json = json.append(jsParameter(parameter))
					}
					case None => JsNull
				}
				json
			}
		)
		json
	}

	def jsEndpoints(list: List[Endpoint]) = {
		var json = Json.arr()
		list.foreach { endpoint =>
			json = json.append(jsEndpoint(endpoint))
		}
		json
	}

	def jsParameter(parameter: Parameters) = {
		val json = Json.obj(
			"id" -> parameter.id,
			"name" -> parameter.name,
			"desc" -> parameter.desc,
			"use" -> parameter.use,
			"kind" -> parameter.kind,
			"endpoint" -> parameter.endpoint
		)
		json
	}

	def jsParameters(list: List[Parameters]) = {
		var json = Json.arr()
		list.foreach { parameter =>
			json = json.append(jsParameter(parameter))
		}
		json
	}

  /**
   * Converts a User object into Play!'s JSON notation.
   * Does not support cross object conversion
   * @param user User object to be converted
   * @return JsObject
   */
  def jsUser(user:Users):JsObject = {
    val json = Json.obj(
      "id"    -> user.id,
      "name"  -> user.name,
      "email" -> user.email,
      "role"  -> user.role
    )
    json
  }

  def jsSystemConfiguration(sc: SystemConfigurations):JsObject = {
    val json = Json.obj(
      "name"    -> sc.name,
      "parameter"  -> (if(sc.parameter.isDefined) sc.parameter.get else JsNull),
      "description"  -> (if(sc.description.isDefined) sc.description.get else JsNull)
    )
    json
  }

  /**
   * Converts a List of Users into Play!'s JSON notation
   * Does not support cross object conversion
   * @param list List of Users to be convert
   * @return JsArray
   */
  def jsUsers(list:List[Users]):JsArray = {
    var json = Json.arr()
    list.foreach{ user =>
      json = json.append(jsUser(user))
    }
    json
  }

  /**
   * Converts an Organization object into Play!'s JSON notation.
   * Does not support cross object conversion
   * @param organization Organization object to be converted
   * @return JsObject
   */
  def jsOrganization(organization:Organizations):JsObject = {
    val json = Json.obj(
      "id"    -> organization.id,
      "sname" -> organization.shortname,
      "fname" -> organization.fullname,
      "type"  -> organization.organizationType,
      "landingPage" -> (if(organization.landingPage.isDefined) organization.landingPage.get else JsNull),
      "legalNotice" -> (if(organization.legalNotice.isDefined) organization.legalNotice.get else JsNull),
      "community" -> organization.community,
      "adminOrganization" -> organization.adminOrganization
    )
    json
  }

  /**
   * Converts a List of Organizations into Play!'s JSON notation
   * Does not support cross object conversion
   * @param list List of Organizations to be convert
   * @return JsArray
   */
  def jsOrganizations(list:List[Organizations]):JsArray = {
    var json = Json.arr()
    list.foreach{ organization =>
      json = json.append(jsOrganization(organization))
    }
    json
  }

  /**
   * Converts a System object into Play!'s JSON notation.
   * Does not support cross object conversion
   * @param system System object to be converted
   * @return JsObject
   */
  def jsSystem(system:Systems):JsObject = {
    val json = Json.obj(
      "id"    -> system.id,
      "sname" -> system.shortname,
      "fname" -> system.fullname,
      "description" -> (if(system.description.isDefined) system.description.get else JsNull),
      "version" -> system.version,
      "owner" -> system.owner
    )
    json
  }

  /**
   * Converts a List of Systems into Play!'s JSON notation
   * Does not support cross object conversion
   * @param list List of Systems to be converted
   * @return JsArray
   */
  def jsSystems(list:List[Systems]):JsArray = {
    var json = Json.arr()
    list.foreach{ system =>
      json = json.append(jsSystem(system))
    }
    json
  }

  def jsCommunities(list:List[Communities]):JsArray = {
    var json = Json.arr()
    list.foreach{ community =>
      json = json.append(jsCommunity(community))
    }
    json
  }

  def jsCommunity(community:Communities):JsObject = {
    val json = Json.obj(
      "id"    -> community.id,
      "sname" -> community.shortname,
      "fname" -> community.fullname,
      "domainId" -> community.domain
    )
    json
  }

  def serializeCommunity(community:Community):String = {
    var jCommunity:JsObject = jsCommunity(community.toCaseObject)
    if(community.domain.isDefined){
      jCommunity = jCommunity ++ Json.obj("domain" -> jsDomain(community.domain.get))
    } else{
      jCommunity = jCommunity ++ Json.obj("domain" -> JsNull)
    }
    jCommunity.toString
  }

  /**
   * Converts a Domain object into Play!'s JSON notation.
   * @param domain Domain object to be converted
   * @return JsObject
   */
  def jsDomain(domain:Domain):JsObject = {
    val json = Json.obj(
      "id" -> domain.id,
      "sname" -> domain.shortname,
      "fname" -> domain.fullname,
      "description" -> domain.description
    )
    json
  }

  /**
   * Converts a List of Domains into Play!'s JSON notation
   * @param list List of Domains to be converted
   * @return JsArray
   */
  def jsDomains(list:List[Domain]):JsArray = {
    var json = Json.arr()
    list.foreach{ domain =>
      json = json.append(jsDomain(domain))
    }
    json
  }

  /**
   * Converts a Specification object into Play!'s JSON notation.
   * @param spec Specification object to be converted
   * @return JsObject
   */
  def jsSpecification(spec:Specifications) : JsObject = {
    val json = Json.obj(
      "id"      -> spec.id,
      "sname"   -> spec.shortname,
      "fname"   -> spec.fullname,
      "urls"    -> (if(spec.urls.isDefined) spec.urls.get else JsNull),
      "diagram" -> (if(spec.diagram.isDefined) spec.diagram.get else JsNull),
      "description" -> (if(spec.description.isDefined) spec.description.get else JsNull),
      "spec_type"    -> spec.specificationType,
      "domain"  -> spec.domain
    )
    return json;
  }

  /**
   * Converts a List of Specifications into Play!'s JSON notation
   * @param list List of Specifications to be converted
   * @return JsArray
   */
  def jsSpecifications(list:List[Specifications]):JsArray = {
    var json = Json.arr()
    list.foreach{ spec =>
      json = json.append(jsSpecification(spec))
    }
    json
  }

  /**
   * Converts a Actor object into Play!'s JSON notation.
   * @param actor Actor object to be converted
   * @return JsObject
   */
  def jsActor(actor:Actors) : JsObject = {
    val json = Json.obj(
      "id" -> actor.id,
      "actorId" -> actor.actorId,
      "name"   -> actor.name,
      "description" -> (if(actor.description.isDefined) actor.description.get else JsNull),
      "domain"  -> actor.domain
    )
    return json;
  }

  /**
   * Converts a List of Actors into Play!'s JSON notation
   * @param list List of Actors to be converted
   * @return JsArray
   */
  def jsActors(list:List[Actors]):JsArray = {
    var json = Json.arr()
    list.foreach{ actor =>
      json = json.append(jsActor(actor))
    }
    json
  }

	def jsConformanceStatement(conformanceStatement: ConformanceStatement): JsObject = {
		val json = Json.obj(
			"actor" -> jsActor(conformanceStatement.actor),
      "specification" -> jsSpecification(conformanceStatement.specification),
			"options" -> jsOptions(conformanceStatement.options),
			"results" -> Json.obj(
				"total" -> conformanceStatement.results.total,
				"completed" -> conformanceStatement.results.completed
			)
		)
		json
	}

	def jsConformanceStatements(list: List[ConformanceStatement]): JsArray = {
		var json = Json.arr()
		list.foreach { cs =>
			json = json.append(jsConformanceStatement(cs))
		}
		json
	}

  def jsConfig(config:Config): JsObject = {
    val json = Json.obj(
      "system" -> config.system,
      "value"  -> config.value,
      "endpoint"  -> config.endpoint,
      "parameter" -> config.parameter
    )
    return json;
  }

  def jsConfigs(list:List[Config]):JsArray = {
    var json = Json.arr()
    list.foreach{ config =>
      json = json.append(jsConfig(config))
    }
    json
  }

  def parseJsConfigs(json:String):List[Config] = {
    val jsArray = Json.parse(json).as[List[JsObject]]
    var list:List[Config] = List()
    jsArray.foreach { jsonConfig =>
      list ::= Config(
        (jsonConfig \ "system").as[Long],
	      (jsonConfig \ "parameter").as[Long],
	      (jsonConfig \ "endpoint").as[Long],
	      (jsonConfig \ "value").as[String]
      )
    }
    list
  }

  def parseJsConfig(json:String):Config = {
    val jsonConfig = Json.parse(json).as[JsObject]
    Config(
      (jsonConfig \ "system").as[Long],
	    (jsonConfig \ "parameter").as[Long],
	    (jsonConfig \ "endpoint").as[Long],
	    (jsonConfig \ "value").as[String]
    )
  }

  /**
   * Converts a TestCase object into Play!'s JSON notation.
   * @param testCase TestCase object to be converted
   * @return JsObject
   */
  def jsTestCases(testCase:TestCases) : JsObject = {
    val json = Json.obj(
      "id"      -> testCase.id,
      "sname"   -> testCase.shortname,
      "fname"   -> testCase.fullname,
      "version" -> testCase.version,
      "authors" -> (if(testCase.authors.isDefined) testCase.authors.get else JsNull),
      "originalDate" -> (if(testCase.originalDate.isDefined) testCase.originalDate.get else JsNull),
      "modificationDate" -> (if(testCase.modificationDate.isDefined) testCase.modificationDate.get else JsNull),
      "description" -> (if(testCase.description.isDefined) testCase.description.get else JsNull),
      "keywords" -> (if(testCase.keywords.isDefined) testCase.keywords.get else JsNull),
      "type" -> testCase.testCaseType,
      "targetSpec"  -> testCase.targetSpec,
      "path" -> testCase.path
    )
    return json;
  }

  /**
   * Converts a TestCase object into Play!'s JSON notation.
   * @param testCase TestCase object to be converted
   * @return JsObject
   */
  def jsTestCase(testCase:TestCase) : JsObject = {
    val json = Json.obj(
      "id"      -> testCase.id,
      "sname"   -> testCase.shortname,
      "fname"   -> testCase.fullname,
      "version" -> testCase.version,
      "authors" -> (if(testCase.authors.isDefined) testCase.authors.get else JsNull),
      "originalDate" -> (if(testCase.originalDate.isDefined) testCase.originalDate.get else JsNull),
      "modificationDate" -> (if(testCase.modificationDate.isDefined) testCase.modificationDate.get else JsNull),
      "description" -> (if(testCase.description.isDefined) testCase.description.get else JsNull),
      "keywords" -> (if(testCase.keywords.isDefined) testCase.keywords.get else JsNull),
      "type" -> testCase.testCaseType,
      "path" -> testCase.path,
      "actors" -> Json.arr(if(testCase.targetActors.isDefined) testCase.targetActors.get.map(_.id) else JsNull),
      "targetSpec"  -> testCase.targetSpec
    )
    json;
  }

  /**
   * Converts a List of TestCases into Play!'s JSON notation
   * @param list List of TestCases to be converted
   * @return JsArray
   */
  def jsTestCasesList(list:List[TestCases]):JsArray = {
    var json = Json.arr()
    list.foreach{ testCase =>
      json = json.append(jsTestCases(testCase))
    }
    json
  }

  /**
   * Converts a List of TestCase into Play!'s JSON notation
   * @param list List of TestCase to be converted
   * @return JsArray
   */
  def jsTestCaseList(list:List[TestCase]):JsArray = {
    var json = Json.arr()
    list.foreach{ testCase =>
      json = json.append(jsTestCase(testCase))
    }
    json
  }

	/**
	 * Converts a Option object into Play!'s JSON notation.
	 * @param option Option object to be converted
	 * @return JsObject
	 */
	def jsOption(option:Options):JsObject = {
		val json = Json.obj(
			"id" -> option.id,
			"sname" -> option.sname,
			"fname" -> option.fname,
			"description" -> option.description,
			"actor" -> option.actor
		)
		json
	}

	/**
	 * Converts a List of Options into Play!'s JSON notation
	 * @param list List of Options to be converted
	 * @return JsArray
	 */
	def jsOptions(list:List[Options]):JsArray = {
		var json = Json.arr()
		list.foreach{ option =>
			json = json.append(jsOption(option))
		}
		json
	}

  /**
   * Converts a TestResult object into Play!'s JSON notation.
   * @param testResult TestResult object to be converted
   * @return JsObject
   */
  def jsTestResult(testResult:TestResult, returnTPL:Boolean):JsObject = {
    val json = Json.obj(
      "sessionId" -> testResult.sessionId,
      "systemId"  -> (if (testResult.systemId.isDefined) testResult.systemId else JsNull),
      "actorId"   -> (if (testResult.actorId.isDefined) testResult.actorId else JsNull),
      "testId"    -> (if (testResult.testCaseId.isDefined) testResult.testCaseId else JsNull),
      "result"    -> testResult.result,
      "startTime" -> TimeUtil.serializeTimestamp(testResult.startTime),
      "endTime"   -> (if(testResult.endTime.isDefined) TimeUtil.serializeTimestamp(testResult.endTime.get) else JsNull),
      "tpl"       -> (if(returnTPL) testResult.tpl else JsNull),
      "obsolete"  -> (if (testResult.testSuiteId.isDefined && testResult.testCaseId.isDefined && testResult.systemId.isDefined && testResult.organizationId.isDefined && testResult.communityId.isDefined && testResult.domainId.isDefined && testResult.specificationId.isDefined && testResult.actorId.isDefined) false else true)
    )
    json
  }

  def jsTestResultReports(list: List[TestResult]): JsArray = {
    var json = Json.arr()
    list.foreach { report =>
      json = json.append(jsTestResultReport(report))
    }
    json
  }

  def jsTestResultSessionReports(list: List[TestResult]): JsArray = {
    var json = Json.arr()
    list.foreach { report =>
      json = json.append(jsTestResultReport(report))
    }
    json
  }

  def jsCount(count: Long): JsObject = {
    val json = Json.obj(
      "count"    -> count
    )
    json
  }

  def jsTestResultReport(result: TestResult): JsObject = {
    val json = Json.obj(
      "result" -> jsTestResult(result, false),
      "test" ->  {
        Json.obj(
          "id"      -> (if (result.testCaseId.isDefined) result.testCaseId.get else JsNull),
          "sname"   -> (if (result.testCase.isDefined) result.testCase.get else JsNull)
        )
      },
      "organization" -> {
        Json.obj(
          "id"    -> (if (result.organizationId.isDefined) result.organizationId.get else JsNull),
          "sname" -> (if (result.organization.isDefined) result.organization.get else JsNull),
          "community" -> (if (result.communityId.isDefined) result.communityId.get else JsNull)
        )
      },
      "system" -> {
        Json.obj(
          "id"    -> (if (result.systemId.isDefined) result.systemId.get else JsNull),
          "sname" -> (if (result.system.isDefined) result.system.get else JsNull),
          "owner" -> (if (result.organizationId.isDefined) result.organizationId.get else JsNull)
        )
      },
      "actor" -> {
        Json.obj(
          "id" -> result.actorId,
          "name"   -> result.actor,
          "domain"  -> result.domainId
        )
      },
      "specification" -> {
        Json.obj(
          "id"      -> (if (result.specificationId.isDefined) result.specificationId.get else JsNull),
          "sname"   -> (if (result.specification.isDefined) result.specification.get else JsNull),
          "domain"  -> (if (result.domainId.isDefined) result.domainId.get else JsNull)
        )
      },
      "domain" -> {
        Json.obj(
          "id" -> (if (result.domainId.isDefined) result.domainId.get else JsNull),
          "sname" -> (if (result.domain.isDefined) result.domain.get else JsNull)
        )
      },
      "testSuite" -> {
        Json.obj(
          "id"                -> (if (result.testSuiteId.isDefined) result.testSuiteId.get else JsNull),
          "sname"             -> (if (result.testSuite.isDefined) result.testSuite.get else JsNull),
          "specification"     -> (if (result.specificationId.isDefined) result.specificationId.get else JsNull)
        )
      }
    )
    json
  }

  /**
   * Converts a List of TestResults into Play!'s JSON notation
   * @param list List of TestResults to be converted
   * @return JsArray
   */
  def jsTestResults(list:List[TestResult], returnTPL:Boolean):JsArray = {
    var json = Json.arr()
    list.foreach{ testResult =>
      json = json.append(jsTestResult(testResult, returnTPL))
    }
    json
  }

  /**
   * Converts a TestStepResult object into Play!'s JSON notation.
   * @param step TestStepResult object to be converted
   * @return JsObject
   */
  def jsTestStepResult(step:TestStepResult):JsObject = {
    val json = Json.obj(
      "sessionId" -> step.sessionId,
      "stepId"    -> step.stepId,
      "result"    -> step.result,
      "path"      -> step.path
    )
    json
  }

  /**
   * Converts a List of TestStepResults into Play!'s JSON notation
   * @param list List of TestStepResults to be converted
   * @return JsArray
   */
  def jsTestStepResults(list:List[TestStepResult]):JsArray = {
    var json = Json.arr()
    list.foreach{ result =>
      json = json.append(jsTestStepResult(result))
    }
    json
  }

  def jsTestResultStatuses(testCaseIds: List[Long], testResultStatuses: List[TestResultStatus.Value]): JsObject = {
    val zippedResults = testCaseIds.zip(testResultStatuses)
    var json = Json.obj()
    zippedResults.foreach { pair =>
      val (testCaseId, testResultStatus) = pair
      json = json + (testCaseId.toString, JsString(testResultStatus.toString))
    }
    json
  }

  def jsTestResultStatuses(testSuiteId: Long, testCaseIds: List[Long], testResultStatuses: List[TestResultStatus.Value]): JsObject = {
    val json = Json.obj(
      "id"    -> testSuiteId,
      "testCases" -> jsTestResultStatuses(testCaseIds, testResultStatuses)
    )
    json
  }

  /**
   * Converts a User object into a JSON string with its complex objects
   * @param user User object to be converted
   * @return String
   */
  def serializeUser(user:User):String = {
    //1) Serialize User
    var jUser:JsObject = jsUser(user.toCaseObject)
    //2) If Organization exists, convert and append it to User
    if(user.organization.isDefined){
      jUser = jUser ++ Json.obj("organization" -> jsOrganization(user.organization.get))
    }
    //3) Return JSON String
    jUser.toString
  }

  def serializeSystemConfig(sc:SystemConfiguration):String = {
    var jConfig:JsObject = jsSystemConfiguration(sc.toCaseObject)
    jConfig.toString
  }

  def serializeSpecification(spec:Specification):String = {
    var jSpec:JsObject = jsSpecification(spec.toCaseObject)
    jSpec.toString
  }

  /**
   * Converts an Organization object into a JSON string with its complex objects
   * @param org Organization object to be converted
   * @return String
   */
  def serializeOrganization(org:Organization):String = {
    //1) Serialize Organization
    var jOrganization:JsObject = jsOrganization(org.toCaseObject)
    //2) If User exists, convert and append it to Organization
    if(org.admin.isDefined){
      jOrganization = jOrganization ++ Json.obj("admin" -> jsUser(org.admin.get))
    } else{
      jOrganization = jOrganization ++ Json.obj("admin" -> JsNull)
    }
    //3) If Systems exist, convert and append them to Organization
    if(org.systems.isDefined){
      var jsSystems:JsArray = Json.arr()
      org.systems.get.foreach { system =>
        jsSystems = jsSystems.append( jsSystem(system) )
      }
      jOrganization = jOrganization ++ Json.obj("systems" -> jsSystems)
    } else{
      jOrganization = jOrganization ++ Json.obj("systems" -> JsNull)
    }
    //
    if(org.landingPageObj.isDefined){
      jOrganization = jOrganization ++ Json.obj("landingPages" -> jsLandingPage(org.landingPageObj.get))
    } else{
      jOrganization = jOrganization ++ Json.obj("landingPages" -> JsNull)
    }
    //
    if(org.legalNoticeObj.isDefined){
      jOrganization = jOrganization ++ Json.obj("legalNotices" -> jsLegalNotice(org.legalNoticeObj.get))
    } else{
      jOrganization = jOrganization ++ Json.obj("legalNotices" -> JsNull)
    }
    //
    if(org.community.isDefined){
      jOrganization = jOrganization ++ Json.obj("communities" -> jsCommunity(org.community.get))
    } else{
      jOrganization = jOrganization ++ Json.obj("communities" -> JsNull)
    }
    //4) Return JSON String
    jOrganization.toString
  }

  /**
   * Converts a System object into a JSON string with its complex objects
   * @param system System object to be converted
   * @return String
   */
  def serializeSystem(system:models.System):String = {
    //1) Serialize System
    var jSystem = jsSystem(system.toCaseObject)
    //2) If Owner exists, convert and append it to System
    if(system.owner.isDefined){
      jSystem = jSystem ++ Json.obj("owner" -> jsOrganization(system.owner.get))
    } else{
      jSystem = jSystem ++ Json.obj("owner" -> JsNull)
    }
    //3) If admins exist, convert and append them to System
    if(system.admins.isDefined) {
      var jsAdmins:JsArray = Json.arr()
      system.admins.get.foreach { user =>
        jsAdmins = jsAdmins.append( jsUser(user) )
      }
      jSystem = jSystem ++ Json.obj("admins" -> jsAdmins)
    } else{
      jSystem = jSystem ++ Json.obj("admins" -> JsNull)
    }
    //4) Return JSON String
    jSystem.toString
  }

  /**
   * Converts a LandingPage object into Play!'s JSON notation.
   * Does not support cross object conversion
   * @param landingPage LandingPage object to be converted
   * @return JsObject
   */
  def jsLandingPage(landingPage:LandingPages):JsObject = {
    val json = Json.obj(
      "id"    -> landingPage.id,
      "name"  -> landingPage.name,
      "description" -> (if(landingPage.description.isDefined) landingPage.description.get else JsNull),
      "content"  -> landingPage.content,
      "default" -> landingPage.default
    )
    json
  }

  /**
   * Converts a LandingPage object into Play!'s JSON notation.
   * Does not support cross object conversion
   * @param legalNotice LandingPage object to be converted
   * @return JsObject
   */
  def jsLegalNotice(legalNotice:LegalNotices):JsObject = {
    val json = Json.obj(
      "id"    -> legalNotice.id,
      "name"  -> legalNotice.name,
      "description" -> (if(legalNotice.description.isDefined) legalNotice.description.get else JsNull),
      "content"  -> legalNotice.content,
      "default" -> legalNotice.default
    )
    json
  }

  /**
   * Converts a List of LandingPages into Play!'s JSON notation
   * Does not support cross object conversion
   * @param list List of LandingPages to be convert
   * @return JsArray
   */
  def jsLandingPages(list:List[LandingPages]):JsArray = {
    var json = Json.arr()
    list.foreach{ landingPage =>
      json = json.append(jsLandingPage(landingPage))
    }
    json
  }

  /**
   * Converts a List of LegalNotices into Play!'s JSON notation
   * Does not support cross object conversion
   * @param list List of LegalNotices to be convert
   * @return JsArray
   */
  def jsLegalNotices(list:List[LegalNotices]):JsArray = {
    var json = Json.arr()
    list.foreach{ ln =>
      json = json.append(jsLegalNotice(ln))
    }
    json
  }

  /**
   * Converts a LandingPage object into a JSON string with its complex objects
   * @param landingPage LandingPage object to be converted
   * @return String
   */
  def serializeLandingPage(landingPage:LandingPage):String = {
    //1) Serialize LandingPage
    val exists = landingPage != null
    var jLandingPage:JsObject =  jsExists(exists)
    if (exists) {
      jLandingPage = jLandingPage ++ jsLandingPage(landingPage.toCaseObject)
    }
    //3) Return JSON String
    jLandingPage.toString
  }

  /**
   * Converts a LegalNotice object into a JSON string with its complex objects
   * @param legalNotice LegalNotice object to be converted
   * @return String
   */
  def serializeLegalNotice(legalNotice:LegalNotice):String = {
    //1) Serialize LandingPage
    val exists = legalNotice != null
    var jLegalNotice:JsObject = jsExists(exists)
    if (exists) {
      jLegalNotice = jLegalNotice ++ jsLegalNotice(legalNotice.toCaseObject)
    }
    //3) Return JSON String
    jLegalNotice.toString
  }

  def jsExists(bool:Boolean):JsObject = {
    val json = Json.obj(
      "exists" -> bool
    )
    json
  }

  def jsTestSuiteUploadItemResult(item: TestSuiteUploadItemResult):JsObject = {
    val json = Json.obj(
      "name" -> item.itemName,
      "type" -> item.itemType,
      "action" -> item.actionType
    )
    json
  }

  def jsTestSuiteUploadItemResults(items: util.ArrayList[TestSuiteUploadItemResult]):JsArray = {
    var json = Json.arr()
    for (item <- items.asScala) {
      json = json.append(jsTestSuiteUploadItemResult(item))
    }
    json
  }

  def jsTestSuiteUploadResult(result: TestSuiteUploadResult):JsObject = {
    val json = Json.obj(
      "success"    -> result.success,
      "errorInformation"  -> result.errorInformation,
      "pendingFolderId"  -> result.pendingTestSuiteFolderName,
      "items" -> jsTestSuiteUploadItemResults(result.items)
    )
    json
  }

}