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

package managers

import exceptions.{AutomationApiException, ErrorCodes}
import models.automation.{CustomPropertyInfo, KeyValueRequired, OrganisationIdsForApi, StatementIds}
import org.apache.commons.lang3.StringUtils
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.JsonUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AutomationApiHelper @Inject()(dbConfigProvider: DatabaseConfigProvider)
                                   (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def loadOrganisationDataForAutomationProcessing(organisationKey: String): DBIO[Option[OrganisationIdsForApi]] = {
    PersistenceSchema.organizations
      .join(PersistenceSchema.communities).on(_.community === _.id)
      .filter(_._1.apiKey === organisationKey)
      .map(x => (x._1.id, x._1.community, x._2.domain, x._2.allowAutomationApi))
      .result
      .headOption
      .map(_.map(x => OrganisationIdsForApi(x._1, x._2, x._3, x._4)))
  }

  def checkOrganisationForAutomationApiUse(organisationData: Option[OrganisationIdsForApi]): DBIO[_] = {
    if (organisationData.isEmpty) {
      throw AutomationApiException(ErrorCodes.API_ORGANISATION_NOT_FOUND, "Unable to find organisation based on provided API key")
    } else if (!organisationData.get.apiEnabled) {
      throw AutomationApiException(ErrorCodes.API_COMMUNITY_DOES_NOT_ENABLE_API, "Community does not allow use of the test automation API")
    } else {
      DBIO.successful(())
    }
  }

  def getStatementIdsForApiKeys(organisationKey: String, systemKey: Option[String], actorKey: Option[String], snapshotKey: Option[String], testSuiteKeys: Option[List[String]], testCaseKeys: Option[List[String]]): DBIO[StatementIds] = {
    for {
      // We don't check the snapshot case for the organisation because we need a current one to load the community from.
      organisationData <- loadOrganisationDataForAutomationProcessing(organisationKey)
      _ <- checkOrganisationForAutomationApiUse(organisationData)
      // Load snapshot ID (if needed).
      snapshotId <- if (snapshotKey.isDefined) {
        PersistenceSchema.conformanceSnapshots
          .filter(_.apiKey === snapshotKey.get)
          .filter(_.community === organisationData.get.communityId)
          .map(_.id).result.headOption
      } else {
        DBIO.successful(None)
      }
      // System ID.
      systemId <- {
        if (systemKey.isDefined) {
          // Lookup using provided system key.
          if (snapshotId.isEmpty) {
            PersistenceSchema.systems
              .filter(_.owner === organisationData.get.organisationId)
              .filter(_.apiKey === systemKey.get)
              .map(_.id).result.headOption
          } else {
            PersistenceSchema.conformanceSnapshotSystems
              .filter(_.apiKey === systemKey.get)
              .filter(_.snapshotId === snapshotId.get)
              .map(_.id).result.headOption
          }
        } else {
          // Lookup from organisation (it should define a single system). We ignore the snapshot in this case.
          for {
            organisationSystemIds <- PersistenceSchema.systems
              .filter(_.owner === organisationData.get.organisationId)
              .map(_.id).result
            organisationSystemId <- {
              if (organisationSystemIds.size == 1) {
                DBIO.successful(organisationSystemIds.headOption)
              } else {
                DBIO.successful(None)
              }
            }
          } yield organisationSystemId
        }
      }
      _ <- {
        if (systemId.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_SYSTEM_NOT_FOUND, "Unable to determine system based on the provided information")
        } else {
          DBIO.successful(())
        }
      }
      actorId <- {
        if (actorKey.isDefined) {
          // Lookup using provided actor key.
          if (snapshotId.isEmpty) {
            PersistenceSchema.actors
              .filter(_.apiKey === actorKey.get)
              .filterOpt(organisationData.get.domainId)((q, domain) => q.domain === domain)
              .map(_.id).result.headOption
          } else {
            PersistenceSchema.conformanceSnapshotActors
              .filter(_.apiKey === actorKey.get)
              .map(_.id).result.headOption
          }
        } else {
          // No actor API key was provided - determine otherwise.  We ignore the snapshot in this case.
          if (testSuiteKeys.isDefined && testSuiteKeys.get.nonEmpty) {
            // Lookup using test suite ID(s).
            for {
              matchedActorIds <- PersistenceSchema.testSuites
                .join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testsuite)
                .join(PersistenceSchema.testCases).on(_._2.testcase === _.id)
                .join(PersistenceSchema.testCaseHasActors).on(_._2.id === _.testcase)
                .filter(_._1._1._1.identifier inSet testSuiteKeys.get.toSet)
                .filterOpt(organisationData.get.domainId)((q, domainId) => q._1._1._1.domain === domainId)
                .filterIf(testCaseKeys.exists(_.nonEmpty))(q => q._1._2.identifier inSet testCaseKeys.get.toSet)
                .filter(_._2.sut === true)
                .map(_._2.actor)
                .distinct
                .result
              matchedActorId <- {
                if (matchedActorIds.size == 1) {
                  DBIO.successful(matchedActorIds.headOption)
                } else {
                  DBIO.successful(None)
                }
              }
            } yield matchedActorId
          } else if (testCaseKeys.isDefined && testCaseKeys.get.nonEmpty) {
            // Lookup using test case ID(s).
            for {
              matchedActorIds <- PersistenceSchema.testCaseHasActors
                .join(PersistenceSchema.testCases).on(_.testcase === _.id)
                .join(PersistenceSchema.actors).on(_._1.actor === _.id)
                .filterOpt(organisationData.get.domainId)((q, domainId) => q._2.domain === domainId)
                .filter(_._1._2.identifier inSet testCaseKeys.get.toSet)
                .filter(_._1._1.sut === true)
                .map(_._1._1.actor)
                .distinct
                .result
              matchedActorId <- {
                if (matchedActorIds.size == 1) {
                  DBIO.successful(matchedActorIds.headOption)
                } else {
                  DBIO.successful(None)
                }
              }
            } yield matchedActorId
          } else {
            // There's no way to lookup the actor.
            DBIO.successful(None)
          }
        }
      }
      statementIds <- {
        if (actorId.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_ACTOR_NOT_FOUND, "Unable to determine actor based on the provided information")
        } else {
          DBIO.successful(StatementIds(organisationData.get.organisationId, systemId.get, actorId.get, organisationData.get.communityId, snapshotId))
        }
      }
    } yield statementIds
  }

  def getCommunityIdsByCommunityApiKey(communityApiKey: String): DBIO[(Long, Option[Long])] = {
    for {
      communityIds <- PersistenceSchema.communities
        .filter(_.apiKey === communityApiKey)
        .map(x => (x.id, x.domain))
        .result
        .headOption
      _ <- {
        if (communityIds.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_COMMUNITY_NOT_FOUND, "No community found for the provided API key")
        } else {
          DBIO.successful(())
        }
      }
    } yield communityIds.get
  }

  def getCommunityByCommunityApiKey(communityApiKey: String): DBIO[Long] = {
    for {
      communityIds <- getCommunityIdsByCommunityApiKey(communityApiKey)
    } yield communityIds._1
  }

  def getOrganisationByOrganisationApiKey(communityId: Long, organisationApiKey: String): DBIO[Long] = {
    for {
      organisationId <- PersistenceSchema.organizations
        .filter(_.community === communityId)
        .filter(_.apiKey === organisationApiKey)
        .map(_.id)
        .result
        .headOption
        .map { result =>
          result.getOrElse(throw AutomationApiException(ErrorCodes.API_ORGANISATION_NOT_FOUND, "No organisation found for the provided API key"))
        }
    } yield organisationId
  }

  def getDomainIdByDomainApiKey(domainApiKey: String, communityKeyToCheckAgainst: Option[String] = None): DBIO[Long] = {
    for {
      newDomainId <- PersistenceSchema.domains
        .filter(_.apiKey === domainApiKey)
        .map(_.id)
        .result
        .headOption
      domainIdToUse <- {
        if (newDomainId.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_DOMAIN_NOT_FOUND, "No domain found for the provided API key")
        } else {
          DBIO.successful(newDomainId.get)
        }
      }
      communityDomainIdToMatch <- {
        if (communityKeyToCheckAgainst.isDefined) {
          getDomainIdByCommunity(communityKeyToCheckAgainst.get)
        } else {
          DBIO.successful(None)
        }
      }
      _ <- {
        if (communityDomainIdToMatch.isDefined && communityDomainIdToMatch.get != domainIdToUse) {
          throw AutomationApiException(ErrorCodes.API_DOMAIN_NOT_FOUND, "No domain found for the provided API keys")
        } else {
          DBIO.successful(())
        }
      }
    } yield domainIdToUse
  }

  def getDomainIdByCommunity(communityApiKey: String): DBIO[Option[Long]] = {
    for {
      communityDomainId <- PersistenceSchema.communities
        .filter(_.apiKey === communityApiKey)
        .map(_.domain)
        .result
        .headOption
      _ <- {
        if (communityDomainId.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_COMMUNITY_NOT_FOUND, "No community found for the provided API key")
        } else {
          DBIO.successful(())
        }
      }
    } yield communityDomainId.get
  }

  def getDomainIdByCommunityOrDomainApiKey(communityApiKey: String, domainApiKey: Option[String]): DBIO[Long] = {
    for {
      // Look-up the community's domain.
      communityDomainId <- getDomainIdByCommunity(communityApiKey)
      // Look-up the domain related to the domain API key (if provided).
      domainId <- {
        if (domainApiKey.isDefined) {
          for {
            domainId <- PersistenceSchema.domains
              .filter(_.apiKey === domainApiKey.get)
              .map(_.id)
              .result
              .headOption
            _ <- {
              if (domainId.isEmpty) {
                throw AutomationApiException(ErrorCodes.API_DOMAIN_NOT_FOUND, "No domain found for the provided domain API key")
              } else if (communityDomainId.isDefined && communityDomainId.get != domainId.get) {
                throw AutomationApiException(ErrorCodes.API_DOMAIN_NOT_FOUND, "The provided domain API key did not match the community's domain")
              } else {
                DBIO.successful(())
              }
            }
          } yield domainId
        } else {
          DBIO.successful(None)
        }
      }
      // Make consistency checks and return the domain ID.
      domainIdToReturn <- {
        if (communityDomainId.isDefined) {
          DBIO.successful(communityDomainId.get)
        } else if (domainId.isDefined) {
          DBIO.successful(domainId.get)
        } else {
          throw AutomationApiException(ErrorCodes.API_DOMAIN_NOT_FOUND, "You need to specify the domain API key to identify a specific domain.")
        }
      }
    } yield domainIdToReturn
  }

  def getActorIdsByDomainId(domainId: Option[Long], actorApiKey: String, endpointRequired: Boolean): DBIO[(Long, Option[Long])] = {
    for {
      // Load actor ID.
      actorId <- {
        for {
          actorId <- PersistenceSchema.actors
            .filter(_.apiKey === actorApiKey)
            .filterOpt(domainId)((q, id) => q.domain === id)
            .map(_.id)
            .result
            .headOption
          _ <- {
            if (actorId.isEmpty) {
              throw AutomationApiException(ErrorCodes.API_ACTOR_NOT_FOUND, "No actor found for the provided API key")
            } else {
              DBIO.successful(())
            }
          }
        } yield actorId.get
      }
      // Load endpoint ID (if exists).
      endpointId <- {
        for {
          endpointId <- PersistenceSchema.endpoints
            .filter(_.actor === actorId)
            .map(_.id)
            .result
            .headOption
          _ <- {
            if (endpointRequired && endpointId.isEmpty) {
              throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "No property found for the provided API key")
            } else {
              DBIO.successful(())
            }
          }
        } yield endpointId
      }
    } yield (actorId, endpointId)
  }

  def propertyUseText(required: Option[Boolean], defaultValue: String = "O"): String = {
    if (required.isDefined) {
      "R"
    } else {
      defaultValue
    }
  }

  def propertyAllowedValuesText(values: Option[List[KeyValueRequired]]): Option[String] = {
    if (values.isDefined) {
      val nonEmptyValues = values.get.filter(keyValue => StringUtils.isNotEmpty(keyValue.key) && StringUtils.isNotEmpty(keyValue.value))
      if (nonEmptyValues.nonEmpty) {
        Some(JsonUtil.jsAllowedPropertyValues(nonEmptyValues).toString())
      } else {
        None
      }
    } else {
      None
    }
  }

  def propertyDefaultValue(defaultValue: Option[String], allowedValues: Option[List[KeyValueRequired]]): Option[String] = {
    if (defaultValue.isDefined) {
      if (allowedValues.isDefined) {
        if (allowedValues.get.exists(kv => kv.key == defaultValue.get)) {
          defaultValue
        } else {
          throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "The default value must be one of the defined allowed values")
        }
      } else {
        defaultValue
      }
    } else {
      None
    }
  }

  def propertyDependsOnStatus(input: CustomPropertyInfo, dependencyAllowedValues: Option[String]): (Option[Option[String]], Option[Option[String]]) = {
    var dependsOn = input.dependsOn
    var dependsOnValue = input.dependsOnValue
    if (dependsOn.isEmpty || dependsOnValue.isEmpty) {
      dependsOn = None
      dependsOnValue = None
    } else if (dependsOn.get.isEmpty || dependsOnValue.get.isEmpty) {
      dependsOn = Some(None)
      dependsOnValue = Some(None)
    } else if (dependsOnValue.flatten.isDefined
      && dependencyAllowedValues.isDefined
      && !JsonUtil.parseJsAllowedPropertyValues(dependencyAllowedValues.get).exists(p => p.key == dependsOnValue.flatten.get)) {
      // The property we depend upon does not support the configured value.
      throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "The property [%s] upon which this property depends on does not support the value [%s]".formatted(dependsOn.flatten.get, dependsOnValue.flatten.get))
    }
    (dependsOn, dependsOnValue)
  }

}
