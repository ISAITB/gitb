package utils

import com.gitb.core.{AnyContent, ValueEmbeddingEnumeration}
import com.gitb.ps.{ProcessRequest, ProcessResponse}
import com.gitb.tbs.UserInput
import com.gitb.tr._
import config.Configurations
import controllers.dto.ParameterInfo
import exceptions.JsonValidationException
import jakarta.xml.bind.JAXBElement
import managers.breadcrumb.BreadcrumbLabelResponse
import managers.export.{ExportSettings, ImportItem, ImportSettings}
import models.Enums.TestSuiteReplacementChoice.TestSuiteReplacementChoice
import models.Enums._
import models._
import models.TestCaseGroup
import models.automation._
import models.snapshot.ConformanceSnapshot
import models.theme.Theme
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.StringUtils
import play.api.libs.json.{JsObject, Json, _}

import java.util
import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsJava}

object JsonUtil {

  def jsThemes(themes: List[Theme]): JsArray = {
    var json = Json.arr()
    themes.foreach { theme =>
      json = json.append(jsTheme(theme))
    }
    json
  }

  def jsTheme(theme: Theme): JsObject = {
    Json.obj(
      "id" -> theme.id,
      "key" -> theme.key,
      "description" -> (if (theme.description.isDefined) theme.description.get else JsNull),
      "active" -> JsBoolean(theme.active),
      "custom" -> JsBoolean(theme.custom),
      "separatorTitleColor" -> theme.separatorTitleColor,
      "modalTitleColor" -> theme.modalTitleColor,
      "tableTitleColor" -> theme.tableTitleColor,
      "cardTitleColor" -> theme.cardTitleColor,
      "pageTitleColor" -> theme.pageTitleColor,
      "headingColor" -> theme.headingColor,
      "tabLinkColor" -> theme.tabLinkColor,
      "footerTextColor" -> theme.footerTextColor,
      "headerBackgroundColor" -> theme.headerBackgroundColor,
      "headerBorderColor" -> theme.headerBorderColor,
      "headerSeparatorColor" -> theme.headerSeparatorColor,
      "headerLogoPath" -> theme.headerLogoPath,
      "footerBackgroundColor" -> theme.footerBackgroundColor,
      "footerBorderColor" -> theme.footerBorderColor,
      "footerLogoPath" -> theme.footerLogoPath,
      "footerLogoDisplay" -> theme.footerLogoDisplay,
      "faviconPath" -> theme.faviconPath,
      "primaryButtonColor" -> theme.primaryButtonColor,
      "primaryButtonLabelColor" -> theme.primaryButtonLabelColor,
      "primaryButtonHoverColor" -> theme.primaryButtonHoverColor,
      "primaryButtonActiveColor" -> theme.primaryButtonActiveColor,
      "secondaryButtonColor" -> theme.secondaryButtonColor,
      "secondaryButtonLabelColor" -> theme.secondaryButtonLabelColor,
      "secondaryButtonHoverColor" -> theme.secondaryButtonHoverColor,
      "secondaryButtonActiveColor" -> theme.secondaryButtonActiveColor
    )
  }

  def jsErrorMessages(texts: List[String], contentType: String): JsObject = {
    var json = jsTextArray(texts)
    json += ("success", JsBoolean(false))
    json += ("contentType", JsString(contentType))
    json
  }

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

  def jsMessages(texts: Iterable[String]): JsValue = {
    var json = Json.obj()
    if (texts.nonEmpty) {
      json = json + ("messages" -> JsonUtil.jsStringArray(texts))
    }
    json
  }

  def jsStringArray(texts: Iterable[String]): JsValue = {
    var textArray = Json.arr()
    texts.foreach { text =>
      textArray = textArray.append(JsString(text))
    }
    textArray
  }

  def jsApiKeyInfo(apiKeyInfo: ApiKeyInfo): JsObject = {
    val json = Json.obj(
      "organisation" -> (if (apiKeyInfo.organisation.isDefined) apiKeyInfo.organisation.get else JsNull),
      "systems" -> jsApiKeySystemInfo(apiKeyInfo.systems),
      "specifications" -> jsApiKeySpecificationsInfo(apiKeyInfo.specifications)
    )
    json
  }

  private def jsApiKeySystemInfo(systems: List[ApiKeySystemInfo]) = {
    var json = Json.arr()
    systems.foreach { system =>
      json = json.append(Json.obj(
        "id" -> system.id,
        "name" -> system.name,
        "key" -> system.key
      ))
    }
    json
  }

  private def jsApiKeySpecificationsInfo(specifications: List[ApiKeySpecificationInfo]) = {
    var json = Json.arr()
    specifications.foreach { specification =>
      json = json.append(Json.obj(
        "name" -> specification.name,
        "actors" -> jsApiKeyActorsInfo(specification.actors),
        "testSuites" -> jsApiKeyTestSuiteInfo(specification.testSuites)
      ))
    }
    json
  }

  private def jsApiKeyActorsInfo(actors: List[ApiKeyActorInfo]) = {
    var json = Json.arr()
    actors.foreach { actor =>
      json = json.append(Json.obj(
        "name" -> actor.name,
        "key" -> actor.key
      ))
    }
    json
  }

  private def jsApiKeyTestSuiteInfo(testSuites: List[ApiKeyTestSuiteInfo]) = {
    var json = Json.arr()
    testSuites.foreach { testSuite =>
      json = json.append(Json.obj(
        "name" -> testSuite.name,
        "key" -> testSuite.key,
        "testCases" -> jsApiKeyTestCaseInfo(testSuite.testcases)
      ))
    }
    json
  }

  private def jsApiKeyTestCaseInfo(testCases: List[ApiKeyTestCaseInfo]) = {
    var json = Json.arr()
    testCases.foreach { testCase =>
      json = json.append(Json.obj(
        "name" -> testCase.name,
        "key" -> testCase.key
      ))
    }
    json
  }

  def jsTestSessionLaunchInfo(sessions: Seq[TestSessionLaunchInfo]): JsObject = {
    var jsonItems = Json.arr()
    sessions.foreach{ item =>
      var session = Json.obj(
        "testSuite" -> item.testSuiteIdentifier,
        "testCase" -> item.testCaseIdentifier,
        "session" -> item.testSessionIdentifier
      )
      if (item.completed.isDefined) {
        session = session + ("completed" -> JsBoolean(item.completed.get))
      }
      jsonItems = jsonItems.append(session)
    }
    val json = Json.obj("createdSessions" -> jsonItems)
    json
  }

  def jsTestSessionStatusInfo(statusItems: Seq[TestSessionStatus]): JsObject = {
    var jsonItems = Json.arr()
    statusItems.foreach{ item =>
      var obj = Json.obj(
        "session" -> item.sessionId,
        "result" -> item.result,
        "startTime" -> TimeUtil.serializeTimestampUTC(item.startTime)
      )
      if (item.endTime.isDefined) {
        obj = obj.+("endTime", JsString(TimeUtil.serializeTimestampUTC(item.endTime.get)))
      }
      if (item.outputMessage.isDefined) {
        obj = obj.+("message", JsString(item.outputMessage.get))
      }
      if (item.logs.isDefined) {
        var logItems = Json.arr()
        item.logs.get.foreach { log =>
          logItems = logItems.append(JsString(log))
        }
        obj = obj.+("logs", logItems)
      }
      if (item.report.isDefined) {
        obj = obj.+("report", JsString(item.report.get))
      }
      jsonItems = jsonItems.append(obj)
    }
    val json = Json.obj("sessions" -> jsonItems)
    json
  }

  def jsImportPreviewResult(pendingImportId: String, importItems: Iterable[ImportItem]): JsObject = {
    val json = Json.obj(
      "pendingImportId" -> pendingImportId,
      "importItems" -> jsImportItems(importItems)
    )
    json
  }

  def jsUpdateCounts(created: Int, updated: Int): JsObject = {
    val json = Json.obj(
      "created" -> created,
      "updated" -> updated
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

  def jsConformanceStatementItemInfo(info: (Boolean, Iterable[ConformanceStatementItem])): JsObject = {
    Json.obj(
      "existing" -> info._1,
      "items" -> jsConformanceStatementItems(info._2)
    )
  }

  def jsConformanceStatementItems(items: Iterable[ConformanceStatementItem]): JsArray = {
    var json = Json.arr()
    items.foreach { item =>
      json = json.append(jsConformanceStatementItem(item))
    }
    json
  }

  def jsConformanceStatementItem(item: ConformanceStatementItem): JsObject = {
    var json = Json.obj(
      "id" -> item.id,
      "itemType" -> item.itemType.id,
      "name" -> item.name,
      "order" -> item.displayOrder
    )
    if (item.description.nonEmpty && item.description.get.nonEmpty) {
      json = json +("description" -> JsString(item.description.get))
    }
    if (item.itemType == ConformanceStatementItemType.ACTOR && !item.actorToShow) {
      json = json + ("hidden" -> JsBoolean(true))
    }
    if (item.items.nonEmpty) {
      json = json +("items" -> jsConformanceStatementItems(item.items.get))
    }
    if (item.results.isDefined) {
      json = json +("results" -> Json.obj(
          "updateTime" -> (if (item.results.get.updateTime.isDefined) TimeUtil.serializeTimestamp(item.results.get.updateTime.get) else JsNull),
          "undefined" -> item.results.get.undefinedTests,
          "failed" -> item.results.get.failedTests,
          "completed" -> item.results.get.completedTests,
          "undefinedOptional" -> item.results.get.undefinedOptionalTests,
          "failedOptional" -> item.results.get.failedOptionalTests,
          "completedOptional" -> item.results.get.completedOptionalTests,
          "undefinedToConsider" -> item.results.get.undefinedTestsToConsider,
          "failedToConsider" -> item.results.get.failedTestsToConsider,
          "completedToConsider" -> item.results.get.completedTestsToConsider
        )
      )
    }
    json
  }

  def jsCommunityResourceSearchResult(list: Iterable[CommunityResources], resultCount: Int): JsObject = {
    val jsonResult = Json.obj(
      "data" -> jsCommunityResources(list),
      "count" -> resultCount
    )
    jsonResult
  }

  def jsCommunityResources(resources: Iterable[CommunityResources]): JsArray = {
    var json = Json.arr()
    resources.foreach { resource =>
      json = json.append(jsCommunityResource(resource))
    }
    json
  }

  def jsCommunityResource(resource: CommunityResources): JsObject = {
    val json = Json.obj(
      "id" -> resource.id,
      "name" -> resource.name,
      "description" -> (if(resource.description.isDefined) resource.description.get else JsNull),
      "reference" -> pathForResource(resource),
      "community" -> resource.community
    )
    json
  }

  private def pathForResource(resource: CommunityResources): String = {
    if (resource.community == Constants.DefaultCommunityId) {
      "systemResources/" + resource.name
    } else {
      "resources/" + resource.name
    }
  }

  def jsTestSuite(suite: TestSuites, specificationIds: Option[List[Long]], withDocumentation: Boolean, withSpecReference: Boolean): JsObject = {
    var json = Json.obj(
      "id"                -> suite.id,
      "identifier"        -> suite.identifier,
      "sname"             -> suite.shortname,
      "fname"             -> suite.fullname,
      "version"           -> suite.version,
      "specifications"    -> toJsArray(specificationIds),
      "authors"           -> (if(suite.authors.isDefined) suite.authors.get else JsNull),
      "description"       -> (if(suite.description.isDefined) suite.description.get else JsNull),
      "keywords"          -> (if(suite.keywords.isDefined) suite.keywords.get else JsNull),
      "modificationDate"  -> (if(suite.modificationDate.isDefined) suite.modificationDate.get else JsNull),
      "originalDate"      -> (if(suite.originalDate.isDefined) suite.originalDate.get else JsNull),
      "hasDocumentation"  -> suite.hasDocumentation,
      "shared"            -> suite.shared,
      "documentation"     -> (if(withDocumentation && suite.documentation.isDefined) suite.documentation else JsNull),
    )
    if (withSpecReference) {
      if (suite.specReference.isDefined) json = json + ("specReference" -> JsString(suite.specReference.get))
      if (suite.specDescription.isDefined) json = json + ("specDescription" -> JsString(suite.specDescription.get))
      if (suite.specLink.isDefined) json = json + ("specLink" -> JsString(suite.specLink.get))
    }
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
      json = json.append(jsTestSuite(testSuite, None, withDocumentation = false, withSpecReference = false))
    }
    json
  }

  def jsTestSuite(testSuite: TestSuite, withDocumentation: Boolean, withSpecReference: Boolean, withGroups: Boolean, withTags: Boolean): JsObject = {
    var obj: JsObject = jsTestSuite(testSuite.toCaseObject, testSuite.specifications, withDocumentation, withSpecReference)
    if (testSuite.testCases.isDefined) {
      obj = obj ++ Json.obj("testCases" -> jsTestCasesList(testSuite.testCases.get, withSpecReference, withTags))
    } else {
      obj = obj ++ Json.obj("testCases" -> JsNull)
    }
    if (withGroups && testSuite.testCaseGroups.isDefined) {
      obj = obj ++ Json.obj("testCaseGroups" -> jsTestCaseGroups(testSuite.testCaseGroups.get))
    }
    obj
  }

  def jsTestSuiteList(testSuites: Iterable[TestSuite]): JsArray = {
    var json = Json.arr()
    testSuites.foreach { testSuite =>
      json = json.append(jsTestSuite(testSuite, withDocumentation = false, withSpecReference = false, withGroups = false, withTags = false))
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
      "testKey" -> parameter.testKey,
			"desc" -> (if (parameter.desc.isDefined) parameter.desc.get else JsNull),
			"use" -> parameter.use,
			"kind" -> parameter.kind,
      "adminOnly" -> parameter.adminOnly,
      "notForTests" -> parameter.notForTests,
      "hidden" -> parameter.hidden,
      "allowedValues" -> (if (parameter.allowedValues.isDefined) parameter.allowedValues.get else JsNull),
      "dependsOn" -> (if (parameter.dependsOn.isDefined) parameter.dependsOn.get else JsNull),
      "dependsOnValue" -> (if (parameter.dependsOnValue.isDefined) parameter.dependsOnValue.get else JsNull),
      "defaultValue" -> (if (parameter.defaultValue.isDefined) parameter.defaultValue.get else JsNull),
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
      "defaultValue" -> (if (parameter.defaultValue.isDefined) parameter.defaultValue.get else JsNull),
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
      "defaultValue" -> (if (parameter.defaultValue.isDefined) parameter.defaultValue.get else JsNull),
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

  def jsSystemConfigurations(configs: List[SystemConfigurationsWithEnvironment]): JsArray = {
    var json = Json.arr()
    configs.foreach { config =>
      json = json.append(jsSystemConfiguration(config))
    }
    json
  }

  def jsSystemConfiguration(config: SystemConfigurationsWithEnvironment):JsObject = {
    var json = Json.obj(
      "name"    -> config.config.name,
      "default" -> config.defaultSetting,
      "environment" -> config.environmentSetting
    )
    if (config.config.parameter.isDefined) {
      if (config.config.name == Constants.EmailSettings) {
        // Make sure the SMTP password is not included.
        json = json + ("parameter" -> JsString(jsEmailSettings(parseJsEmailSettings(config.config.parameter.get)).toString()))
      } else {
        json = json + ("parameter" -> JsString(config.config.parameter.get))
      }
    }
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
      "version" -> (if(system.version.isDefined) system.version.get else JsNull),
      "apiKey" -> system.apiKey,
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
      "allowAutomationApi" -> community.allowAutomationApi,
      "allowCommunityView" -> community.allowCommunityView,
      "domainId" -> community.domain
    )
    if (includeAdminInfo) {
      json = json.+("email" -> (if (community.supportEmail.isDefined) JsString(community.supportEmail.get) else JsNull))
      json = json.+("selfRegType" -> JsNumber(community.selfRegType))
      json = json.+("selfRegRestriction" -> JsNumber(community.selfRegRestriction))
      json = json.+("selfRegToken" -> (if(community.selfRegToken.isDefined) JsString(community.selfRegToken.get) else JsNull))
      json = json.+("selfRegTokenHelpText" -> (if(community.selfRegTokenHelpText.isDefined) JsString(community.selfRegTokenHelpText.get) else JsNull))
      json = json.+("selfRegNotification" -> JsBoolean(community.selfRegNotification))
      json = json.+("selfRegForceTemplateSelection" -> JsBoolean(community.selfRegForceTemplateSelection))
      json = json.+("selfRegForceRequiredProperties" -> JsBoolean(community.selfRegForceRequiredProperties))
      json = json.+("description" -> (if(community.description.isDefined) JsString(community.description.get) else JsNull))
      json = json.+("interactionNotification" -> JsBoolean(community.interactionNotification))
      if (Configurations.AUTOMATION_API_ENABLED) {
        json = json.+("apiKey" -> JsString(community.apiKey))
      }
    }
    json
  }

  def serializeCommunity(community:Community, labels: Option[List[CommunityLabels]], includeAdminInfo: Boolean):String = {
    var jCommunity:JsObject = jsCommunity(community.toCaseObject, includeAdminInfo)
    if(community.domain.isDefined){
      jCommunity = jCommunity ++ Json.obj("domain" -> jsDomain(community.domain.get, withApiKeys = false))
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
  def jsDomain(domain:Domain, withApiKeys: Boolean):JsObject = {
    var json = Json.obj(
      "id" -> domain.id,
      "sname" -> domain.shortname,
      "fname" -> domain.fullname,
      "description" -> domain.description,
      "reportMetadata" -> domain.reportMetadata
    )
    if (withApiKeys && Configurations.AUTOMATION_API_ENABLED) {
      json = json.+("apiKey" -> JsString(domain.apiKey))
    }
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
  def jsDomains(list:List[Domain], withApiKeys: Boolean):JsArray = {
    var json = Json.arr()
    list.foreach{ domain =>
      json = json.append(jsDomain(domain, withApiKeys))
    }
    json
  }

  def jsCommunityDomains(domains: List[Domain], linkedDomain: Option[Long]): JsObject = {
    var json = Json.obj(
      "domains" -> jsDomains(domains, withApiKeys = false)
    )
    if (linkedDomain.isDefined) {
      json = json + ("linkedDomain" -> JsNumber(linkedDomain.get))
    }
    json
  }

  def jsFileReference(dataURL: String, mimeType: String): JsObject = {
    Json.obj(
      "dataAsDataURL" -> dataURL,
      "mimeType" -> mimeType
    )
  }

  /**
   * Converts a Specification object into Play!'s JSON notation.
   * @param spec Specification object to be converted
   * @return JsObject
   */
  def jsSpecification(spec:Specifications, withApiKeys:Boolean = false, badgeStatus: Option[(BadgeStatus, BadgeStatus)] = None) : JsObject = {
    var json = Json.obj(
      "id"      -> spec.id,
      "sname"   -> spec.shortname,
      "fname"   -> spec.fullname,
      "description" -> (if(spec.description.isDefined) spec.description.get else JsNull),
      "reportMetadata" -> (if(spec.reportMetadata.isDefined) spec.reportMetadata.get else JsNull),
      "hidden" -> spec.hidden,
      "domain"  -> spec.domain,
      "group" -> spec.group,
      "order" -> spec.displayOrder
    )
    if (withApiKeys && Configurations.AUTOMATION_API_ENABLED) {
      json = json.+("apiKey" -> JsString(spec.apiKey))
    }
    if (badgeStatus.isDefined) {
      json = json.+("badges" -> jsBadgeStatus(badgeStatus.get._1, badgeStatus.get._2))
    }
    json
  }

  private def jsBadgeStatus(badgeStatus: BadgeStatus, badgeStatusForReport: BadgeStatus): JsObject = {
    Json.obj(
      "success" -> Json.obj(
        "enabled" -> badgeStatus.success.isDefined,
        "nameToShow" -> (if (badgeStatus.success.isDefined) badgeStatus.success.get else JsNull)
      ),
      "failure" -> Json.obj(
        "enabled" -> badgeStatus.failure.isDefined,
        "nameToShow" -> (if (badgeStatus.failure.isDefined) badgeStatus.failure.get else JsNull)
      ),
      "other" -> Json.obj(
        "enabled" -> badgeStatus.other.isDefined,
        "nameToShow" -> (if (badgeStatus.other.isDefined) badgeStatus.other.get else JsNull)
      ),
      "successForReport" -> Json.obj(
        "enabled" -> badgeStatusForReport.success.isDefined,
        "nameToShow" -> (if (badgeStatusForReport.success.isDefined) badgeStatusForReport.success.get else JsNull)
      ),
      "failureForReport" -> Json.obj(
        "enabled" -> badgeStatusForReport.failure.isDefined,
        "nameToShow" -> (if (badgeStatusForReport.failure.isDefined) badgeStatusForReport.failure.get else JsNull)
      ),
      "otherForReport" -> Json.obj(
        "enabled" -> badgeStatusForReport.other.isDefined,
        "nameToShow" -> (if (badgeStatusForReport.other.isDefined) badgeStatusForReport.other.get else JsNull)
      )
    )
  }

  def jsSpecificationGroup(group: SpecificationGroups, withApiKeys: Boolean): JsObject = {
    var json = Json.obj(
      "id" -> group.id,
      "sname" -> group.shortname,
      "fname" -> group.fullname,
      "description" -> (if (group.description.isDefined) group.description.get else JsNull),
      "reportMetadata" -> (if (group.reportMetadata.isDefined) group.reportMetadata.get else JsNull),
      "domain" -> group.domain,
      "order" -> group.displayOrder
    )
    if (withApiKeys && Configurations.AUTOMATION_API_ENABLED) {
      json = json.+("apiKey" -> JsString(group.apiKey))
    }
    json
  }

  /**
   * Converts a List of Specifications into Play!'s JSON notation
   * @return JsArray
   */
  def jsSpecifications(list:Iterable[Specifications], withApiKeys:Boolean = false):JsArray = {
    var json = Json.arr()
    list.foreach{ spec =>
      json = json.append(jsSpecification(spec, withApiKeys, None))
    }
    json
  }

  def jsSpecificationGroups(list: List[SpecificationGroups]): JsArray = {
    var json = Json.arr()
    list.foreach { group =>
      json = json.append(jsSpecificationGroup(group, withApiKeys = false))
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
      "reportMetadata" -> (if(actor.reportMetadata.isDefined) actor.reportMetadata.get else JsNull),
      "default" -> (if(actor.default.isDefined) actor.default.get else JsNull),
      "hidden" -> actor.hidden,
      "displayOrder" -> (if(actor.displayOrder.isDefined) actor.displayOrder.get else JsNull),
      "domain"  -> actor.domain
    )
    json
  }

  def jsActor(actor:Actor, badgeStatus: Option[(BadgeStatus, BadgeStatus)] = None) : JsObject = {
    var json = Json.obj(
      "id" -> actor.id,
      "actorId" -> actor.actorId,
      "name"   -> actor.name,
      "description" -> (if(actor.description.isDefined) actor.description.get else JsNull),
      "reportMetadata" -> (if(actor.reportMetadata.isDefined) actor.reportMetadata.get else JsNull),
      "default" -> (if(actor.default.isDefined) actor.default.get else JsNull),
      "hidden" -> actor.hidden,
      "displayOrder" -> (if(actor.displayOrder.isDefined) actor.displayOrder.get else JsNull),
      "domain"  -> (if(actor.domain.isDefined) actor.domain.get.id else JsNull),
      "specification"  -> (if(actor.specificationId.isDefined) actor.specificationId.get else JsNull)
    )
    if (Configurations.AUTOMATION_API_ENABLED) {
      json = json.+("apiKey"  -> (if(actor.apiKey.isDefined) JsString(actor.apiKey.get) else JsNull))
    }
    if (badgeStatus.isDefined) {
      json = json.+("badges" -> jsBadgeStatus(badgeStatus.get._1, badgeStatus.get._2))
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

	def jsConformanceStatement(conformanceStatement: models.ConformanceStatement): JsObject = {
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
				"completed" -> conformanceStatement.completedTests,
        "undefinedOptional" -> conformanceStatement.undefinedOptionalTests,
        "failedOptional" -> conformanceStatement.failedOptionalTests,
        "completedOptional" -> conformanceStatement.completedOptionalTests,
        "undefinedToConsider" -> conformanceStatement.undefinedTestsToConsider,
        "failedToConsider" -> conformanceStatement.failedTestsToConsider,
        "completedToConsider" -> conformanceStatement.completedTestsToConsider
      )
		)
		json
	}

	def jsConformanceStatements(list: List[models.ConformanceStatement]): JsArray = {
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

  def parseJsSessions(jsonConfig: JsValue): List[String] = {
    val sessionIds: List[String] = (jsonConfig \ "session").asOpt[JsArray].getOrElse(JsArray.empty).value.map { jsValue =>
      jsValue.as[String]
    }.toList
    sessionIds
  }

  def parseJsSessionStatusRequest(jsonConfig: JsValue): (List[String], Boolean, Boolean) = {
    val sessionIds: List[String] = (jsonConfig \ "session").asOpt[JsArray].getOrElse(JsArray.empty).value.map { jsValue =>
      jsValue.as[String]
    }.toList
    val withLogs = (jsonConfig \ "withLogs").asOpt[Boolean].getOrElse(false)
    val withReports = (jsonConfig \ "withReports").asOpt[Boolean].getOrElse(false)
    (sessionIds, withLogs, withReports)
  }

  def parseJsTestSessionLaunchRequest(jsonConfig: JsValue, organisationKey: String): TestSessionLaunchRequest = {
    val system = (jsonConfig \ "system").asOpt[String]
    val actor = (jsonConfig \ "actor").asOpt[String]
    val testSuites: List[String] = (jsonConfig \ "testSuite").asOpt[JsArray].getOrElse(JsArray.empty).value.map { jsValue =>
      jsValue.as[JsString].value
    }.toList
    val testCases: List[String] = (jsonConfig \ "testCase").asOpt[JsArray].getOrElse(JsArray.empty).value.map { jsValue =>
      jsValue.as[JsString].value
    }.toList
    val inputMappings: List[InputMapping] = (jsonConfig \ "inputMapping").asOpt[JsArray].getOrElse(JsArray.empty).value.map { jsValue =>
      parseJsInputMapping(jsValue)
    }.toList
    val forceSequential = (jsonConfig \ "forceSequentialExecution").asOpt[Boolean].getOrElse(false)
    val waitForCompletion = (jsonConfig \ "waitForCompletion").asOpt[Boolean].getOrElse(false)
    val maximumWaitTime = (jsonConfig \ "maximumWaitTime").asOpt[Long]
    TestSessionLaunchRequest(organisationKey, system, actor, testSuites, testCases, inputMappings, forceSequential, waitForCompletion, maximumWaitTime)
  }

  def parseJsTestSuiteDeployRequest(jsonConfig: JsValue, sharedTestSuite: Boolean): (TestSuiteDeployRequest, String) = {
    val specification = if (sharedTestSuite) {
      None
    } else {
      Some((jsonConfig \ "specification").as[String])
    }
    val testSuite = (jsonConfig \ "testSuite").as[String]
    val ignoreWarnings = (jsonConfig \ "ignoreWarnings").asOpt[Boolean].getOrElse(false)
    val replaceTestHistory = (jsonConfig \ "replaceTestHistory").asOpt[Boolean]
    val updateSpecification = (jsonConfig \ "updateSpecification").asOpt[Boolean]
    val testCaseActions = (jsonConfig \ "testCases").asOpt[List[JsValue]].getOrElse(List.empty).map { item =>
      val testCase = new TestCaseDeploymentAction(
        (item \ "identifier").as[String],
        (item \ "updateSpecification").asOpt[Boolean],
        (item \ "replaceTestHistory").asOpt[Boolean]
      )
      if (testCase.updateDefinition.isEmpty) testCase.updateDefinition = updateSpecification
      if (testCase.resetTestHistory.isEmpty) testCase.resetTestHistory = replaceTestHistory
      testCase
    }
    val testCaseMap = new mutable.HashMap[String, TestCaseDeploymentAction]()
    testCaseActions.foreach { action =>
      testCaseMap.put(action.identifier, action)
    }
    (TestSuiteDeployRequest(specification, ignoreWarnings, replaceTestHistory, updateSpecification, testCaseMap, sharedTestSuite), testSuite)
  }

  def parseJsTestSuiteUndeployRequest(jsonConfig: JsValue, sharedTestSuite: Boolean): TestSuiteUndeployRequest = {
    val specification = if (sharedTestSuite) {
      None
    } else {
      (jsonConfig \ "specification").asOpt[String]
    }
    val testSuite = (jsonConfig \ "testSuite").as[String]
    TestSuiteUndeployRequest(specification, testSuite, sharedTestSuite)
  }

  def parseJsCreateSpecificationGroupRequest(json: JsValue, communityApiKey: String): CreateSpecificationGroupRequest = {
    CreateSpecificationGroupRequest(
      (json \ "shortName").as[String],
      (json \ "fullName").as[String],
      (json \ "description").asOpt[String],
      (json \ "reportMetadata").asOpt[String],
      (json \ "displayOrder").asOpt[Short],
      (json \ "apiKey").asOpt[String],
      (json \ "domain").asOpt[String],
      communityApiKey
    )
  }

  def parseJsCreateOrganisationRequest(json: JsValue, communityApiKey: String): CreateOrganisationRequest = {
    CreateOrganisationRequest(
      (json \ "shortName").as[String],
      (json \ "fullName").as[String],
      (json \ "apiKey").asOpt[String],
      communityApiKey
    )
  }

  def parseJsCreateSystemRequest(json: JsValue, communityApiKey: String): CreateSystemRequest = {
    CreateSystemRequest(
      (json \ "shortName").as[String],
      (json \ "fullName").as[String],
      (json \ "description").asOpt[String],
      (json \ "version").asOpt[String],
      (json \ "apiKey").asOpt[String],
      (json \ "organisation").as[String],
      communityApiKey
    )
  }

  def parseJsCreateSpecificationRequest(json: JsValue, communityApiKey: String): CreateSpecificationRequest = {
    CreateSpecificationRequest(
      (json \ "shortName").as[String],
      (json \ "fullName").as[String],
      (json \ "description").asOpt[String],
      (json \ "reportMetadata").asOpt[String],
      (json \ "hidden").asOpt[Boolean],
      (json \ "displayOrder").asOpt[Short],
      (json \ "apiKey").asOpt[String],
      (json \ "domain").asOpt[String],
      (json \ "group").asOpt[String],
      communityApiKey
    )
  }

  def parseJsCreateActorRequest(json: JsValue, communityApiKey: String): CreateActorRequest = {
    CreateActorRequest(
      (json \ "identifier").as[String],
      (json \ "name").as[String],
      (json \ "description").asOpt[String],
      (json \ "reportMetadata").asOpt[String],
      (json \ "default").asOpt[Boolean],
      (json \ "hidden").asOpt[Boolean],
      (json \ "displayOrder").asOpt[Short].flatMap(x => if (x < 0) None else Some(x)),
      (json \ "apiKey").asOpt[String],
      (json \ "specification").as[String],
      communityApiKey
    )
  }

  def parseJsCreateDomainRequest(json: JsValue): CreateDomainRequest = {
    CreateDomainRequest(
      (json \ "shortName").as[String],
      (json \ "fullName").as[String],
      (json \ "description").asOpt[String],
      (json \ "reportMetadata").asOpt[String],
      (json \ "apiKey").asOpt[String]
    )
  }

  def parseJsCreateCommunityRequest(json: JsValue): CreateCommunityRequest = {
    CreateCommunityRequest(
      (json \ "shortName").as[String],
      (json \ "fullName").as[String],
      (json \ "description").asOpt[String],
      (json \ "supportEmail").asOpt[String],
      (json \ "interactionNotifications").asOpt[Boolean],
      (json \ "apiKey").asOpt[String],
      (json \ "domain").asOpt[String]
    )
  }

  def parseJsUpdateDomainRequest(json: JsValue, domainApiKey: Option[String], communityApiKey: Option[String]): UpdateDomainRequest = {
    UpdateDomainRequest(
      domainApiKey,
      (json \ "shortName").asOpt[String],
      (json \ "fullName").asOpt[String],
      (json \ "description").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "reportMetadata").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      communityApiKey
    )
  }

  def parseJsUpdateCommunityRequest(json: JsValue, communityApiKey: String): UpdateCommunityRequest = {
    UpdateCommunityRequest(
      communityApiKey,
      (json \ "shortName").asOpt[String],
      (json \ "fullName").asOpt[String],
      (json \ "description").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "supportEmail").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "interactionNotifications").asOpt[Boolean],
      (json \ "domain").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
    )
  }

  def parseJsUpdateSpecificationGroupRequest(json: JsValue, groupApiKey: String, communityApiKey: String): UpdateSpecificationGroupRequest = {
    UpdateSpecificationGroupRequest(
      groupApiKey,
      (json \ "shortName").asOpt[String],
      (json \ "fullName").asOpt[String],
      (json \ "description").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "reportMetadata").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "displayOrder").asOpt[Short],
      communityApiKey
    )
  }

  def parseJsUpdateOrganisationRequest(json: JsValue, organisationApiKey: String, communityApiKey: String): UpdateOrganisationRequest = {
    UpdateOrganisationRequest(
      organisationApiKey,
      (json \ "shortName").asOpt[String],
      (json \ "fullName").asOpt[String],
      communityApiKey
    )
  }

  def parseJsUpdateSystemRequest(json: JsValue, systemApiKey: String, communityApiKey: String): UpdateSystemRequest = {
    UpdateSystemRequest(
      systemApiKey,
      (json \ "shortName").asOpt[String],
      (json \ "fullName").asOpt[String],
      (json \ "description").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "version").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      communityApiKey
    )
  }

  def parseJsUpdateSpecificationRequest(json: JsValue, specificationApiKey: String, communityApiKey: String): UpdateSpecificationRequest = {
    UpdateSpecificationRequest(
      specificationApiKey,
      (json \ "shortName").asOpt[String],
      (json \ "fullName").asOpt[String],
      (json \ "description").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "reportMetadata").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "hidden").asOpt[Boolean],
      (json \ "displayOrder").asOpt[Short],
      (json \ "group").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      communityApiKey
    )
  }

  def parseJsUpdateActorRequest(json: JsValue, actorApiKey: String, communityApiKey: String): UpdateActorRequest = {
    UpdateActorRequest(
      actorApiKey,
      (json \ "identifier").asOpt[String],
      (json \ "name").asOpt[String],
      (json \ "description").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "reportMetadata").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "default").asOpt[Boolean].map(Some(_)),
      (json \ "hidden").asOpt[Boolean],
      (json \ "displayOrder").asOpt[Short].map(x => if (x < 0) None else Some(x)),
      communityApiKey
    )
  }

  def parseJsStringValue(json: JsValue, propertyName: String): String = {
    (json \ propertyName).as[String]
  }

  def parseJsConfigurationRequest(json: JsValue): ConfigurationRequest = {
    ConfigurationRequest(
      domainProperties = parseJsDomainParameterConfigurationArray((json \ "domainProperties").asOpt[JsArray].getOrElse(JsArray.empty)),
      organisationProperties = parseJsPartyConfigurationArray((json \ "organisationProperties").asOpt[JsArray].getOrElse(JsArray.empty), "organisation"),
      systemProperties = parseJsPartyConfigurationArray((json \ "systemProperties").asOpt[JsArray].getOrElse(JsArray.empty), "system"),
      statementProperties = parseJsStatementConfigurationArray((json \ "statementProperties").asOpt[JsArray].getOrElse(JsArray.empty))
    )
  }

  private def parseJsStatementConfigurationArray(jsonArray: JsArray): List[StatementConfiguration] = {
    jsonArray.value.map { json =>
      StatementConfiguration(
        (json \ "system").as[String],
        (json \ "actor").as[String],
        parseJsKeyValueArray((json \ "properties").asOpt[JsArray].getOrElse(JsArray.empty))
      )
    }.toList
  }

  private def parseJsPartyConfigurationArray(jsonArray: JsArray, partyPropertyName: String): List[PartyConfiguration] = {
    jsonArray.value.map { json =>
      PartyConfiguration(
        (json \ partyPropertyName).as[String],
        parseJsKeyValueArray((json \ "properties").asOpt[JsArray].getOrElse(JsArray.empty))
      )
    }.toList
  }

  private def jsAllowedPropertyValue(keyValue: KeyValueRequired): JsObject = {
    Json.obj(
      "value" -> keyValue.key,
      "label" -> keyValue.value,
    )
  }

  def jsAllowedPropertyValues(keyValues: List[KeyValueRequired]): JsArray = {
    var json = Json.arr()
    keyValues.foreach{ keyValue =>
      json = json.append(jsAllowedPropertyValue(keyValue))
    }
    json
  }

  def parseJsCustomPropertyInfo(json: JsValue): CustomPropertyInfo = {
    CustomPropertyInfo(
      (json \ "key").as[String],
      (json \ "name").asOpt[String],
      (json \ "description").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "required").asOpt[Boolean],
      (json \ "editableByUsers").asOpt[Boolean],
      (json \ "inTests").asOpt[Boolean],
      (json \ "inExports").asOpt[Boolean],
      (json \ "inSelfRegistration").asOpt[Boolean],
      (json \ "hidden").asOpt[Boolean],
      (json \ "allowedValues").asOpt[JsArray].map(x => parseJsAllowedPropertyValues(x)).map(x => if (x.isEmpty) None else Some(x)),
      (json \ "displayOrder").asOpt[Short],
      (json \ "dependsOn").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "dependsOnValue").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "defaultValue").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x))
    )
  }

  def parseJsAllowedPropertyValues(jsonContent: String): List[KeyValueRequired] = {
    parseJsAllowedPropertyValues(Json.parse(jsonContent).as[JsArray])
  }

  private def parseJsAllowedPropertyValues(jsonArray: JsArray): List[KeyValueRequired] = {
    jsonArray.value.map { json =>
      KeyValueRequired(
        (json \ "value").as[String],
        (json \ "label").as[String]
      )
    }.toList
  }

  private def parseJsDomainParameterConfigurationArray(jsonArray: JsArray): List[DomainParameterInfo] = {
    jsonArray.value.map(x => parseJsDomainParameterConfiguration(x, None, None)).toList
  }

  def parseJsDomainParameterConfiguration(json: JsValue, domainApiKey: Option[String] = None, parameterKey: Option[String] = None): DomainParameterInfo = {
    DomainParameterInfo(
      KeyValue(
        parameterKey.getOrElse((json \ "key").as[String]),
        (json \ "value").asOpt[String]
      ),
      (json \ "description").asOpt[String].map(x => if (StringUtils.isBlank(x)) None else Some(x)),
      (json \ "inTests").asOpt[Boolean],
      domainApiKey.orElse((json \ "domain").asOpt[String])
    )
  }

  private def parseJsKeyValueArray(jsonArray: JsArray): List[KeyValue] = {
    jsonArray.value.map { json =>
      KeyValue(
        (json \ "key").as[String],
        (json \ "value").asOpt[String]
      )
    }.toList
  }

  def parseJsTestSuiteLinkRequest(json: JsValue): TestSuiteLinkRequest = {
    val testSuite = (json \ "testSuite").as[String]
    val specifications = parseJsTestSuiteLinkSpecificationInfo((json \ "specifications").asOpt[JsArray].getOrElse(JsArray.empty))
    TestSuiteLinkRequest(testSuite, specifications)
  }

  def parseJsTestSuiteUnlinkRequest(json: JsValue): TestSuiteUnlinkRequest = {
    val testSuite = (json \ "testSuite").as[String]
    val specifications = parseStringArray((json \ "specifications").as[JsArray])
    TestSuiteUnlinkRequest(testSuite, specifications)
  }

  private def parseJsTestSuiteLinkSpecificationInfo(jsArray: JsArray): List[TestSuiteLinkRequestSpecification] = {
    jsArray.value.map { json =>
      TestSuiteLinkRequestSpecification(
        (json \ "specification").as[String],
        (json \ "update").asOpt[Boolean].getOrElse(false)
      )
    }.toList
  }

  def jsTestSuiteLinkResponseSpecifications(results: List[TestSuiteLinkResponseSpecification]): JsArray = {
    var items = Json.arr()
    results.foreach { result =>
      var item = Json.obj(
        "specification" -> result.specificationKey,
        "linked" -> result.linked
      )
      if (result.message.isDefined) {
        item = item + ("message", JsString(result.message.get))
      }
      if (result.actors.isDefined && result.actors.get.nonEmpty) {
        item = item + ("actors", jsTestSuiteSpecificationActorApiKeys(result.actors.get))
      }
      items = items.append(item)
    }
    items
  }

  def parseJsTags(json: String): List[TestCaseTag] = {
    val tags = new ListBuffer[TestCaseTag]
    Json.parse(json).as[JsArray].value.foreach { value =>
      tags += TestCaseTag(
        (value \ "name").as[String],
        (value \ "description").asOpt[String],
        (value \ "foreground").asOpt[String],
        (value \ "background").asOpt[String]
      )
    }
    tags.toList
  }

  private def parseJsInputMapping(json: JsValue): InputMapping = {
    val testSuites: List[String] = (json \ "testSuite").asOpt[JsArray].getOrElse(JsArray.empty).value.map { jsValue =>
      jsValue.as[JsString].value
    }.toList
    val testCases: List[String] = (json \ "testCase").asOpt[JsArray].getOrElse(JsArray.empty).value.map { jsValue =>
      jsValue.as[JsString].value
    }.toList
    InputMapping(testSuites, testCases, parseJsAnyContent((json \ "input").get))
  }

  def parseJsProcessResponse(json: JsValue): ProcessResponse = {
    val response = new ProcessResponse
    response.getOutput.addAll((json \ "output").asOpt[JsArray].getOrElse(JsArray.empty).value.map { jsValue => parseJsAnyContent(jsValue)}.toList.asJavaCollection)
    response
  }

  def parseJsAnyContent(json: JsValue): AnyContent = {
    val anyContent = new AnyContent
    anyContent.setName((json \ "name").asOpt[String].orNull)
    var enumValue = (json \ "embeddingMethod").asOpt[String].getOrElse(ValueEmbeddingEnumeration.STRING.value())
    if (enumValue == "BASE_64") {
      enumValue = ValueEmbeddingEnumeration.BASE_64.value()
    }
    anyContent.setEmbeddingMethod(ValueEmbeddingEnumeration.fromValue(enumValue))
    anyContent.setValue((json \ "value").asOpt[String].orNull)
    anyContent.setType((json \ "type").asOpt[String].orNull)
    anyContent.setEncoding((json \ "encoding").asOpt[String].orNull)
    anyContent.getItem.addAll((json \ "item").asOpt[JsArray].getOrElse(JsArray.empty).value.map { jsValue => parseJsAnyContent(jsValue)}.toList.asJavaCollection)
    anyContent
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

  def parseJsPendingTestSuiteActions(json: String): List[TestSuiteDeploymentAction] = {
    val jsArray = Json.parse(json).as[JsArray].value
    val values = new ListBuffer[TestSuiteDeploymentAction]()
    jsArray.foreach { jsonConfig =>
      val pendingActionStr = (jsonConfig \ "action").as[String]
      var pendingAction: TestSuiteReplacementChoice = null
      var testCasesToReset: Option[List[TestCaseDeploymentAction]] = None
      if ("proceed".equals(pendingActionStr)) {
        pendingAction = TestSuiteReplacementChoice.PROCEED
        val testCaseActions = new ListBuffer[TestCaseDeploymentAction]()
        testCaseActions ++= (jsonConfig \ "testCaseUpdates").asOpt[List[JsValue]].getOrElse(List.empty).map { item =>
          new TestCaseDeploymentAction(
            (item \ "identifier").as[String],
            (item \ "updateDefinition").asOpt[Boolean],
            (item \ "resetTestHistory").asOpt[Boolean]
          )
        }
        testCasesToReset = Some(testCaseActions.toList)
      } else {
        // Cancel
        pendingAction = TestSuiteReplacementChoice.CANCEL
      }
      values += new TestSuiteDeploymentAction(
        (jsonConfig \ "specification").asOpt[Long],
        pendingAction,
        (jsonConfig \ "updateTestSuite").asOpt[Boolean].getOrElse(false),
        (jsonConfig \ "updateActors").asOpt[Boolean],
        (jsonConfig \ "sharedTestSuite").asOpt[Boolean].getOrElse(false),
        testCasesToReset
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

  def parseJsConfigs(json:String, systemId: Long): List[Configs] = {
    val jsArray = Json.parse(json).as[JsArray].value
    var list:List[Configs] = List()
    jsArray.foreach { jsonConfig =>
      val value = (jsonConfig \ "value").asOpt[String]
      list ::= Configs(
        systemId,
        (jsonConfig \ "parameter").as[Long],
        (jsonConfig \ "endpoint").as[Long],
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

  def parseJsTriggerFireExpressions(json:String, triggerId: Option[Long]): List[TriggerFireExpression] = {
    val jsArray = Json.parse(json).as[JsArray].value
    val list = ListBuffer[TriggerFireExpression]()
    jsArray.foreach { jsonConfig =>
      list += parseJsTriggerFireExpression(jsonConfig, triggerId)
    }
    list.toList
  }

  private def parseJsTriggerDataItem(jsonConfig: JsValue, triggerId: Option[Long]): TriggerData = {
    val dataType = (jsonConfig \ "dataType").as[Short]
    val dataTypeEnum = TriggerDataType.apply(dataType)
    var dataIdToUse: Long = -1
    if (dataTypeEnum == TriggerDataType.OrganisationParameter || dataTypeEnum == TriggerDataType.SystemParameter || dataTypeEnum == TriggerDataType.DomainParameter || dataTypeEnum == TriggerDataType.StatementParameter) {
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

  private def parseJsTriggerFireExpression(jsonConfig: JsValue, triggerId: Option[Long]): TriggerFireExpression = {
    TriggerFireExpression(
      (jsonConfig \ "id").asOpt[Long].getOrElse(0L),
      (jsonConfig \ "expression").as[String],
      TriggerFireExpressionType.apply((jsonConfig \ "expressionType").as[Short]).id.toShort,
      (jsonConfig \ "notMatch").asOpt[Boolean].getOrElse(false),
      triggerId.getOrElse(0L)
    )
  }

  def parseJsImportItems(json:String): List[ImportItem] = {
    val jsArray = Json.parse(json).as[JsArray].value
    val list = ListBuffer[ImportItem]()
    jsArray.foreach { jsonConfig =>
      list += parseJsImportItem(jsonConfig, None)
    }
    list.toList
  }

  def parseStringArray(json: Option[JsValue]): Option[List[String]] = {
    json.map(x => {
      x.as[JsArray].value.iterator.map { value =>
        value.as[JsString].value
      }.toList
    })
  }

  def parseStringArray(json: JsValue): List[String] = {
    json.as[JsArray].value.iterator.map { value =>
      value.as[JsString].value
    }.toList
  }

  def parseStringArray(json: String): List[String] = {
    parseStringArray(Json.parse(json))
  }

  def parseJsImportSettings(json:String):ImportSettings = {
    val jsonConfig = Json.parse(json).as[JsObject]
    val settings = new ImportSettings()
    settings.encryptionKey = (jsonConfig \ "encryptionKey").asOpt[String]
    settings.shortNameReplacement = (jsonConfig \ "shortNameReplacement").asOpt[String].filter(StringUtils.isNotBlank(_))
    settings.fullNameReplacement = (jsonConfig \ "fullNameReplacement").asOpt[String].filter(StringUtils.isNotBlank(_))
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

  def parseJsExportSettings(json:String, includeSettings: Boolean):ExportSettings = {
    val jsonConfig = Json.parse(json).as[JsObject]
    val settings = new ExportSettings()
    settings.communityAdministrators = (jsonConfig \ "communityAdministrators").as[Boolean]
    settings.landingPages = (jsonConfig \ "landingPages").as[Boolean]
    settings.errorTemplates = (jsonConfig \ "errorTemplates").as[Boolean]
    settings.legalNotices = (jsonConfig \ "legalNotices").as[Boolean]
    settings.triggers = (jsonConfig \ "triggers").as[Boolean]
    settings.resources = (jsonConfig \ "resources").as[Boolean]
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
    if (includeSettings) {
      settings.themes = (jsonConfig \ "themes").as[Boolean]
      settings.systemResources = (jsonConfig \ "systemResources").as[Boolean]
      settings.systemAdministrators = (jsonConfig \ "systemAdministrators").as[Boolean]
      settings.defaultLandingPages = (jsonConfig \ "defaultLandingPages").as[Boolean]
      settings.defaultLegalNotices = (jsonConfig \ "defaultLegalNotices").as[Boolean]
      settings.defaultErrorTemplates = (jsonConfig \ "defaultErrorTemplates").as[Boolean]
      settings.systemConfigurations = (jsonConfig \ "systemConfigurations").as[Boolean]
    } else {
      settings.themes = false
      settings.systemResources = false
      settings.systemAdministrators = false
      settings.defaultLandingPages = false
      settings.defaultLegalNotices = false
      settings.defaultErrorTemplates = false
      settings.systemConfigurations = false
    }
    // Deletions
    settings.communitiesToDelete = parseStringArray((jsonConfig \ "communitiesToDelete").asOpt[JsArray])
    settings.domainsToDelete = parseStringArray((jsonConfig \ "domainsToDelete").asOpt[JsArray])
    settings.encryptionKey = (jsonConfig \ "encryptionKey").asOpt[String]
    settings
  }

  def parseAllowedParameterValues(json: String): Map[String, Option[String]] = {
    val jsArray = Json.parse(json).as[JsArray].value
    val items = mutable.LinkedHashMap[String, Option[String]]()
    jsArray.foreach { jsItem =>
      items += ((jsItem \ "value").as[String] -> (jsItem \ "label").asOpt[String])
    }
    items.toMap
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
    val list = ListBuffer[UserInput]()
    jsArray.foreach { jsonInput =>
      val input = new UserInput()
      input.setId((jsonInput \ "id").as[String])
      input.setName((jsonInput \ "name").as[String])
      input.setType((jsonInput \ "type").as[String])
      input.setEmbeddingMethod(ValueEmbeddingEnumeration.fromValue((jsonInput \ "embeddingMethod").as[String]))
      val inputValue = (jsonInput \ "value").asOpt[String]
      input.setValue(inputValue.orNull)
      list += input
    }
    list.toList
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

  def parseJsConformanceCertificateSettings(json:String, communityId: Long): ConformanceCertificate = {
    val jsonConfig = Json.parse(json).as[JsObject]
    // Ensure the message content (if provided) is correctly sanitized.
    var certificateMessage = (jsonConfig \ "message").asOpt[String]
    if (certificateMessage.isDefined) {
      certificateMessage = Some(HtmlUtil.sanitizePdfContent(certificateMessage.get))
    }
    ConformanceCertificate(
      0L,
      (jsonConfig \ "title").asOpt[String],
      (jsonConfig \ "includeTitle").as[Boolean],
      (jsonConfig \ "includeMessage").as[Boolean],
      (jsonConfig \ "includeTestStatus").as[Boolean],
      (jsonConfig \ "includeTestCases").as[Boolean],
      (jsonConfig \ "includeDetails").as[Boolean],
      (jsonConfig \ "includeSignature").as[Boolean],
      (jsonConfig \ "includePageNumbers").as[Boolean],
      certificateMessage,
      communityId
    )
  }

  def parseJsConformanceOverviewCertificateMessage(json: JsValue, communityId: Long): ConformanceOverviewCertificateMessage = {
    val id = (json \ "id").asOpt[Long]
    val identifier = (json \ "identifier").asOpt[Long]
    val message = HtmlUtil.sanitizePdfContent((json \ "message").as[String])
    val level = OverviewLevelType.withName((json \ "level").as[String])
    ConformanceOverviewCertificateMessage(
      id.getOrElse(0L),
      level.id.toShort,
      message,
      if (level == OverviewLevelType.DomainLevel) identifier else None,
      if (level == OverviewLevelType.SpecificationGroupLevel) identifier else None,
      if (level == OverviewLevelType.SpecificationLevel) identifier else None,
      None,
      communityId
    )
  }

  def parseJsConformanceOverviewCertificateWithMessages(json:String, communityId: Long): ConformanceOverviewCertificateWithMessages = {
    val jsonConfig = Json.parse(json).as[JsObject]
    ConformanceOverviewCertificateWithMessages(
      ConformanceOverviewCertificate(
        0L,
        (jsonConfig \ "title").asOpt[String],
        (jsonConfig \ "includeTitle").as[Boolean],
        (jsonConfig \ "includeMessage").as[Boolean],
        (jsonConfig \ "includeTestStatus").as[Boolean],
        (jsonConfig \ "includeTestCases").as[Boolean],
        (jsonConfig \ "includeTestCaseDetails").as[Boolean],
        (jsonConfig \ "includeDetails").as[Boolean],
        (jsonConfig \ "includeSignature").as[Boolean],
        (jsonConfig \ "includePageNumbers").as[Boolean],
        (jsonConfig \ "enableAllLevel").asOpt[Boolean].getOrElse(false),
        (jsonConfig \ "enableDomainLevel").asOpt[Boolean].getOrElse(false),
        (jsonConfig \ "enableGroupLevel").asOpt[Boolean].getOrElse(false),
        (jsonConfig \ "enableSpecificationLevel").asOpt[Boolean].getOrElse(false),
        communityId
      ),
      (jsonConfig \ "messages").asOpt[JsArray].getOrElse(JsArray.empty).value.map { jsValue => parseJsConformanceOverviewCertificateMessage(jsValue, communityId)}.toList
    )
  }

  /**
   * Converts a TestCase object into Play!'s JSON notation.
   * @param testCase TestCase object to be converted
   * @return JsObject
   */
  def jsTestCases(testCase:TestCases, withDocumentation: Boolean, withTags: Boolean, withSpecReference: Boolean) : JsObject = {
    var json = Json.obj(
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
      "hasDocumentation" -> testCase.hasDocumentation,
      "optional" -> testCase.isOptional,
      "disabled" -> testCase.isDisabled,
      "group" -> (if(testCase.group.isDefined) testCase.group.get else JsNull)
    )
    if (withDocumentation && testCase.documentation.isDefined) json = json + ("documentation" -> JsString(testCase.documentation.get))
    if (withTags && testCase.tags.isDefined) json = json + ("tags" -> JsString(testCase.tags.get))
    if (withSpecReference) {
      if (testCase.specReference.isDefined) json = json + ("specReference" -> JsString(testCase.specReference.get))
      if (testCase.specDescription.isDefined) json = json + ("specDescription" -> JsString(testCase.specDescription.get))
      if (testCase.specLink.isDefined) json = json + ("specLink" -> JsString(testCase.specLink.get))
    }
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
      "hasDocumentation"  -> testCase.hasDocumentation,
      "optional"  -> testCase.isOptional,
      "disabled"  -> testCase.isDisabled
    )
    json
  }

  /**
   * Converts a List of TestCases into Play!'s JSON notation
   * @param list List of TestCases to be converted
   * @return JsArray
   */
  def jsTestCasesList(list:List[TestCases], withSpecReference: Boolean = false, withTags: Boolean = false):JsArray = {
    var json = Json.arr()
    list.foreach{ testCase =>
      json = json.append(jsTestCases(testCase, withDocumentation = false, withTags, withSpecReference = withSpecReference))
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
  def jsTestResult(testResult:TestResult, tpl:Option[String], withOutputMessage:Boolean):JsObject = {
    val json = Json.obj(
      "sessionId" -> testResult.sessionId,
      "systemId"  -> (if (testResult.systemId.isDefined) testResult.systemId else JsNull),
      "actorId"   -> (if (testResult.actorId.isDefined) testResult.actorId else JsNull),
      "testId"    -> (if (testResult.testCaseId.isDefined) testResult.testCaseId else JsNull),
      "specificationId"    -> (if (testResult.specificationId.isDefined) testResult.specificationId else JsNull),
      "result"    -> testResult.result,
      "startTime" -> TimeUtil.serializeTimestamp(testResult.startTime),
      "endTime"   -> (if(testResult.endTime.isDefined) TimeUtil.serializeTimestamp(testResult.endTime.get) else JsNull),
      "tpl"       -> (if(tpl.isDefined) tpl.get else JsNull),
      "outputMessage" -> (if (withOutputMessage && testResult.outputMessage.isDefined) testResult.outputMessage.get else JsNull),
      "obsolete"  -> (if (testResult.testSuiteId.isDefined && testResult.testCaseId.isDefined && testResult.systemId.isDefined && testResult.organizationId.isDefined && testResult.communityId.isDefined && testResult.domainId.isDefined && testResult.specificationId.isDefined && testResult.actorId.isDefined) false else true)
    )
    json
  }

  def jsTestResultReports(list: Iterable[TestResult], resultCount: Option[Int]): JsObject = {
    var json = Json.arr()
    list.foreach { report =>
      json = json.append(jsTestResultReport(report, None))
    }
    val jsonResult = Json.obj(
      "data" -> json,
      "count" -> (if (resultCount.isDefined) resultCount.get else JsNull)
    )
    jsonResult
  }

  def jsTestResultSessionReports(list: Iterable[TestResult], parameterInfo: Option[ParameterInfo], resultCount: Option[Int]): JsObject = {
    var json = Json.arr()
    list.foreach { report =>
      json = json.append(jsTestResultReport(report, parameterInfo))
    }
    var jsonResult = Json.obj(
      "data" -> json,
      "count" -> (if (resultCount.isDefined) resultCount.get else JsNull)
    )
    if (parameterInfo.isDefined) {
      var orgParameters = Json.arr()
      parameterInfo.get.orgDefinitions.foreach{ param =>
        orgParameters = orgParameters.append(JsString(param.testKey))
      }
      jsonResult = jsonResult.+("orgParameters" -> orgParameters)
      var sysParameters = Json.arr()
      parameterInfo.get.sysDefinitions.foreach{ param =>
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

  def jsOrgParameterValuesForExport(organisationId: Long, orgParameterDefinitions: List[OrganisationParameters], orgParameterValues: Map[Long, Map[Long, String]]): JsObject = {
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

  def jsSysParameterValuesForExport(systemId: Long, sysParameterDefinitions: List[SystemParameters], sysParameterValues: Map[Long, Map[Long, String]]): JsObject = {
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

  def jsTestResultReport(result: TestResult,
                         parameterInfo: Option[ParameterInfo],
                         withOutputMessage: Boolean = false,
                         logEntries: Option[List[String]] = None,
                         pendingInteractions: Option[List[TestInteraction]] = None
                        ): JsObject = {
    var json = Json.obj(
      "result" -> jsTestResult(result, None, withOutputMessage),
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
          "parameters" -> (if (result.organizationId.isDefined && parameterInfo.isDefined) jsOrgParameterValuesForExport(result.organizationId.get, parameterInfo.get.orgDefinitions, parameterInfo.get.orgValues) else JsNull)
        )
      },
      "system" -> {
        Json.obj(
          "id"    -> (if (result.systemId.isDefined) result.systemId.get else JsNull),
          "sname" -> (if (result.system.isDefined) result.system.get else JsNull),
          "owner" -> (if (result.organizationId.isDefined) result.organizationId.get else JsNull),
          "parameters" -> (if (result.systemId.isDefined && parameterInfo.isDefined) jsSysParameterValuesForExport(result.systemId.get, parameterInfo.get.sysDefinitions, parameterInfo.get.sysValues) else JsNull)
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
    if (logEntries.isDefined) {
      json = json + ("logs", jsStringArray(logEntries.get))
    }
    if (pendingInteractions.isDefined) {
      json = json + ("interactions", jsTestInteractions(pendingInteractions.get))
    }
    json
  }

  def jsTestInteractions(interactions: List[TestInteraction]): JsArray = {
    var array = Json.arr()
    interactions.foreach{ interaction =>
      array = array.append(Json.parse(interaction.tpl))
    }
    array
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

  def jsBreadcrumbLabelResponse(labels: BreadcrumbLabelResponse): JsObject = {
    var json = Json.obj()
    if (labels.domain.isDefined) json = json + ("domain" -> JsString(labels.domain.get))
    if (labels.specificationGroup.isDefined) json = json + ("specificationGroup" -> JsString(labels.specificationGroup.get))
    if (labels.specification.isDefined) json = json + ("specification" -> JsString(labels.specification.get))
    if (labels.actor.isDefined) json = json + ("actor" -> JsString(labels.actor.get))
    if (labels.community.isDefined) json = json + ("community" -> JsString(labels.community.get))
    if (labels.organisation.isDefined) json = json + ("organisation" -> JsString(labels.organisation.get))
    if (labels.system.isDefined) json = json + ("system" -> JsString(labels.system.get))
    json
  }

  def serializeConfigurationProperties(config: util.HashMap[String, String]):JsObject = {
    val json = Json.obj(
      "emailEnabled" -> config.get("email.enabled").toBoolean,
      "emailContactFormEnabled" -> config.get("email.contactFormEnabled").toBoolean,
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
      "mode" -> config.get("mode"),
      "automationApiEnabled" -> config.get("automationApi.enabled").toBoolean,
      "versionNumber" -> config.get("versionNumber"),
      "hasDefaultLegalNotice" -> config.get("hasDefaultLegalNotice").toBoolean,
      "conformanceStatementReportMaxTestCases" -> config.get("conformanceStatementReportMaxTestCases").toInt
    )
    json
  }

  /**
   * Converts an Organization object into a JSON string with its complex objects
   * @param org Organization object to be converted
   * @return String
   */
  def serializeOrganization(org:Organization):String = {
    // Serialize Organization
    var jOrganization:JsObject = jsOrganization(org.toCaseObject)
    org.landingPageObj.foreach(x => jOrganization = jOrganization ++ Json.obj("landingPages" -> jsLandingPage(x.toLandingPage())))
    org.legalNoticeObj.foreach(x => jOrganization = jOrganization ++ Json.obj("legalNotices" -> jsLegalNotice(x.toLegalNotice())))
    org.errorTemplateObj.foreach(x => jOrganization = jOrganization ++ Json.obj("errorTemplates" -> jsErrorTemplate(x.toErrorTemplate())))
    jOrganization = jOrganization ++ Json.obj("communityLegalNoticeAppliesAndExists" -> org.communityLegalNoticeAppliesAndExists)
    // Return JSON String
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
  def jsLandingPage(landingPage: LandingPage):JsObject = {
    var json = Json.obj(
      "id"    -> landingPage.id,
      "name"  -> landingPage.name,
      "default" -> landingPage.default
    )
    landingPage.description.foreach(x => json += ("description" -> JsString(x)))
    landingPage.content.foreach(x => json += ("content" -> JsString(x)))
    json
  }

  /**
   * Converts a LegalNotice object into Play!'s JSON notation.
   * Does not support cross object conversion
   * @param legalNotice LegalNotice object to be converted
   * @return JsObject
   */
  def jsLegalNotice(legalNotice: LegalNotice):JsObject = {
    var json = Json.obj(
      "id"    -> legalNotice.id,
      "name"  -> legalNotice.name,
      "default" -> legalNotice.default
    )
    legalNotice.description.foreach(x => json += ("description" -> JsString(x)))
    legalNotice.content.foreach(x => json += ("content" -> JsString(x)))
    json
  }

  /**
    * Converts a ErrorTemplate object into Play!'s JSON notation.
    * Does not support cross object conversion
    * @param errorTemplate ErrorTemplate object to be converted
    * @return JsObject
    */
  def jsErrorTemplate(errorTemplate:ErrorTemplate):JsObject = {
    var json = Json.obj(
      "id"    -> errorTemplate.id,
      "name"  -> errorTemplate.name,
      "default" -> errorTemplate.default
    )
    errorTemplate.description.foreach(x => json += ("description" -> JsString(x)))
    errorTemplate.content.foreach(x => json += ("content" -> JsString(x)))
    json
  }

  /**
   * Converts a List of LandingPages into Play!'s JSON notation
   * Does not support cross object conversion
   * @param list List of LandingPages to be convert
   * @return JsArray
   */
  def jsLandingPages(list: List[LandingPage]): JsArray = {
    var json = Json.arr()
    list.foreach { landingPage =>
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
      "serviceType" -> trigger.serviceType,
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

  def jsTriggerFireExpression(item:TriggerFireExpression):JsObject = {
    val json = Json.obj(
      "id"  -> item.id,
      "expression"  -> item.expression,
      "expressionType"  -> item.expressionType,
      "notMatch" -> item.notMatch
    )
    json
  }

  def jsTriggerFireExpressions(items:List[TriggerFireExpression]):JsArray = {
    var json = Json.arr()
    items.foreach { item =>
      json = json.append(jsTriggerFireExpression(item))
    }
    json
  }

  def jsTriggerInfo(trigger: Trigger): JsObject = {
    val json = Json.obj(
      "trigger"    -> jsTrigger(trigger.trigger),
      "data"  -> (if (trigger.data.isDefined) jsTriggerDataItems(trigger.data.get) else JsNull),
      "fireExpressions"  -> (if (trigger.fireExpressions.isDefined) jsTriggerFireExpressions(trigger.fireExpressions.get) else JsNull)
    )
    json
  }

  /**
   * Converts a List of LegalNotices into Play!'s JSON notation
   * Does not support cross object conversion
   * @param list List of LegalNotices to be convert
   * @return JsArray
   */
  def jsLegalNotices(list:List[LegalNotice]):JsArray = {
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
  def jsErrorTemplates(list:List[ErrorTemplate]):JsArray = {
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
  def serializeLandingPage(landingPage: Option[LandingPages]):String = {
    var jLandingPage: JsObject = jsExists(landingPage.isDefined)
    landingPage.foreach(x => jLandingPage = jLandingPage ++ jsLandingPage(x.toLandingPage()))
    jLandingPage.toString
  }

  /**
   * Converts a LegalNotice object into a JSON string with its complex objects
   * @param legalNotice LegalNotice object to be converted
   * @return String
   */
  def serializeLegalNotice(legalNotice: Option[LegalNotices]):String = {
    var jLegalNotice: JsObject = jsExists(legalNotice.isDefined)
    legalNotice.foreach(x => jLegalNotice = jLegalNotice ++ jsLegalNotice(x.toLegalNotice()))
    jLegalNotice.toString
  }

  /**
    * Converts a ErrorTemplate object into a JSON string with its complex objects
    * @param errorTemplate ErrorTemplate object to be converted
    * @return String
    */
  def serializeErrorTemplate(errorTemplate: Option[ErrorTemplates]):String = {
    var jErrorTemplate: JsObject = jsExists(errorTemplate.isDefined)
    errorTemplate.foreach(x => jErrorTemplate = jErrorTemplate ++ jsErrorTemplate(x.toErrorTemplate()))
    jErrorTemplate.toString
  }

  def jsReportSettings(settings: CommunityReportSettings, stylesheetExists: Boolean): JsObject = {
    var json = Json.obj(
    "stylesheetExists" -> stylesheetExists,
    "signPdfs" -> settings.signPdfs,
    "customPdfs" -> settings.customPdfs,
    "customPdfsWithCustomXml" -> settings.customPdfsWithCustomXml
    )
    if (settings.customPdfService.isDefined) {
      json += ("customPdfService" -> JsString(settings.customPdfService.get))
    }
    json
  }

  def jsExists(bool:Boolean):JsObject = {
    val json = Json.obj(
      "exists" -> bool
    )
    json
  }

  private def jsTestSuiteUploadItemResult(item: TestSuiteUploadItemResult):JsObject = {
    val json = Json.obj(
      "name" -> item.itemName,
      "type" -> item.itemType,
      "action" -> item.actionType,
      "specification" -> item.specification
    )
    json
  }

  private def jsTestSuiteUploadItemResults(items: Iterable[TestSuiteUploadItemResult]):JsArray = {
    var json = Json.arr()
    for (item <- items) {
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

  def jsConformanceSnapshotList(latestLabel: Option[String], snapshots: Iterable[ConformanceSnapshot], public: Boolean, withApiKeys: Boolean): JsObject = {
    var json = Json.obj(
      "snapshots" -> jsConformanceSnapshots(snapshots, public, withApiKeys)
    )
    if (latestLabel.isDefined) {
      json += ("latest" -> JsString(latestLabel.get))
    }
    json
  }

  private def jsConformanceSnapshots(snapshots: Iterable[ConformanceSnapshot], public: Boolean, withApiKeys: Boolean): JsArray = {
    var array = Json.arr()
    snapshots.foreach { snapshot =>
      array = array.append(jsConformanceSnapshot(snapshot, public, withApiKeys))
    }
    array
  }

  def jsConformanceSnapshot(snapshot: ConformanceSnapshot, public: Boolean, withApiKey: Boolean):JsObject = {
    var json = Json.obj(
      "id" -> snapshot.id,
      "snapshotTime" -> TimeUtil.serializeTimestamp(snapshot.snapshotTime)
    )
    if (withApiKey) {
      json += ("apiKey" -> JsString(snapshot.apiKey))
    }
    if (public) {
      json += ("label" -> JsString(snapshot.publicLabel.getOrElse(snapshot.label)))
    } else {
      json += ("label" -> JsString(snapshot.label))
      if (snapshot.publicLabel.isDefined) {
        json += ("publicLabel" -> JsString(snapshot.publicLabel.get))
      }
      json += ("hidden" -> JsBoolean(!snapshot.isPublic))
    }
    json
  }

  def jsEndpointId(endpointId: Long): JsObject = {
    Json.obj("endpoint" -> endpointId)
  }

  def jsApiKey(apiKey: String): JsObject = {
    Json.obj("apiKey" -> apiKey)
  }

  private def toJsonString(value: Option[String]): JsValue = {
    value.map(JsString).getOrElse(JsNull)
  }

  def jsTestSuiteUploadResult(result: TestSuiteUploadResult):JsObject = {
    val json = Json.obj(
      "success"    -> result.success,
      "errorInformation"  -> toJsonString(result.errorInformation),
      "pendingFolderId"  -> toJsonString(result.pendingTestSuiteFolderName),
      "existsForSpecs" -> testSuiteExistsForSpec(result.existsForSpecs),
      "matchingDataExists" -> toJsArray(result.matchingDataExists),
      "items" -> (if (result.items.isDefined) jsTestSuiteUploadItemResults(result.items.get) else JsNull),
      "validationReport" -> (if (result.validationReport.isDefined) jsTAR(result.validationReport.get) else JsNull),
      "needsConfirmation" -> result.needsConfirmation,
      "testCases" -> (if (result.testCases.isDefined) jsTestSuiteUploadSpecificationTestCases(result.testCases.get) else JsNull),
      "sharedTestSuiteId" -> (if (result.sharedTestSuiteId.isDefined) result.sharedTestSuiteId.get else JsNull),
      "sharedTestCases" -> (if (result.sharedTestCases.isDefined) jsTestSuiteUploadTestCases(result.sharedTestCases.get) else JsNull),
      "updateMetadata" -> result.updateMetadata,
      "updateSpecification" -> result.updateSpecification
    )
    json
  }

  private def testSuiteExistsForSpec(info: Option[List[(Long, Boolean)]]): JsArray = {
    var json = Json.arr()
    if (info.isDefined) {
      info.get.foreach { value =>
        json = json.append(Json.obj(
          "id" -> value._1,
          "shared" -> value._2
        ))
      }
    }
    json
  }

  private def jsTestSuiteUploadSpecificationTestCases(specTestCases: Map[Long, List[TestSuiteUploadTestCase]]): JsArray = {
    var specArray = Json.arr()
    specTestCases.foreach { entry =>
      specArray = specArray.append(Json.obj(
        "specification" -> entry._1,
        "testCases" -> jsTestSuiteUploadTestCases(entry._2)
      ))
    }
    specArray
  }

  private def jsTestSuiteUploadTestCases(testCases: List[TestSuiteUploadTestCase]): JsArray = {
    var testCaseArray = Json.arr()
    testCases.foreach { testCase =>
      testCaseArray = testCaseArray.append(Json.obj(
        "identifier" -> testCase.identifier,
        "name" -> testCase.name,
        "status" -> testCase.matchType.id,
        "updateMetadata" -> testCase.updateMetadata,
        "resetTestHistory" -> testCase.resetTestHistory
      ))
    }
    testCaseArray
  }

  def jsTestSuiteDeployInfo(resultWithKeys: TestSuiteUploadResultWithApiKeys):JsObject = {
    var errors = Json.arr()
    var warnings = Json.arr()
    var messages = Json.arr()
    if (resultWithKeys.result.existsForSpecs.nonEmpty) {
      // Non-shared test suite that we tried to deploy to a specification with a matching (by identifier) shared test suite.
      messages = messages.append(Json.obj("description" -> "The specification contains a shared test suite with the same identifier. Deployment was skipped."))
    }
    if (resultWithKeys.result.validationReport.exists(_.getReports != null)) {
      resultWithKeys.result.validationReport.get.getReports.getInfoOrWarningOrError.asScala.toList.foreach(item => {
        var itemJson = Json.obj("description" -> item.getValue.asInstanceOf[BAR].getDescription)
        if (item.getValue.asInstanceOf[BAR].getLocation != null) {
          itemJson = itemJson.+("location", JsString(item.getValue.asInstanceOf[BAR].getLocation))
        }
        if (item.getName.getLocalPart == "error") {
          errors = errors.append(itemJson)
        } else if (item.getName.getLocalPart == "warning") {
          warnings = warnings.append(itemJson)
        } else {
          messages = messages.append(itemJson)
        }
      })
    }
    var json = Json.obj(
      "completed" -> resultWithKeys.result.success
    )
    if (errors.value.nonEmpty) {
      json = json.+("errors", errors)
    }
    if (warnings.value.nonEmpty) {
      json = json.+("warnings", warnings)
    }
    if (messages.value.nonEmpty) {
      json = json.+("messages", messages)
    }
    // API key identifiers.
    if (resultWithKeys.testSuiteIdentifier.isDefined) {
      var identifiers = Json.obj(
        "testSuite" -> resultWithKeys.testSuiteIdentifier.get
      )
      if (resultWithKeys.testCaseIdentifiers.isDefined && resultWithKeys.testCaseIdentifiers.get.nonEmpty) {
        identifiers = identifiers+("testCases" -> jsStringArray(resultWithKeys.testCaseIdentifiers.get))
      }
      if (resultWithKeys.specifications.isDefined && resultWithKeys.specifications.get.nonEmpty) {
        identifiers = identifiers+("specifications" -> jsTestSuiteSpecificationApiKeys(resultWithKeys.specifications.get))
      }
      json = json+("identifiers", identifiers)
    }
    json
  }

  private def jsTestSuiteSpecificationApiKeys(specifications: Iterable[SpecificationActorApiKeys]): JsArray = {
    var specArray = Json.arr()
    specifications.foreach { spec =>
      var specJson = Json.obj(
        "name" -> spec.specificationName,
        "identifier" -> spec.specificationApiKey,
      )
      if (spec.actors.isDefined && spec.actors.get.nonEmpty) {
        specJson = specJson + ("actors" -> jsTestSuiteSpecificationActorApiKeys(spec.actors.get))
      }
      specArray = specArray.append(specJson)
    }
    specArray
  }

  private def jsTestSuiteSpecificationActorApiKeys(actors: Iterable[KeyValueRequired]): JsArray = {
    var actorArray = Json.arr()
    actors.foreach { actor =>
      actorArray = actorArray.append(Json.obj(
        "name" -> actor.key,
        "identifier" -> actor.value
      ))
    }
    actorArray
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

  def jsConformanceTestCases(testCases: Iterable[ConformanceTestCase]): JsArray = {
    var json = Json.arr()
    testCases.foreach { testCase =>
      json = json.append(Json.obj(
        "id" -> testCase.id,
        "sname" -> testCase.name,
        "description" -> (if (testCase.description.isDefined) testCase.description.get else JsNull),
        "outputMessage" -> (if (testCase.outputMessage.isDefined) testCase.outputMessage.get else JsNull),
        "sessionId" -> (if (testCase.sessionId.isDefined) testCase.sessionId.get else JsNull),
        "updateTime" -> (if (testCase.updateTime.isDefined) TimeUtil.serializeTimestamp(testCase.updateTime.get) else JsNull),
        "hasDocumentation" -> testCase.hasDocumentation,
        "optional" -> testCase.optional,
        "disabled" -> testCase.disabled,
        "result" -> testCase.result.value(),
        "tags" -> (if (testCase.tags.isDefined) testCase.tags.get else JsNull),
        "specReference" -> (if (testCase.specReference.isDefined) testCase.specReference.get else JsNull),
        "specDescription" -> (if (testCase.specDescription.isDefined) testCase.specDescription.get else JsNull),
        "specLink" -> (if (testCase.specLink.isDefined) testCase.specLink.get else JsNull),
        "group" -> (if (testCase.group.isDefined) testCase.group.get else JsNull)
      ))
    }
    json
  }

  def jsConformanceTestSuites(testSuites: Iterable[ConformanceTestSuite]): JsArray = {
    var json = Json.arr()
    testSuites.foreach { testSuite =>
      var obj = Json.obj(
        "id" -> testSuite.id,
        "sname" -> testSuite.name,
        "description" -> (if (testSuite.description.isDefined) testSuite.description.get else JsNull),
        "hasDocumentation" -> testSuite.hasDocumentation,
        "specReference" -> (if (testSuite.specReference.isDefined) testSuite.specReference.get else JsNull),
        "specDescription" -> (if (testSuite.specDescription.isDefined) testSuite.specDescription.get else JsNull),
        "specLink" -> (if (testSuite.specLink.isDefined) testSuite.specLink.get else JsNull),
        "result" -> testSuite.result.value(),
        "testCases" -> jsConformanceTestCases(testSuite.testCases)
      )
      if (testSuite.testCaseGroups.nonEmpty) {
        obj = obj + ("testCaseGroups" -> jsTestCaseGroups(testSuite.testCaseGroups))
      }
      json = json.append(obj)
    }
    json
  }

  def jsTestCaseGroups(testCaseGroups: Iterable[TestCaseGroup]): JsArray = {
    var json = Json.arr()
    testCaseGroups.foreach { group =>
      json = json.append(jsTestCaseGroup(group))
    }
    json
  }

  def jsTestCaseGroup(group: TestCaseGroup): JsObject = {
    var obj = Json.obj(
      "id" -> group.id
    )
    if (group.name.isDefined) obj = obj + ("name" -> JsString(group.name.get))
    if (group.description.isDefined) obj = obj + ("description" -> JsString(group.description.get))
    obj
  }

  def jsTags(tags: Iterable[TestCaseTag]): JsArray = {
    var json = Json.arr()
    tags.foreach { tag =>
      var tagJson = Json.obj("name" -> tag.name)
      if (tag.description.isDefined) tagJson = tagJson + ("description" -> JsString(tag.description.get))
      if (tag.foreground.isDefined) tagJson = tagJson + ("foreground" -> JsString(tag.foreground.get))
      if (tag.background.isDefined) tagJson = tagJson + ("background" -> JsString(tag.background.get))
      json = json.append(tagJson)
    }
    json
  }

  def jsConformanceStatement(statement: ConformanceStatementItem, results: models.ConformanceStatus, systemInfo: models.System): JsObject = {
    Json.obj(
      "statement" -> jsConformanceStatementItem(statement),
      "results" -> jsConformanceStatus(results),
      "system" -> jsSystem(systemInfo.toCaseObject),
      "organisation" -> jsOrganization(systemInfo.owner.get) // This is always present.
    )
  }

  def jsConformanceStatus(status: ConformanceStatus): JsObject = {
    val json = Json.obj(
      "summary" -> Json.obj(
        "failed"    -> status.failed,
        "completed"    -> status.completed,
        "undefined"    -> status.undefined,
        "failedOptional" -> status.failedOptional,
        "completedOptional" -> status.completedOptional,
        "undefinedOptional" -> status.undefinedOptional,
        "failedToConsider"    -> status.failedToConsider,
        "completedToConsider"    -> status.completedToConsider,
        "undefinedToConsider"    -> status.undefinedToConsider,
        "result" -> status.result.value(),
        "hasBadge" -> status.hasBadge,
        "updateTime" -> (if (status.updateTime.isDefined) TimeUtil.serializeTimestamp(status.updateTime.get) else JsNull),
        "systemId" -> status.systemId,
        "domainId" -> status.domainId,
        "specificationId" -> status.specificationId,
        "actorId" -> status.actorId
      ),
      "testSuites" -> jsConformanceTestSuites(status.testSuites)
    )
    json
  }

  def jsConformanceResultFullList(list: List[ConformanceStatementFull], parameterInfo: Option[ParameterInfo], count: Int): JsObject = {
    var json = Json.arr()
    list.foreach{ info =>
      json = json.append(jsConformanceResultFull(info, parameterInfo))
    }
    var jsonResult = Json.obj(
      "data" -> json,
      "count" -> count
    )
    if (parameterInfo.isDefined) {
      var orgParameters = Json.arr()
      parameterInfo.get.orgDefinitions.foreach{ param =>
        orgParameters = orgParameters.append(JsString(param.testKey))
      }
      jsonResult = jsonResult.+("orgParameters" -> orgParameters)
      var sysParameters = Json.arr()
      parameterInfo.get.sysDefinitions.foreach{ param =>
        sysParameters = sysParameters.append(JsString(param.testKey))
      }
      jsonResult = jsonResult.+("sysParameters" -> sysParameters)
    }
    jsonResult
  }

  def jsConformanceResultFull(item: ConformanceStatementFull,
                              parameterInfo: Option[ParameterInfo]): JsObject = {
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
      "specGroupName"    -> (if(item.specificationGroupName.isDefined) item.specificationGroupName.get else JsNull),
      "specGroupOptionName"    -> item.specificationGroupOptionName,
      "actorId"    -> item.actorId,
      "actorName"    -> item.actorFull,
      "testSuiteName" -> item.testSuiteName,
      "testCaseName" -> item.testCaseName,
      "testCaseDescription" -> item.testCaseDescription,
      "failed"    -> item.failedTests,
      "completed"    -> item.completedTests,
      "undefined"    -> item.undefinedTests,
      "failedOptional" -> item.failedOptionalTests,
      "completedOptional" -> item.completedOptionalTests,
      "undefinedOptional" -> item.undefinedOptionalTests,
      "failedToConsider"    -> item.failedTestsToConsider,
      "completedToConsider"    -> item.completedTestsToConsider,
      "undefinedToConsider"    -> item.undefinedTestsToConsider,
      "result" -> item.result,
      "updateTime" -> (if(item.updateTime.isDefined) TimeUtil.serializeTimestamp(item.updateTime.get) else JsNull),
      "outputMessage" -> item.outputMessage
    )
    if (parameterInfo.isDefined) {
      parameterInfo.get.orgDefinitions.foreach{ param =>
        if (parameterInfo.get.orgValues.contains(item.organizationId)) {
          val value = parameterInfo.get.orgValues(item.organizationId).get(param.id)
          if (value.isDefined) {
            json = json.+("orgparam_"+param.testKey -> JsString(value.get))
          }
        }
      }
      parameterInfo.get.sysDefinitions.foreach{ param =>
        if (parameterInfo.get.sysValues.contains(item.systemId)) {
          val value = parameterInfo.get.sysValues(item.systemId).get(param.id)
          if (value.isDefined) {
            json = json.+("sysparam_"+param.testKey -> JsString(value.get))
          }
        }
      }
    }
    json
  }

  def jsCommunityKeystore(keystoreType: String): JsObject = {
    Json.obj(
      "keystoreType" -> keystoreType
    )
  }

  def jsConformanceSettings(settings: ConformanceCertificate): JsObject = {
    Json.obj(
      "title" -> (if(settings.title.isDefined) settings.title.get else JsNull),
      "message" -> (if(settings.message.isDefined) settings.message.get else JsNull),
      "includeTitle" -> settings.includeTitle,
      "includeMessage" -> settings.includeMessage,
      "includeTestStatus" -> settings.includeTestStatus,
      "includeTestCases" -> settings.includeTestCases,
      "includeDetails" -> settings.includeDetails,
      "includeSignature" -> settings.includeSignature,
      "includePageNumbers" -> settings.includePageNumbers,
      "community" -> settings.community
    )
  }

  def jsConformanceOverviewCertificateMessages(data: Iterable[ConformanceOverviewCertificateMessage]): JsArray = {
    var json = Json.arr()
    data.foreach { item =>
      json = json.append(jsConformanceOverviewCertificateMessage(item))
    }
    json
  }

  def jsConformanceOverviewCertificateMessage(data: ConformanceOverviewCertificateMessage): JsObject = {
    var level: Option[String] = None
    var identifier: Option[Long] = None
    if (data.messageType == OverviewLevelType.DomainLevel.id) {
      level = Some("domain")
      identifier = data.domain
    } else if (data.messageType == OverviewLevelType.SpecificationGroupLevel.id) {
      level = Some("group")
      identifier = data.group
    } else if (data.messageType == OverviewLevelType.SpecificationLevel.id) {
      level = Some("specification")
      identifier = data.specification
    } else {
      level = Some("all")
    }
    var json = Json.obj(
      "id" -> data.id,
      "level" -> level.get,
      "message" -> data.message
    )
    if (identifier.isDefined) {
      json = json + ("identifier" -> JsNumber(identifier.get))
    }
    json
  }

  def jsConformanceOverviewSettings(data: ConformanceOverviewCertificateWithMessages): JsObject = {
    var json = Json.obj(
      "title" -> (if(data.settings.title.isDefined) data.settings.title.get else JsNull),
      "includeTitle" -> data.settings.includeTitle,
      "includeMessage" -> data.settings.includeMessage,
      "includeTestStatus" -> data.settings.includeStatementStatus,
      "includeTestCases" -> data.settings.includeStatements,
      "includeTestCaseDetails" -> data.settings.includeStatementDetails,
      "includeDetails" -> data.settings.includeDetails,
      "includeSignature" -> data.settings.includeSignature,
      "includePageNumbers" -> data.settings.includePageNumbers,
      "enableAllLevel" -> data.settings.enableAllLevel,
      "enableDomainLevel" -> data.settings.enableDomainLevel,
      "enableGroupLevel" -> data.settings.enableGroupLevel,
      "enableSpecificationLevel" -> data.settings.enableSpecificationLevel,
      "community" -> data.settings.community
    )
    if (data.messages.nonEmpty) {
      json = json + ("messages" -> jsConformanceOverviewCertificateMessages(data.messages))
    }
    json
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

  def jsParametersWithValues(list: List[ParametersWithValue], includeValues: Boolean):JsArray = {
    var json = Json.arr()
    list.foreach { parameter =>
      json = json.append(jsParametersWithValue(parameter, includeValues))
    }
    json
  }

  def jsParametersWithValue(param: ParametersWithValue, includeValues: Boolean): JsObject = {
    var json = jsParameter(param.parameter)
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

  def jsStatementParametersMinimal(items: List[StatementParameterMinimal]): JsArray = {
    var json = Json.arr()
    items.foreach { item =>
      json = json.append(Json.obj(
        "id" -> item.id,
        "name" -> item.name,
        "testKey" -> item.testKey,
        "kind" -> item.kind
      ))
    }
    json
  }

  def jsProcessRequest(processRequest: ProcessRequest): JsObject = {
    var json = Json.obj()
    if (processRequest.getOperation != null) {
      json += ("operation" -> JsString(processRequest.getOperation))
    }
    if (processRequest.getInput.asScala.nonEmpty) {
      var inputs = Json.arr()
      processRequest.getInput.asScala.foreach { input =>
        inputs = inputs.append(jsAnyContent(input))
      }
      json += ("inputs" -> inputs)
    }
    json
  }

  def jsAnyContent(content: AnyContent): JsObject = {
    var json = Json.obj()
    if (content.getName != null) json = json + ("name" -> JsString(content.getName))
    if (content.getType != null) {
      json = json + ("type" -> JsString(content.getType))
      if (content.getType == "binary") {
        json = json + ("embeddingMethod" -> JsString(ValueEmbeddingEnumeration.BASE_64.value()))
      }
    }
    if (content.getValue != null) {
      json = json + ("value" -> JsString(content.getValue))
    }
    if (content.getEncoding != null) json = json + ("encoding" -> JsString(content.getEncoding))
    if (content.getMimeType != null) json = json + ("mimeType" -> JsString(content.getMimeType))
    if (content.getItem.asScala.nonEmpty) {
      var children = Json.arr()
      content.getItem.asScala.foreach { item =>
        children = children.append(jsAnyContent(item))
      }
      json = json + ("item" -> children)
    }
    json
  }

  def jsEmailSettings(settings: EmailSettings, maskPassword: Boolean = true): JsObject = {
    var json = Json.obj("enabled" -> JsBoolean(settings.enabled))
    if (settings.from.isDefined) json = json + ("from" -> JsString(settings.from.get))
    if (settings.to.isDefined && settings.to.get.nonEmpty) json = json + ("to" -> jsStringArray(settings.to.get))
    if (settings.smtpHost.isDefined) json = json + ("host" -> JsString(settings.smtpHost.get))
    if (settings.smtpPort.isDefined) json = json + ("port" -> JsNumber(settings.smtpPort.get))
    if (settings.authEnabled.exists(x => x)) {
      json = json + ("authenticate" -> JsBoolean(settings.authEnabled.get))
      if (settings.authUsername.exists(StringUtils.isNoneBlank(_))) json = json + ("username" -> JsString(settings.authUsername.get))
      if (settings.authPassword.exists(StringUtils.isNoneBlank(_))) json = json + ("password" -> JsString(if (maskPassword) "*****" else settings.authPassword.get))
    }
    if (settings.sslEnabled.isDefined) json = json + ("sslEnabled" -> JsBoolean(settings.sslEnabled.get))
    if (settings.sslProtocols.isDefined && settings.sslProtocols.get.nonEmpty) json = json + ("sslProtocols" -> jsStringArray(settings.sslProtocols.get))
    if (settings.startTlsEnabled.isDefined) json = json + ("startTlsEnabled" -> JsBoolean(settings.startTlsEnabled.get))
    if (settings.maximumAttachments.isDefined) json = json + ("maxAttachmentCount" -> JsNumber(settings.maximumAttachments.get))
    if (settings.maximumAttachmentSize.isDefined) json = json + ("maxAttachmentSize" -> JsNumber(settings.maximumAttachmentSize.get))
    if (settings.allowedAttachmentTypes.isDefined && settings.allowedAttachmentTypes.get.nonEmpty) json = json + ("allowedAttachmentTypes" -> jsStringArray(settings.allowedAttachmentTypes.get))
    if (settings.testInteractionReminder.isDefined) json = json + ("testInteractionReminder" -> JsNumber(settings.testInteractionReminder.get))
    if (settings.contactFormEnabled.isDefined) json = json + ("contactFormEnabled" -> JsBoolean(settings.contactFormEnabled.get))
    if (settings.contactFormCopyDefaultMailbox.isDefined) json = json + ("contactFormCopyDefaultMailbox" -> JsBoolean(settings.contactFormCopyDefaultMailbox.get))
    json
  }

  def parseJsEmailSettings(jsonString: String): EmailSettings = {
    val json = Json.parse(jsonString)
    EmailSettings(
      enabled = (json \ "enabled").as[Boolean],
      from = (json \ "from").asOpt[String],
      to = (json \ "to").asOpt[JsArray].map(_.value.map(_.as[String]).toArray),
      smtpHost = (json \ "host").asOpt[String],
      smtpPort = (json \ "port").asOpt[Int],
      authEnabled = (json \ "authenticate").asOpt[Boolean],
      authUsername = (json \ "username").asOpt[String],
      authPassword = (json \ "password").asOpt[String],
      sslEnabled = (json \ "sslEnabled").asOpt[Boolean],
      startTlsEnabled = (json \ "startTlsEnabled").asOpt[Boolean],
      sslProtocols = (json \ "sslProtocols").asOpt[JsArray].map(_.value.map(_.as[String]).toArray),
      maximumAttachments = (json \ "maxAttachmentCount").asOpt[Int],
      maximumAttachmentSize = (json \ "maxAttachmentSize").asOpt[Int],
      allowedAttachmentTypes = (json \ "allowedAttachmentTypes").asOpt[JsArray].map(_.value.map(_.as[String]).toSet),
      testInteractionReminder = (json \ "testInteractionReminder").asOpt[Int],
      contactFormEnabled = (json \ "contactFormEnabled").asOpt[Boolean],
      contactFormCopyDefaultMailbox = (json \ "contactFormCopyDefaultMailbox").asOpt[Boolean]
    )
  }

  def validatorForAnyContent(): Reads[AnyContent] = {
    (js: JsValue) => {
      val content = new AnyContent()
      val obj = js.asInstanceOf[JsObject]
      val definedKeys = new mutable.HashSet[String]()
      definedKeys.addAll(obj.keys)
      content.setName(parseOptionalStringField(obj, "name", definedKeys).orNull)
      content.setType(parseOptionalStringField(obj, "type", definedKeys).orNull)
      content.setValue(parseOptionalStringField(obj, "value", definedKeys).orNull)
      content.setEncoding(parseOptionalStringField(obj, "encoding", definedKeys).orNull)
      content.setMimeType(parseOptionalStringField(obj, "mimeType", definedKeys).orNull)
      val embeddingMethod = parseOptionalStringField(obj, "embeddingMethod", definedKeys)
      if (embeddingMethod.nonEmpty) {
        content.setEmbeddingMethod(ValueEmbeddingEnumeration.fromValue(embeddingMethod.get))
      }
      parseOptionalArrayField(obj, "item", validatorForAnyContent(), definedKeys).foreach { item =>
        content.getItem.add(item)
      }
      ensureNoExtraFields(definedKeys.toSet)
      JsSuccess(content)
    }
  }

  private def ensureNoExtraFields(extraFields: Set[String]): Unit = {
    if (extraFields.nonEmpty) {
      throw JsonValidationException("Unexpected fields found: ["+extraFields.mkString(",")+"]")
    }
  }

  def validatorForProcessRequest(): Reads[ProcessRequest] = {
    (js: JsValue) => {
      val processRequest = new ProcessRequest()
      val obj = js.asInstanceOf[JsObject]
      val definedKeys = new mutable.HashSet[String]()
      definedKeys.addAll(obj.keys)
      processRequest.setOperation(parseOptionalStringField(obj, "operation", definedKeys).orNull)
      parseOptionalArrayField(obj, "inputs", validatorForAnyContent(), definedKeys).foreach { item =>
        processRequest.getInput.add(item)
      }
      ensureNoExtraFields(definedKeys.toSet)
      JsSuccess(processRequest)
    }
  }

  private def parseOptionalStringField(obj: JsObject, fieldName: String, remainingKeys: mutable.HashSet[String]): Option[String] = {
    if (obj.keys.contains(fieldName)) {
      remainingKeys.remove(fieldName)
      val field = obj(fieldName)
      field match {
        case string: JsString => Some(string.value)
        case _ => throw JsonValidationException("Field [" + fieldName + "] must be defined as a string.")
      }
    } else {
      None
    }
  }

  private def parseOptionalArrayField[A](obj: JsObject, fieldName: String, reader: Reads[A], remainingKeys: mutable.HashSet[String]): List[A] = {
    if (obj.keys.contains(fieldName)) {
      remainingKeys.remove(fieldName)
      val field = obj(fieldName)
      field match {
        case array: JsArray =>
          val items = new ListBuffer[A]()
          array.value.toList.foreach { item =>
            items += reader.reads(item).get
          }
          items.toList
        case _ => throw JsonValidationException("Field [" + fieldName + "] must be defined as an array.")
      }
    } else {
      List()
    }
  }

  def constructErrorMessage(errorCode: Any, errorDesc: String, errorIdentifier: Option[String], errorHint: Option[String] = None): JsObject = {
    val code = errorCode match {
      case _: Int => "" + errorCode
      case _: String => errorCode.asInstanceOf[String]
      case _ => ""
    }
    var obj = Json.obj(
      "error_code" -> code,
      "error_description" -> errorDesc
    )
    if (errorIdentifier.isDefined) {
      obj = obj.+("error_id" -> JsString(errorIdentifier.get))
    }
    if (errorHint.isDefined) {
      obj = obj.+("error_hint" -> JsString(errorHint.get))
    }
    obj
  }

}