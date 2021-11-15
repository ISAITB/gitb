package utils

import com.gitb.core.ValueEmbeddingEnumeration
import com.gitb.tbs.UserInput
import com.gitb.tr._
import config.Configurations
import managers.export.{ExportSettings, ImportItem, ImportSettings}
import models.Enums.TestSuiteReplacementChoice.TestSuiteReplacementChoice
import models.Enums.TestSuiteReplacementChoiceHistory.TestSuiteReplacementChoiceHistory
import models.Enums.TestSuiteReplacementChoiceMetadata.TestSuiteReplacementChoiceMetadata
import models.Enums._
import models._
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{JsObject, _}

import java.sql.Timestamp
import java.util
import javax.xml.bind.JAXBElement
import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}
import scala.jdk.CollectionConverters.CollectionHasAsScala

object JsonUtil {

  def jsTextArray(texts: List[String]): JsObject = {
    var textArray = Json.arr()
    texts.foreach { text =>
      textArray = textArray.append(JsString(text))
    }
    val json = Json.obj(
      "texts" -> textArray
    )
    json
  }

  def jsImportPreviewResult(pendingImportId: String, importItems: Iterable[ImportItem]): JsObject = {
    val json = Json.obj(
      "pendingImportId" -> pendingImportId,
      "importItems" -> jsImportItems(importItems)
    )
    json
  }

  def jsImportItems(importItems: Iterable[ImportItem]): JsArray = {
    var json = Json.arr()
    importItems.foreach{ importItem =>
      json = json.append(jsImportItem(importItem))
    }
    json
  }

  def jsImportItem(importItem: ImportItem): JsObject = {
    val json = Json.obj(
      "name" -> (if(importItem.itemName.isDefined) importItem.itemName.get else JsNull),
      "type" -> importItem.itemType.id,
      "match" -> importItem.itemMatch.id,
      "target" -> (if(importItem.targetKey.isDefined) importItem.targetKey.get else JsNull),
      "source" -> (if(importItem.sourceKey.isDefined) importItem.sourceKey.get else JsNull),
      "children" -> jsImportItems(importItem.childrenItems)
    )
    json
  }

  def jsTestSuite(suite: TestSuites, withDocumentation: Boolean): JsObject = {
    val json = Json.obj(
      "id"                -> suite.id,
      "identifier"        -> suite.identifier,
      "sname"             -> suite.shortname,
      "fname"             -> suite.fullname,
      "version"           -> suite.version,
      "specification"     -> suite.specification,
      "authors"           -> (if(suite.authors.isDefined) suite.authors.get else JsNull),
      "description"       -> (if(suite.description.isDefined) suite.description.get else JsNull),
      "keywords"          -> (if(suite.keywords.isDefined) suite.keywords.get else JsNull),
      "modificationDate"  -> (if(suite.modificationDate.isDefined) suite.modificationDate.get else JsNull),
      "originalDate"      -> (if(suite.originalDate.isDefined) suite.originalDate.get else JsNull),
      "hasDocumentation"  -> suite.hasDocumentation,
      "documentation"     -> (if(withDocumentation && suite.documentation.isDefined) suite.documentation else JsNull),
    )
    json
  }

  def jsSystemConfigurationEndpoints(endpoints: List[SystemConfigurationEndpoint], addValues: Boolean, isAdmin: Boolean): JsArray = {
    var json = Json.arr()
    endpoints.foreach{ endpoint =>
      json = json.append(jsSystemConfigurationEndpoint(endpoint, addValues, isAdmin))
    }
    json
  }

  def jsSystemConfigurationEndpoint(endpoint: SystemConfigurationEndpoint, addValues: Boolean, isAdmin: Boolean): JsObject = {
    val json = Json.obj(
      "id" -> endpoint.endpoint.id,
      "name" -> endpoint.endpoint.name,
      "description" -> (if(endpoint.endpoint.desc.isDefined) endpoint.endpoint.desc.get else JsNull),
      "parameters" -> jsSystemConfigurationParameters(endpoint.endpointParameters, addValues, isAdmin)
    )
    json
  }

  def jsSystemConfigurationParameters(parameters: Option[List[SystemConfigurationParameter]], addValues: Boolean, isAdmin: Boolean): JsArray = {
    var json = Json.arr()
    if (parameters.isDefined) {
      parameters.get.foreach{ parameter =>
        json = json.append(jsSystemConfigurationParameter(parameter, addValues, isAdmin))
      }
    }
    json
  }

  def jsSystemConfigurationParameter(parameter: SystemConfigurationParameter, addValues: Boolean, isAdmin: Boolean): JsObject = {
    var json = jsParameter(parameter.parameter)
    json = json.+("configured" -> JsBoolean(parameter.configured))
    if (addValues && parameter.config.isDefined) {
      if (parameter.parameter.kind != "SECRET") {
        json = json.+("value" -> JsString(parameter.config.get.value))
        if (parameter.parameter.kind == "BINARY") {
          if (parameter.config.get.contentType.isDefined) {
            json = json.+("mimeType" -> JsString(parameter.config.get.contentType.get))
          }
        }
      }
    }
    json
  }

  def jsTestSuitesList(list: List[TestSuites]): JsArray = {
    var json = Json.arr()
    list.foreach { testSuite =>
      json = json.append(jsTestSuite(testSuite, withDocumentation = false))
    }
    json
  }

  def jsTestSuite(testSuite: TestSuite, withDocumentation: Boolean): JsObject = {
    var jTestSuite: JsObject = jsTestSuite(testSuite.toCaseObject, withDocumentation)
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
      json = json.append(jsTestSuite(testSuite, withDocumentation = false))
    }
    json
  }

  def jsEndpoint(endpoint: Endpoint): JsObject = {
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

	def jsEndpoints(list: List[Endpoint]): JsArray = {
		var json = Json.arr()
		list.foreach { endpoint =>
			json = json.append(jsEndpoint(endpoint))
		}
		json
	}

	def jsParameter(parameter: Parameters): JsObject = {
		val json = Json.obj(
			"id" -> parameter.id,
			"name" -> parameter.name,
			"desc" -> (if (parameter.desc.isDefined) parameter.desc.get else JsNull),
			"use" -> parameter.use,
			"kind" -> parameter.kind,
      "adminOnly" -> parameter.adminOnly,
      "notForTests" -> parameter.notForTests,
      "hidden" -> parameter.hidden,
      "allowedValues" -> (if (parameter.allowedValues.isDefined) parameter.allowedValues.get else JsNull),
      "dependsOn" -> (if (parameter.dependsOn.isDefined) parameter.dependsOn.get else JsNull),
      "dependsOnValue" -> (if (parameter.dependsOnValue.isDefined) parameter.dependsOnValue.get else JsNull),
			"endpoint" -> parameter.endpoint
		)
		json
	}

	def jsParameters(list: List[Parameters]): JsArray = {
		var json = Json.arr()
		list.foreach { parameter =>
			json = json.append(jsParameter(parameter))
		}
		json
	}

  def jsOrganisationParameters(list: List[OrganisationParameters]): JsArray = {
    var json = Json.arr()
    list.foreach { parameter =>
      json = json.append(jsOrganisationParameter(parameter))
    }
    json
  }

  def jsOrganisationParameter(parameter: OrganisationParameters): JsObject = {
    val json = Json.obj(
      "id" -> parameter.id,
      "name" -> parameter.name,
      "testKey" -> parameter.testKey,
      "desc" -> (if (parameter.description.isDefined) parameter.description.get else JsNull),
      "use" -> parameter.use,
      "kind" -> parameter.kind,
      "adminOnly" -> parameter.adminOnly,
      "notForTests" -> parameter.notForTests,
      "inExports" -> parameter.inExports,
      "inSelfRegistration" -> parameter.inSelfRegistration,
      "hidden" -> parameter.hidden,
      "allowedValues" -> (if (parameter.allowedValues.isDefined) parameter.allowedValues.get else JsNull),
      "dependsOn" -> (if (parameter.dependsOn.isDefined) parameter.dependsOn.get else JsNull),
      "dependsOnValue" -> (if (parameter.dependsOnValue.isDefined) parameter.dependsOnValue.get else JsNull),
      "community" -> parameter.community
    )
    json
  }

  def jsSystemParameters(list: List[SystemParameters]): JsArray = {
    var json = Json.arr()
    list.foreach { parameter =>
      json = json.append(jsSystemParameter(parameter))
    }
    json
  }

  def jsSystemParameter(parameter: SystemParameters): JsObject = {
    val json = Json.obj(
      "id" -> parameter.id,
      "name" -> parameter.name,
      "testKey" -> parameter.testKey,
      "desc" -> (if (parameter.description.isDefined) parameter.description.get else JsNull),
      "use" -> parameter.use,
      "kind" -> parameter.kind,
      "adminOnly" -> parameter.adminOnly,
      "notForTests" -> parameter.notForTests,
      "inExports" -> parameter.inExports,
      "hidden" -> parameter.hidden,
      "allowedValues" -> (if (parameter.allowedValues.isDefined) parameter.allowedValues.get else JsNull),
      "dependsOn" -> (if (parameter.dependsOn.isDefined) parameter.dependsOn.get else JsNull),
      "dependsOnValue" -> (if (parameter.dependsOnValue.isDefined) parameter.dependsOnValue.get else JsNull),
      "community" -> parameter.community
    )
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
      "name"  -> (
        if (Configurations.AUTHENTICATION_SSO_ENABLED) {
          if (user.ssoStatus == Enums.UserSSOStatus.NotLinked.id.toShort) {
            JsNull
          } else {
            user.name
          }
        } else {
          user.name
        }
      ),
      "email" -> (
        if (Configurations.AUTHENTICATION_SSO_ENABLED) {
          if (user.ssoStatus == Enums.UserSSOStatus.NotMigrated.id.toShort) {
            user.email
          } else {
            user.ssoEmail
          }
        } else {
          user.email
        }),
      "role"  -> user.role,
      "onetime" -> (
        if (Configurations.AUTHENTICATION_SSO_ENABLED) {
          false
        } else {
          user.onetimePassword
        }
      ),
      "ssoStatus" -> user.ssoStatus
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

  def jsUserAccounts(list:List[UserAccount]):JsArray = {
    var json = Json.arr()
    list.foreach{ userAccount =>
      json = json.append(jsUserAccount(userAccount))
    }
    json
  }

  def jsUserAccount(userAccount:UserAccount):JsObject = {
    val json = Json.obj(
      "id"    -> userAccount.user.id,
      "name"  -> userAccount.user.name,
      "email" -> userAccount.user.email,
      "role"  -> userAccount.user.role,
      "organisationId" -> userAccount.organisation.id,
      "organisationShortName" -> userAccount.organisation.shortname,
      "organisationFullName" -> userAccount.organisation.fullname,
      "organisationIsAdmin" -> userAccount.organisation.adminOrganization,
      "communityId" -> userAccount.community.id,
      "communityShortName" -> userAccount.community.shortname,
      "communityFullName" -> userAccount.community.fullname
    )
    json
  }

  def jsActualUserInfo(userInfo: ActualUserInfo):JsObject = {
    val json = Json.obj(
      "uid" -> userInfo.uid,
      "email" -> userInfo.email,
      "firstName" -> userInfo.firstName,
      "lastName" -> userInfo.lastName,
      "accounts" -> jsUserAccounts(userInfo.accounts)
    )
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
      "errorTemplate" -> (if(organization.errorTemplate.isDefined) organization.errorTemplate.get else JsNull),
      "template" -> organization.template,
      "templateName" -> (if(organization.templateName.isDefined) organization.templateName.get else JsNull),
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

  def jsOrganizationSearchResults(list: Iterable[Organizations], resultCount: Int): JsObject = {
    var json = Json.arr()
    list.foreach { organisation =>
      json = json.append(jsOrganization(organisation))
    }
    val jsonResult = Json.obj(
      "data" -> json,
      "count" -> resultCount
    )
    jsonResult
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
    jsSystems(list, None)
  }

  def jsSystems(list:List[Systems], systemsWithTests: Option[Set[Long]]):JsArray = {
    var json = Json.arr()
    list.foreach{ system =>
      var systemJson = jsSystem(system)
      if (systemsWithTests.isDefined) {
        systemJson = systemJson + ("hasTests" -> JsBoolean(systemsWithTests.get.contains(system.id)))
      }
      json = json.append(systemJson)
    }
    json
  }

  def jsBinaryMetadata(mimeType: String, extension: String):JsObject = {
    val json = Json.obj(
      "mimeType"    -> mimeType,
      "extension" -> extension
    )
    json
  }

  def jsCommunities(list:List[Communities]):JsArray = {
    var json = Json.arr()
    list.foreach{ community =>
      json = json.append(jsCommunity(community, includeAdminInfo = true))
    }
    json
  }

  def jsCommunity(community:Communities, includeAdminInfo: Boolean):JsObject = {
    var json = Json.obj(
      "id"    -> community.id,
      "sname" -> community.shortname,
      "fname" -> community.fullname,
      "allowCertificateDownload" -> community.allowCertificateDownload,
      "allowStatementManagement" -> community.allowStatementManagement,
      "allowSystemManagement" -> community.allowSystemManagement,
      "allowPostTestOrganisationUpdates" -> community.allowPostTestOrganisationUpdates,
      "allowPostTestSystemUpdates" -> community.allowPostTestSystemUpdates,
      "allowPostTestStatementUpdates" -> community.allowPostTestStatementUpdates,
      "domainId" -> community.domain
    )
    if (includeAdminInfo) {
      json = json.+("email" -> (if (community.supportEmail.isDefined) JsString(community.supportEmail.get) else JsNull))
      json = json.+("selfRegType" -> JsNumber(community.selfRegType))
      json = json.+("selfRegRestriction" -> JsNumber(community.selfRegRestriction))
      json = json.+("selfRegToken" -> (if(community.selfRegToken.isDefined) JsString(community.selfRegToken.get) else JsNull))
      json = json.+("selfRegTokenHelpText" -> (if(community.selfRegTokenHelpText.isDefined) JsString(community.selfRegTokenHelpText.get) else JsNull))
      json = json.+("selfRegNotification" -> JsBoolean(community.selfregNotification))
      json = json.+("selfRegForceTemplateSelection" -> JsBoolean(community.selfRegForceTemplateSelection))
      json = json.+("selfRegForceRequiredProperties" -> JsBoolean(community.selfRegForceRequiredProperties))
      json = json.+("description" -> (if(community.description.isDefined) JsString(community.description.get) else JsNull))
    }
    json
  }

  def serializeCommunity(community:Community, labels: Option[List[CommunityLabels]], includeAdminInfo: Boolean):String = {
    var jCommunity:JsObject = jsCommunity(community.toCaseObject, includeAdminInfo)
    if(community.domain.isDefined){
      jCommunity = jCommunity ++ Json.obj("domain" -> jsDomain(community.domain.get))
    } else{
      jCommunity = jCommunity ++ Json.obj("domain" -> JsNull)
    }
    if (labels.isDefined) {
      jCommunity = jCommunity ++ Json.obj("labels" -> jsCommunityLabels(labels.get))
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

  def jsDomainParameters(list:List[DomainParameter]):JsArray = {
    var json = Json.arr()
    list.foreach{ parameter =>
      json = json.append(jsDomainParameter(parameter))
    }
    json
  }

  def jsDomainParameter(domainParameter:DomainParameter):JsObject = {
    var valueToUse = ""
    if (domainParameter.kind == "SIMPLE" && domainParameter.value.isDefined) {
      valueToUse = domainParameter.value.get
    }
    val json = Json.obj(
      "id" -> domainParameter.id,
      "name" -> domainParameter.name,
      "description" -> domainParameter.desc,
      "kind" -> domainParameter.kind,
      "inTests" -> domainParameter.inTests,
      "contentType" -> (if(domainParameter.contentType.isDefined) JsString(domainParameter.contentType.get) else JsNull),
      "value" -> valueToUse
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
      "description" -> (if(spec.description.isDefined) spec.description.get else JsNull),
      "hidden" -> spec.hidden,
      "domain"  -> spec.domain
    )
    json
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
      "default" -> (if(actor.default.isDefined) actor.default.get else JsNull),
      "hidden" -> actor.hidden,
      "displayOrder" -> (if(actor.displayOrder.isDefined) actor.displayOrder.get else JsNull),
      "domain"  -> actor.domain
    )
    json
  }

  def jsActor(actor:Actor) : JsObject = {
    val json = Json.obj(
      "id" -> actor.id,
      "actorId" -> actor.actorId,
      "name"   -> actor.name,
      "description" -> (if(actor.description.isDefined) actor.description.get else JsNull),
      "default" -> (if(actor.default.isDefined) actor.default.get else JsNull),
      "hidden" -> actor.hidden,
      "displayOrder" -> (if(actor.displayOrder.isDefined) actor.displayOrder.get else JsNull),
      "domain"  -> (if(actor.domain.isDefined) actor.domain.get.id else JsNull),
      "specification"  -> (if(actor.specificationId.isDefined) actor.specificationId.get else JsNull)
    )
    json
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

  def jsActorsNonCase(list:List[Actor]):JsArray = {
    var json = Json.arr()
    list.foreach{ actor =>
      json = json.append(jsActor(actor))
    }
    json
  }

	def jsConformanceStatement(conformanceStatement: ConformanceStatement): JsObject = {
		val json = Json.obj(
      "domainId" -> conformanceStatement.domainId,
      "domain" -> conformanceStatement.domainName,
      "domainFull" -> conformanceStatement.domainNameFull,
      "actorId" -> conformanceStatement.actorId,
			"actor" -> conformanceStatement.actorName,
      "actorFull" -> conformanceStatement.actorFull,
      "specificationId" -> conformanceStatement.specificationId,
      "specification" -> conformanceStatement.specificationName,
      "specificationFull" -> conformanceStatement.specificationNameFull,
      "updateTime" -> (if(conformanceStatement.updateTime.isDefined) TimeUtil.serializeTimestamp(conformanceStatement.updateTime.get) else JsNull),
			"results" -> Json.obj(
				"undefined" -> conformanceStatement.undefinedTests,
        "failed" -> conformanceStatement.failedTests,
				"completed" -> conformanceStatement.completedTests
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

  def jsConfig(config:Configs): JsObject = {
    val json = Json.obj(
      "system" -> config.system,
      "value"  -> config.value,
      "endpoint"  -> config.endpoint,
      "parameter" -> config.parameter
    )
    json
  }

  def jsConfig(config:Config): JsObject = {
    val json = Json.obj(
      "system" -> config.system,
      "value"  -> config.value,
      "endpoint"  -> config.endpoint,
      "parameter" -> config.parameter,
      "mimeType" -> config.mimeType,
      "extension" -> config.extension
    )
    json
  }

  def jsConfigList(list:List[Config]):JsArray = {
    var json = Json.arr()
    list.foreach{ config =>
      json = json.append(jsConfig(config))
    }
    json
  }

  def jsConfigs(list:List[Configs]):JsArray = {
    var json = Json.arr()
    list.foreach{ config =>
      json = json.append(jsConfig(config))
    }
    json
  }

  def parseJsOrganisationParameterValues(json:String):List[OrganisationParameterValues] = {
    val jsArray = Json.parse(json).as[JsArray].value
    var list:List[OrganisationParameterValues] = List()
    jsArray.foreach { jsonConfig =>
      val value = (jsonConfig \ "value").asOpt[String]
      list ::= OrganisationParameterValues(
        -1, // Will be forced later
        (jsonConfig \ "parameter").as[Long],
        value.get,
        None
      )
    }
    list
  }

  def parseJsPendingTestSuiteActions(json: String): List[PendingTestSuiteAction] = {
    val jsArray = Json.parse(json).as[JsArray].value
    val values = new ListBuffer[PendingTestSuiteAction]()
    jsArray.foreach { jsonConfig =>
      val pendingActionStr = (jsonConfig \ "action").as[String]
      var pendingAction: TestSuiteReplacementChoice = null
      var pendingActionHistory: Option[TestSuiteReplacementChoiceHistory] = None
      var pendingActionMetadata: Option[TestSuiteReplacementChoiceMetadata] = None
      if ("proceed".equals(pendingActionStr)) {
        pendingAction = TestSuiteReplacementChoice.PROCEED
        val pendingActionHistoryStr = (jsonConfig \ "pending_action_history").asOpt[String]
        if (pendingActionHistoryStr.isDefined && "drop".equals(pendingActionHistoryStr.get)) {
          // Drop testing history.
          pendingActionHistory = Some(TestSuiteReplacementChoiceHistory.DROP)
        } else {
          // Keep testing history.
          pendingActionHistory = Some(TestSuiteReplacementChoiceHistory.KEEP)
        }
        val pendingActionMetadataStr = (jsonConfig \ "pending_action_metadata").asOpt[String]
        if (pendingActionMetadataStr.isDefined && "update".equals(pendingActionMetadataStr.get)) {
          // Use metadata from archive.
          pendingActionMetadata = Some(TestSuiteReplacementChoiceMetadata.UPDATE)
        } else {
          // Skip metadata from archive.
          pendingActionMetadata = Some(TestSuiteReplacementChoiceMetadata.SKIP)
        }
      } else {
        // Cancel
        pendingAction = TestSuiteReplacementChoice.CANCEL
      }
      values += new PendingTestSuiteAction(
        (jsonConfig \ "specification").as[Long],
        pendingAction,
        pendingActionHistory,
        pendingActionMetadata
      )
    }
    values.toList
  }

  def parseJsSystemParameterValues(json:String):List[SystemParameterValues] = {
    val jsArray = Json.parse(json).as[JsArray].value
    var list:List[SystemParameterValues] = List()
    jsArray.foreach { jsonConfig =>
      val value = (jsonConfig \ "value").asOpt[String]
      list ::= SystemParameterValues(
        -1, // Will be forced later
        (jsonConfig \ "parameter").as[Long],
        value.get,
        None
      )
    }
    list
  }

  private def parseJsImportItem(jsonConfig: JsValue, parentItem: Option[ImportItem]): ImportItem = {
    val item = new ImportItem(
      (jsonConfig \ "name").asOpt[String],
      ImportItemType.apply((jsonConfig \ "type").as[Short]),
      ImportItemMatch.apply((jsonConfig \ "match").as[Short]),
      (jsonConfig \ "target").asOpt[String],
      (jsonConfig \ "source").asOpt[String],
      ImportItemChoice.apply((jsonConfig \ "process").as[Short])
    )
    item.parentItem = parentItem
    val children = (jsonConfig \ "children").asOpt[JsArray]
    if (children.isDefined) {
      children.get.value.foreach { childObj =>
        item.childrenItems += parseJsImportItem(childObj, Some(item))
      }
    }
    item
  }

  def parseJsTriggerDataItems(json:String, triggerId: Option[Long]): List[TriggerData] = {
    val jsArray = Json.parse(json).as[JsArray].value
    val list = ListBuffer[TriggerData]()
    jsArray.foreach { jsonConfig =>
      list += parseJsTriggerDataItem(jsonConfig, triggerId)
    }
    list.toList
  }

  private def parseJsTriggerDataItem(jsonConfig: JsValue, triggerId: Option[Long]): TriggerData = {
    val dataType = (jsonConfig \ "dataType").as[Short]
    val dataTypeEnum = TriggerDataType.apply(dataType)
    var dataIdToUse: Long = -1
    if (dataTypeEnum == TriggerDataType.OrganisationParameter || dataTypeEnum == TriggerDataType.SystemParameter || dataTypeEnum == TriggerDataType.DomainParameter) {
      dataIdToUse = (jsonConfig \ "dataId").as[Long]
    }
    val data = TriggerData(
      dataType,
      dataIdToUse,
      triggerId.getOrElse(0L)
    )
    // Check that this is a valid value (otherwise throw exception)
    TriggerDataType.apply(data.dataType)
    data
  }

  def parseJsImportItems(json:String): List[ImportItem] = {
    val jsArray = Json.parse(json).as[JsArray].value
    val list = ListBuffer[ImportItem]()
    jsArray.foreach { jsonConfig =>
      list += parseJsImportItem(jsonConfig, None)
    }
    list.toList
  }

  def parseStringArray(json: String): List[String] = {
    Json.parse(json).as[JsArray].value.iterator.map { value =>
      value.as[JsString].value
    }.toList
  }

  def parseJsImportSettings(json:String):ImportSettings = {
    val jsonConfig = Json.parse(json).as[JsObject]
    val settings = new ImportSettings()
    settings.encryptionKey = (jsonConfig \ "encryptionKey").asOpt[String]
    settings
  }

  def parseJsIdToValuesMap(json: Option[String]): Option[immutable.Map[Long, Set[String]]] = {
    var result: Option[immutable.Map[Long, Set[String]]] = None
    if (json.isDefined) {
      val jsArray = Json.parse(json.get).as[JsArray].value
      val tempMap = new mutable.HashMap[Long, mutable.HashSet[String]]()
      jsArray.foreach { jsonObj =>
        val id = (jsonObj \ "id").as[Long]
        val value = (jsonObj \ "value").as[String]
        val idValues = tempMap.getOrElseUpdate(id, new mutable.HashSet[String]())
        idValues += value
      }
      result = Some(tempMap.map(x => x._1 -> x._2.toSet).iterator.toMap)
    }
    result
  }

  def parseJsExportSettings(json:String):ExportSettings = {
    val jsonConfig = Json.parse(json).as[JsObject]
    val settings = new ExportSettings()
    settings.communityAdministrators = (jsonConfig \ "communityAdministrators").as[Boolean]
    settings.landingPages = (jsonConfig \ "landingPages").as[Boolean]
    settings.errorTemplates = (jsonConfig \ "errorTemplates").as[Boolean]
    settings.legalNotices = (jsonConfig \ "legalNotices").as[Boolean]
    settings.triggers = (jsonConfig \ "triggers").as[Boolean]
    settings.certificateSettings = (jsonConfig \ "certificateSettings").as[Boolean]
    settings.customLabels = (jsonConfig \ "customLabels").as[Boolean]
    settings.customProperties = (jsonConfig \ "customProperties").as[Boolean]
    settings.organisations = (jsonConfig \ "organisations").as[Boolean]
    settings.organisationUsers = (jsonConfig \ "organisationUsers").as[Boolean]
    settings.organisationPropertyValues = (jsonConfig \ "organisationPropertyValues").as[Boolean]
    settings.systems = (jsonConfig \ "systems").as[Boolean]
    settings.systemPropertyValues = (jsonConfig \ "systemPropertyValues").as[Boolean]
    settings.statements = (jsonConfig \ "statements").as[Boolean]
    settings.statementConfigurations = (jsonConfig \ "statementConfigurations").as[Boolean]
    settings.domain = (jsonConfig \ "domain").as[Boolean]
    settings.domainParameters = (jsonConfig \ "domainParameters").as[Boolean]
    settings.specifications = (jsonConfig \ "specifications").as[Boolean]
    settings.actors = (jsonConfig \ "actors").as[Boolean]
    settings.endpoints = (jsonConfig \ "endpoints").as[Boolean]
    settings.testSuites = (jsonConfig \ "testSuites").as[Boolean]
    settings.encryptionKey = (jsonConfig \ "encryptionKey").asOpt[String]
    settings
  }

  def parseJsCommunityLabels(communityId: Long, json:String):List[CommunityLabels] = {
    val jsArray = Json.parse(json).as[JsArray].value
    var list:List[CommunityLabels] = List()
    jsArray.foreach { jsonConfig =>
      list ::= CommunityLabels(
        communityId,
        LabelType.apply((jsonConfig \ "labelType").as[Int]).id.toShort,
        (jsonConfig \ "singularForm").as[String],
        (jsonConfig \ "pluralForm").as[String],
        (jsonConfig \ "fixedCase").as[Boolean]
      )
    }
    list
  }

  private def stringContentToByteArray(stringContent: String): Array[Byte] = {
    if (stringContent.startsWith("data:")) {
      // Data URL
      Base64.decodeBase64(stringContent.substring(stringContent.indexOf(",") + 1))
    } else {
      Base64.decodeBase64(stringContent)
    }
  }

  def parseJsConfig(json:String):Configs = {
    val jsonConfig = Json.parse(json).as[JsObject]
    val value = (jsonConfig \ "value").asOpt[String]
    Configs(
      (jsonConfig \ "system").as[Long],
	    (jsonConfig \ "parameter").as[Long],
	    (jsonConfig \ "endpoint").as[Long],
      value.get,
      None
    )
  }

  def parseJsUserInputs(json:String): List[UserInput] = {
    val jsArray = Json.parse(json).as[JsArray].value
    var list:List[UserInput] = List()
    jsArray.foreach { jsonInput =>
      val input = new UserInput()
      input.setId((jsonInput \ "id").as[String])
      input.setName((jsonInput \ "name").as[String])
      input.setType((jsonInput \ "type").as[String])
      input.setEmbeddingMethod(ValueEmbeddingEnumeration.fromValue((jsonInput \ "embeddingMethod").as[String]))
      val inputValue = (jsonInput \ "value").asOpt[String]
      input.setValue(inputValue.orNull)
      list ::= input
    }
    list
  }

  def parseJsDomainParameter(json:String, domainParameterId: Option[Long], domainId: Long): DomainParameter = {
    val jsonConfig = Json.parse(json).as[JsObject]
    var idToUse = 0L
    if (domainParameterId.isDefined) {
      idToUse = domainParameterId.get
    }
    val kind = (jsonConfig \ "kind").as[String]
    var value: Option[String] = None
    var contentType: Option[String] = None
    if (kind.equals("HIDDEN")) {
      value = (jsonConfig \ "value").asOpt[String]
      if (value.isDefined) {
        value = Some(MimeUtil.encryptString(value.get))
      }
    } else if (kind.equals("SIMPLE")) {
      value = (jsonConfig \ "value").asOpt[String]
    } else {
      value = Some("")
      contentType = (jsonConfig \ "contentType").asOpt[String]
    }
    DomainParameter(
      idToUse,
      (jsonConfig \ "name").as[String],
      (jsonConfig \ "desc").asOpt[String],
      kind,
      value,
      (jsonConfig \ "inTests").as[Boolean],
      contentType,
      domainId
    )
  }

  def parseJsConformanceCertificateSettings(json:String, communityId: Long, keystoreData: Option[String]): ConformanceCertificates = {
    val jsonConfig = Json.parse(json).as[JsObject]
    // Ensure the message content (if provided) is correctly sanitized.
    var certificateMessage = (jsonConfig \ "message").asOpt[String]
    if (certificateMessage.isDefined) {
      certificateMessage = Some(HtmlUtil.sanitizePdfContent(certificateMessage.get))
    }
    ConformanceCertificates(
      0L,
      (jsonConfig \ "title").asOpt[String],
      certificateMessage,
      (jsonConfig \ "includeMessage").as[Boolean],
      (jsonConfig \ "includeTestStatus").as[Boolean],
      (jsonConfig \ "includeTestCases").as[Boolean],
      (jsonConfig \ "includeDetails").as[Boolean],
      (jsonConfig \ "includeSignature").as[Boolean],
      keystoreData,
      (jsonConfig \ "keystoreType").asOpt[String],
      (jsonConfig \ "keystorePassword").asOpt[String],
      (jsonConfig \ "keyPassword").asOpt[String],
      communityId
    )
  }

  def parseJsConformanceCertificateSettingsForKeystoreTest(json:String, communityId: Long): ConformanceCertificates = {
    val jsonConfig = Json.parse(json).as[JsObject]
    ConformanceCertificates(
      0L,
      None,
      None,
      includeMessage = false,
      includeTestStatus = false,
      includeTestCases = false,
      includeDetails = false,
      includeSignature = false,
      None,
      (jsonConfig \ "keystoreType").asOpt[String],
      (jsonConfig \ "keystorePassword").asOpt[String],
      (jsonConfig \ "keyPassword").asOpt[String],
      communityId
    )
  }

  /**
   * Converts a TestCase object into Play!'s JSON notation.
   * @param testCase TestCase object to be converted
   * @return JsObject
   */
  def jsTestCases(testCase:TestCases, withDocumentation: Boolean) : JsObject = {
    val json = Json.obj(
      "id"      -> testCase.id,
      "identifier"    -> testCase.identifier,
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
      "path" -> testCase.path,
      "hasDocumentation" -> testCase.hasDocumentation,
      "documentation" -> (if (withDocumentation && testCase.documentation.isDefined) testCase.documentation.get else JsNull)
    )
    json
  }

  /**
   * Converts a TestCase object into Play!'s JSON notation.
   * @param testCase TestCase object to be converted
   * @return JsObject
   */
  def jsTestCase(testCase:TestCase) : JsObject = {
    val json = Json.obj(
      "id"      -> testCase.id,
      "identifier"    -> testCase.identifier,
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
      "targetSpec"  -> testCase.targetSpec,
      "hasDocumentation"  -> testCase.hasDocumentation
    )
    json
  }

  /**
   * Converts a List of TestCases into Play!'s JSON notation
   * @param list List of TestCases to be converted
   * @return JsArray
   */
  def jsTestCasesList(list:List[TestCases]):JsArray = {
    var json = Json.arr()
    list.foreach{ testCase =>
      json = json.append(jsTestCases(testCase, withDocumentation = false))
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
  def jsTestResult(testResult:TestResult, withExtendedInformation:Boolean):JsObject = {
    val json = Json.obj(
      "sessionId" -> testResult.sessionId,
      "systemId"  -> (if (testResult.systemId.isDefined) testResult.systemId else JsNull),
      "actorId"   -> (if (testResult.actorId.isDefined) testResult.actorId else JsNull),
      "testId"    -> (if (testResult.testCaseId.isDefined) testResult.testCaseId else JsNull),
      "specificationId"    -> (if (testResult.specificationId.isDefined) testResult.specificationId else JsNull),
      "result"    -> testResult.result,
      "startTime" -> TimeUtil.serializeTimestamp(testResult.startTime),
      "endTime"   -> (if(testResult.endTime.isDefined) TimeUtil.serializeTimestamp(testResult.endTime.get) else JsNull),
      "tpl"       -> (if(withExtendedInformation) testResult.tpl else JsNull),
      "outputMessage" -> (if (withExtendedInformation && testResult.outputMessage.isDefined) testResult.outputMessage.get else JsNull),
      "obsolete"  -> (if (testResult.testSuiteId.isDefined && testResult.testCaseId.isDefined && testResult.systemId.isDefined && testResult.organizationId.isDefined && testResult.communityId.isDefined && testResult.domainId.isDefined && testResult.specificationId.isDefined && testResult.actorId.isDefined) false else true)
    )
    json
  }

  def jsTestResultReports(list: Iterable[TestResult], resultCount: Option[Int]): JsObject = {
    var json = Json.arr()
    list.foreach { report =>
      json = json.append(jsTestResultReport(report, None, None, None, None))
    }
    val jsonResult = Json.obj(
      "data" -> json,
      "count" -> (if (resultCount.isDefined) resultCount.get else JsNull)
    )
    jsonResult
  }

  def jsTestResultSessionReports(list: Iterable[TestResult], orgParameterDefinitions: Option[List[OrganisationParameters]], orgParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]], sysParameterDefinitions: Option[List[SystemParameters]], sysParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]], resultCount: Option[Int]): JsObject = {
    var json = Json.arr()
    list.foreach { report =>
      json = json.append(jsTestResultReport(report, orgParameterDefinitions, orgParameterValues, sysParameterDefinitions, sysParameterValues))
    }
    var jsonResult = Json.obj(
      "data" -> json,
      "count" -> (if (resultCount.isDefined) resultCount.get else JsNull)
    )
    if (orgParameterDefinitions.isDefined) {
      var orgParameters = Json.arr()
      orgParameterDefinitions.get.foreach{ param =>
        orgParameters = orgParameters.append(JsString(param.testKey))
      }
      jsonResult = jsonResult.+("orgParameters" -> orgParameters)
    }
    if (sysParameterDefinitions.isDefined) {
      var sysParameters = Json.arr()
      sysParameterDefinitions.get.foreach{ param =>
        sysParameters = sysParameters.append(JsString(param.testKey))
      }
      jsonResult = jsonResult.+("sysParameters" -> sysParameters)
    }
    jsonResult
  }

  def jsId(id: Long): JsObject = {
    val json = Json.obj(
      "id"    -> id
    )
    json
  }

  def jsOrgParameterValuesForExport(organisationId: Long, orgParameterDefinitions: List[OrganisationParameters], orgParameterValues: scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]): JsObject = {
    var json = Json.obj()
    orgParameterDefinitions.foreach{ param =>
      if (orgParameterValues.contains(organisationId)) {
        val value = orgParameterValues(organisationId).get(param.id)
        if (value.isDefined) {
          json = json.+(param.testKey -> JsString(value.get))
        }
      }
    }
    json
  }

  def jsSysParameterValuesForExport(systemId: Long, sysParameterDefinitions: List[SystemParameters], sysParameterValues: scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]): JsObject = {
    var json = Json.obj()
    sysParameterDefinitions.foreach{ param =>
      if (sysParameterValues.contains(systemId)) {
        val value = sysParameterValues(systemId).get(param.id)
        if (value.isDefined) {
          json = json.+(param.testKey -> JsString(value.get))
        }
      }
    }
    json
  }

  def jsTestResultReport(result: TestResult, orgParameterDefinitions: Option[List[OrganisationParameters]], orgParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]], sysParameterDefinitions: Option[List[SystemParameters]], sysParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]]): JsObject = {
    val json = Json.obj(
      "result" -> jsTestResult(result, withExtendedInformation = false),
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
          "community" -> (if (result.communityId.isDefined) result.communityId.get else JsNull),
          "parameters" -> (if (result.organizationId.isDefined && orgParameterDefinitions.isDefined && orgParameterValues.isDefined) jsOrgParameterValuesForExport(result.organizationId.get, orgParameterDefinitions.get, orgParameterValues.get) else JsNull)
        )
      },
      "system" -> {
        Json.obj(
          "id"    -> (if (result.systemId.isDefined) result.systemId.get else JsNull),
          "sname" -> (if (result.system.isDefined) result.system.get else JsNull),
          "owner" -> (if (result.organizationId.isDefined) result.organizationId.get else JsNull),
          "parameters" -> (if (result.systemId.isDefined && sysParameterDefinitions.isDefined && sysParameterValues.isDefined) jsSysParameterValuesForExport(result.systemId.get, sysParameterDefinitions.get, sysParameterValues.get) else JsNull)
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

  def jsTestStepResultInfo(testSessionId: String, stepId: String, stepStatus: TestStepResultInfo, outputMessage: Option[String], stepHistory: List[(String, TestStepResultInfo)]): JsObject = {
    Json.obj(
      "tcInstanceId" -> testSessionId,
      "stepId" -> stepId,
      "status" -> stepStatus.result,
      "report" -> (if (stepStatus.path.isDefined) Json.obj("path" -> stepStatus.path.get) else JsNull),
      "outputMessage" -> (if (outputMessage.isDefined) outputMessage.get else JsNull),
      "stepHistory" -> jsTestStepResultInfoHistory(stepHistory)
    )
  }

  def jsTestStepResultInfoHistory(steps: List[(String, TestStepResultInfo)]): JsArray = {
    var json = Json.arr()
    steps.foreach{ stepInfo =>
      json = json.append(Json.obj(
        "stepId" -> stepInfo._1,
        "status" -> stepInfo._2.result,
        "path" -> (if (stepInfo._2.path.isDefined) stepInfo._2.path.get else JsNull)
      ))
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

  def jsSelfRegOption(option: SelfRegOption): JsObject = {
    val json = Json.obj(
        "communityId" -> option.communityId,
        "communityName" -> option.communityName,
        "communityDescription" -> (if (option.communityDescription.isDefined) option.communityDescription.get else JsNull),
        "selfRegTokenHelpText" -> (if (option.selfRegTokenHelpText.isDefined) option.selfRegTokenHelpText.get else JsNull),
        "selfRegType" -> option.selfRegType,
        "templates" -> (if (option.templates.isDefined) jsSelfRegTemplates(option.templates.get) else JsNull),
        "labels" -> jsCommunityLabels(option.labels),
        "organisationProperties" -> jsOrganisationParameters(option.customOrganisationProperties),
        "forceTemplateSelection" -> option.forceTemplateSelection,
        "forceRequiredProperties" -> option.forceRequiredProperties
    )
    json
  }

  def jsSelfRegTemplates(templates: List[SelfRegTemplate]): JsArray = {
    var json = Json.arr()
    templates.foreach{ template =>
      json = json.append(jsSelfRegTemplate(template))
    }
    json
  }

  def jsSelfRegTemplate(template: SelfRegTemplate): JsObject = {
    val json = Json.obj(
      "id" -> template.id,
      "name" -> template.name
    )
    json
  }

  def jsSelfRegOptions(options: List[SelfRegOption]): JsArray = {
    var json = Json.arr()
    options.foreach{ option =>
      json = json.append(jsSelfRegOption(option))
    }
    json
  }

  def serializeConfigurationProperties(config: util.HashMap[String, String]):JsObject = {
    val json = Json.obj(
      "emailEnabled" -> config.get("email.enabled").toBoolean,
      "emailAttachmentsMaxCount" -> config.get("email.attachments.maxCount").toInt,
      "emailAttachmentsMaxSize" -> config.get("email.attachments.maxSize").toLong,
      "emailAttachmentsAllowedTypes" -> config.get("email.attachments.allowedTypes"),
      "surveyEnabled" -> config.get("survey.enabled").toBoolean,
      "surveyAddress" -> config.get("survey.address"),
      "moreInfoEnabled" -> config.get("moreinfo.enabled").toBoolean,
      "moreInfoAddress" -> config.get("moreinfo.address"),
      "releaseInfoEnabled" -> config.get("releaseinfo.enabled").toBoolean,
      "releaseInfoAddress" -> config.get("releaseinfo.address"),
      "userGuideOU" -> config.get("userguide.ou"),
      "userGuideOA" -> config.get("userguide.oa"),
      "userGuideCA" -> config.get("userguide.ca"),
      "userGuideTA" -> config.get("userguide.ta"),
      "ssoEnabled" -> config.get("sso.enabled").toBoolean,
      "ssoInMigration" -> config.get("sso.inMigration").toBoolean,
      "demosEnabled" -> config.get("demos.enabled").toBoolean,
      "demosAccount" -> config.get("demos.account").toLong,
      "registrationEnabled" -> config.get("registration.enabled").toBoolean,
      "savedFileMaxSize" -> config.get("savedFile.maxSize").toLong,
      "mode" -> config.get("mode")
    )
    json
  }

  def serializeSystemConfig(sc:SystemConfiguration):String = {
    val jConfig:JsObject = jsSystemConfiguration(sc.toCaseObject)
    jConfig.toString
  }

  /**
   * Converts an Organization object into a JSON string with its complex objects
   * @param org Organization object to be converted
   * @return String
   */
  def serializeOrganization(org:Organization, includeAdminInfo: Boolean):String = {
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
    if(org.errorTemplateObj.isDefined){
      jOrganization = jOrganization ++ Json.obj("errorTemplates" -> jsErrorTemplate(org.errorTemplateObj.get))
    } else{
      jOrganization = jOrganization ++ Json.obj("errorTemplates" -> JsNull)
    }
    //
    if(org.community.isDefined){
      jOrganization = jOrganization ++ Json.obj("communities" -> jsCommunity(org.community.get, includeAdminInfo))
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
   * Converts a LegalNotice object into Play!'s JSON notation.
   * Does not support cross object conversion
   * @param legalNotice LegalNotice object to be converted
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
    * Converts a ErrorTemplate object into Play!'s JSON notation.
    * Does not support cross object conversion
    * @param errorTemplate ErrorTemplate object to be converted
    * @return JsObject
    */
  def jsErrorTemplate(errorTemplate:ErrorTemplates):JsObject = {
    val json = Json.obj(
      "id"    -> errorTemplate.id,
      "name"  -> errorTemplate.name,
      "description" -> (if(errorTemplate.description.isDefined) errorTemplate.description.get else JsNull),
      "content"  -> errorTemplate.content,
      "default" -> errorTemplate.default
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

  def jsTrigger(trigger:Triggers):JsObject = {
    val json = Json.obj(
      "id"    -> trigger.id,
      "name"  -> trigger.name,
      "description" -> (if(trigger.description.isDefined) trigger.description.get else JsNull),
      "url"  -> trigger.url,
      "eventType" -> trigger.eventType,
      "active" -> trigger.active,
      "latestResultOk" -> (if(trigger.latestResultOk.isDefined) trigger.latestResultOk.get else JsNull),
      "latestResultOutput" -> (if(trigger.latestResultOutput.isDefined) trigger.latestResultOutput.get else JsNull),
      "operation" -> (if(trigger.operation.isDefined) trigger.operation.get else JsNull)
    )
    json
  }

  def jsTriggers(list:List[Triggers]):JsArray = {
    var json = Json.arr()
    list.foreach{ trigger =>
      json = json.append(jsTrigger(trigger))
    }
    json
  }

  def jsTriggerDataItem(item:TriggerData):JsObject = {
    val json = Json.obj(
      "dataId"  -> item.dataId,
      "dataType"  -> item.dataType
    )
    json
  }

  def jsTriggerDataItems(items:List[TriggerData]):JsArray = {
    var json = Json.arr()
    items.foreach { item =>
      json = json.append(jsTriggerDataItem(item))
    }
    json
  }

  def jsTriggerInfo(trigger: Trigger): JsObject = {
    val json = Json.obj(
      "trigger"    -> jsTrigger(trigger.trigger),
      "data"  -> (if (trigger.data.isDefined) jsTriggerDataItems(trigger.data.get) else JsNull)
    )
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
    * Converts a List of ErrorTemplates into Play!'s JSON notation
    * Does not support cross object conversion
    * @param list List of ErrorTemplates to be convert
    * @return JsArray
    */
  def jsErrorTemplates(list:List[ErrorTemplates]):JsArray = {
    var json = Json.arr()
    list.foreach{ et =>
      json = json.append(jsErrorTemplate(et))
    }
    json
  }

  /**
   * Converts a LandingPage object into a JSON string with its complex objects
   * @param landingPage LandingPage object to be converted
   * @return String
   */
  def serializeLandingPage(landingPage: Option[LandingPage]):String = {
    //1) Serialize LandingPage
    val exists = landingPage.isDefined
    var jLandingPage:JsObject =  jsExists(exists)
    if (exists) {
      jLandingPage = jLandingPage ++ jsLandingPage(landingPage.get.toCaseObject)
    }
    //3) Return JSON String
    jLandingPage.toString
  }

  /**
   * Converts a LegalNotice object into a JSON string with its complex objects
   * @param legalNotice LegalNotice object to be converted
   * @return String
   */
  def serializeLegalNotice(legalNotice: Option[LegalNotice]):String = {
    //1) Serialize LegalNotice
    val exists = legalNotice.isDefined
    var jLegalNotice:JsObject = jsExists(exists)
    if (exists) {
      jLegalNotice = jLegalNotice ++ jsLegalNotice(legalNotice.get.toCaseObject)
    }
    //3) Return JSON String
    jLegalNotice.toString
  }

  /**
    * Converts a ErrorTemplate object into a JSON string with its complex objects
    * @param errorTemplate ErrorTemplate object to be converted
    * @return String
    */
  def serializeErrorTemplate(errorTemplate: Option[ErrorTemplate]):String = {
    //1) Serialize ErrorTemplate
    val exists = errorTemplate.isDefined
    var jErrorTemplate:JsObject = jsExists(exists)
    if (exists) {
      jErrorTemplate = jErrorTemplate ++ jsErrorTemplate(errorTemplate.get.toCaseObject)
    }
    //3) Return JSON String
    jErrorTemplate.toString
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
      "action" -> item.actionType,
      "specification" -> item.specification
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

  private def toJsArray(values: Option[List[Long]]): JsArray = {
    var json = Json.arr()
    if (values.isDefined) {
      values.get.foreach { value =>
        json = json.append(JsNumber(value))
      }
    }
    json
  }

  def jsTestSuiteUploadResult(result: TestSuiteUploadResult):JsObject = {
    val json = Json.obj(
      "success"    -> result.success,
      "errorInformation"  -> result.errorInformation,
      "pendingFolderId"  -> result.pendingTestSuiteFolderName,
      "existsForSpecs" -> toJsArray(result.existsForSpecs),
      "matchingDataExists" -> toJsArray(result.matchingDataExists),
      "items" -> jsTestSuiteUploadItemResults(result.items),
      "validationReport" -> (if (result.validationReport != null) jsTAR(result.validationReport) else JsNull),
      "needsConfirmation" -> result.needsConfirmation
    )
    json
  }

  def jsTAR(report: TAR): JsObject = {
    val json = Json.obj(
      "counters" -> jsValidationCounters(report.getCounters),
      "result" -> report.getResult.value(),
      "reports" -> jsTARReports(report.getReports)
    )
    json
  }

  def jsTARReports(reports: TestAssertionGroupReportsType): JsArray = {
    var json = Json.arr()
    if (reports != null && reports.getInfoOrWarningOrError != null) {
      reports.getInfoOrWarningOrError.asScala.toList.foreach(item => {
        val jsItem = jsTestAssertionReportType(item)
        if (jsItem != null) {
          json = json.append(jsItem)
        }
      })
    }
    json
  }

  def jsTestAssertionReportType(reportItem: JAXBElement[TestAssertionReportType]): JsObject = {
    if (reportItem != null && reportItem.getValue.isInstanceOf[BAR]) {
      val json = Json.obj(
        "level" -> reportItem.getName.getLocalPart,
        "assertionId" -> reportItem.getValue.asInstanceOf[BAR].getAssertionID,
        "description" -> reportItem.getValue.asInstanceOf[BAR].getDescription,
        "location" -> reportItem.getValue.asInstanceOf[BAR].getLocation
      )
      json
    } else {
      null
    }
  }

  def jsValidationCounters(counters: ValidationCounters): JsObject = {
    val json = Json.obj(
      "infos" -> counters.getNrOfAssertions.intValue(),
      "errors" -> counters.getNrOfErrors.intValue(),
      "warnings" -> counters.getNrOfWarnings.intValue()
    )
    json
  }

  def jsConformanceResultList(list: List[ConformanceStatusItem]): JsObject = {
    var updateTime: Option[Timestamp] = None
    var undefinedTests = 0L
    var completedTests = 0L
    var failedTests = 0L
    var itemArray = Json.arr()
    list.foreach{ info =>
      if (info.sessionTime.isDefined && (updateTime.isEmpty || updateTime.get.before(info.sessionTime.get))) {
        updateTime = info.sessionTime
      }
      if (TestResultStatus.withName(info.result) == TestResultStatus.SUCCESS) {
        completedTests += 1
      } else if (TestResultStatus.withName(info.result) == TestResultStatus.FAILURE) {
        failedTests += 1
      } else {
        undefinedTests += 1
      }
      itemArray = itemArray.append(jsConformanceResult(info))
    }
    var result = TestResultStatus.SUCCESS.toString
    if (failedTests > 0) {
      result = TestResultStatus.FAILURE.toString
    } else if (undefinedTests > 0) {
      result = TestResultStatus.UNDEFINED.toString
    }
    val json = Json.obj(
      "summary" -> Json.obj(
        "failed"    -> failedTests,
        "completed"    -> completedTests,
        "undefined"    -> undefinedTests,
        "result" -> result,
        "updateTime" -> (if (updateTime.isDefined) TimeUtil.serializeTimestamp(updateTime.get) else JsNull)
      ),
      "items" -> itemArray
    )
    json
  }

  def jsConformanceResult(listItem: ConformanceStatusItem): JsObject = {
    val json = Json.obj(
      "testSuiteId"    -> listItem.testSuiteId,
      "testSuiteName"    -> listItem.testSuiteName,
      "testSuiteDescription"    -> listItem.testSuiteDescription,
      "testSuiteHasDocumentation"    -> listItem.testSuiteHasDocumentation,
      "testCaseId"    -> listItem.testCaseId,
      "testCaseName"    -> listItem.testCaseName,
      "testCaseDescription"    -> listItem.testCaseDescription,
      "testCaseHasDocumentation"    -> listItem.testCaseHasDocumentation,
      "result"    -> listItem.result,
      "outputMessage" -> listItem.outputMessage,
      "sessionId"    -> listItem.sessionId,
      "sessionTime"    -> (if (listItem.sessionTime.isDefined) TimeUtil.serializeTimestamp(listItem.sessionTime.get) else JsNull)
    )
    json
  }

  def jsConformanceResultFullList(list: List[ConformanceStatementFull], orgParameterDefinitions: Option[List[OrganisationParameters]], orgParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]], sysParameterDefinitions: Option[List[SystemParameters]], sysParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]]): JsObject = {
    var json = Json.arr()
    list.foreach{ info =>
      json = json.append(jsConformanceResultFull(info, orgParameterDefinitions, orgParameterValues, sysParameterDefinitions, sysParameterValues))
    }
    var jsonResult = Json.obj(
      "data" -> json
    )
    if (orgParameterDefinitions.isDefined) {
      var orgParameters = Json.arr()
      orgParameterDefinitions.get.foreach{ param =>
        orgParameters = orgParameters.append(JsString(param.testKey))
      }
      jsonResult = jsonResult.+("orgParameters" -> orgParameters)
    }
    if (sysParameterDefinitions.isDefined) {
      var sysParameters = Json.arr()
      sysParameterDefinitions.get.foreach{ param =>
        sysParameters = sysParameters.append(JsString(param.testKey))
      }
      jsonResult = jsonResult.+("sysParameters" -> sysParameters)
    }
    jsonResult
  }

  def jsConformanceResultFull(item: ConformanceStatementFull,
                              orgParameterDefinitions: Option[List[OrganisationParameters]],
                              orgParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]],
                              sysParameterDefinitions: Option[List[SystemParameters]],
                              sysParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]]): JsObject = {
    var json = Json.obj(
      "communityId"    -> item.communityId,
      "communityName"    -> item.communityName,
      "organizationId"    -> item.organizationId,
      "organizationName"    -> item.organizationName,
      "systemId"    -> item.systemId,
      "systemName"    -> item.systemName,
      "domainId"    -> item.domainId,
      "domainName"    -> item.domainName,
      "specId"    -> item.specificationId,
      "specName"    -> item.specificationName,
      "actorId"    -> item.actorId,
      "actorName"    -> item.actorName,
      "testSuiteName" -> item.testSuiteName,
      "testCaseName" -> item.testCaseName,
      "testCaseDescription" -> item.testCaseDescription,
      "failed"    -> item.failedTests,
      "completed"    -> item.completedTests,
      "undefined"    -> item.undefinedTests,
      "result" -> item.result,
      "updateTime" -> (if(item.updateTime.isDefined) TimeUtil.serializeTimestamp(item.updateTime.get) else JsNull),
      "outputMessage" -> item.outputMessage
    )
    if (orgParameterDefinitions.isDefined && orgParameterValues.isDefined) {
      orgParameterDefinitions.get.foreach{ param =>
        if (orgParameterValues.get.contains(item.organizationId)) {
          val value = orgParameterValues.get(item.organizationId).get(param.id)
          if (value.isDefined) {
            json = json.+("orgparam_"+param.testKey -> JsString(value.get))
          }
        }
      }
    }
    if (sysParameterDefinitions.isDefined && sysParameterValues.isDefined) {
      sysParameterDefinitions.get.foreach{ param =>
        if (sysParameterValues.get.contains(item.systemId)) {
          val value = sysParameterValues.get(item.systemId).get(param.id)
          if (value.isDefined) {
            json = json.+("sysparam_"+param.testKey -> JsString(value.get))
          }
        }
      }
    }
    json
  }

  def jsConformanceSettings(settings:Option[ConformanceCertificates], includeKeystoreData: Boolean):Option[JsObject] = {
    if (settings.isDefined) {
      val json = Json.obj(
        "id"    -> settings.get.id,
        "title" -> (if(settings.get.title.isDefined) settings.get.title.get else JsNull),
        "message" -> (if(settings.get.message.isDefined) settings.get.message.get else JsNull),
        "includeMessage" -> settings.get.includeMessage,
        "includeTestStatus" -> settings.get.includeTestStatus,
        "includeTestCases" -> settings.get.includeTestCases,
        "includeDetails" -> settings.get.includeDetails,
        "includeSignature" -> settings.get.includeSignature,
        "keystoreType" -> (if(includeKeystoreData && settings.get.keystoreType.isDefined) settings.get.keystoreType.get else JsNull),
        "passwordsSet" -> (if(includeKeystoreData && settings.get.keystorePassword.isDefined && settings.get.keyPassword.isDefined) true else false),
        "keystoreDefined" -> (if(settings.get.keystoreFile.isDefined && settings.get.keystoreType.isDefined && settings.get.keystorePassword.isDefined && settings.get.keyPassword.isDefined) true else false),
        "community" -> settings.get.community
      )
      Some(json)
    } else {
      None
    }
  }

  def jsConformanceSettingsValidation(problem: String, level: String): JsObject = {
    val json = Json.obj(
      "problem"    -> problem,
      "level"    -> level
    )
    json
  }

  def jsOrganisationParametersWithValues(list: List[OrganisationParametersWithValue], includeValues: Boolean):JsArray = {
    var json = Json.arr()
    list.foreach { parameter =>
      json = json.append(jsOrganisationParametersWithValue(parameter, includeValues))
    }
    json
  }

  def jsOrganisationParametersWithValue(param: OrganisationParametersWithValue, includeValues: Boolean): JsObject = {
    var json = jsOrganisationParameter(param.parameter)
    json = json.+("configured" -> JsBoolean(param.value.isDefined))
    if (includeValues) {
      if (param.value.isDefined && param.parameter.kind != "SECRET") {
        json = json.+("value" -> JsString(param.value.get.value))
        if (param.value.get.contentType.isDefined && param.parameter.kind == "BINARY") {
          json = json.+("mimeType" -> JsString(param.value.get.contentType.get))
        }
      }
    }
    json
  }

  def jsSystemParametersWithValues(list: List[SystemParametersWithValue], includeValues: Boolean):JsArray = {
    var json = Json.arr()
    list.foreach { parameter =>
      json = json.append(jsSystemParametersWithValue(parameter, includeValues))
    }
    json
  }

  def jsSystemParametersWithValue(param: SystemParametersWithValue, includeValues: Boolean): JsObject = {
    var json = jsSystemParameter(param.parameter)
    json = json.+("configured" -> JsBoolean(param.value.isDefined))
    if (includeValues) {
      if (param.value.isDefined && param.parameter.kind != "SECRET") {
        json = json.+("value" -> JsString(param.value.get.value))
        if (param.value.get.contentType.isDefined && param.parameter.kind == "BINARY") {
          json = json.+("mimeType" -> JsString(param.value.get.contentType.get))
        }
      }
    }
    json
  }

  def jsCommunityLabels(labels: List[CommunityLabels]): JsArray = {
    var json = Json.arr()
    labels.foreach { label =>
      json = json.append(jsCommunityLabel(label))
    }
    json
  }

  def jsCommunityLabel(label: CommunityLabels): JsObject = {
    val json = Json.obj(
      "labelType"    -> label.labelType,
      "singularForm"    -> label.singularForm,
      "pluralForm"    -> label.pluralForm,
      "fixedCase" -> JsBoolean(label.fixedCase)
    )
    json
  }

}