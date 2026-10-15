/*
 * Copyright (C) 2026 European Union
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

import exceptions.{ErrorCodes, InvalidRequestException}
import models.Enums.TestResultStatus
import models._
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.RepositoryUtils

import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
 * Merges organisations, merges systems and moves systems between organisations.
 *
 * Test sessions and conformance statements are kept by repointing their references (the names cached in the test
 * sessions are deliberately left unchanged as they record the situation at the time of execution). Whenever two systems
 * are merged, the target system's configuration is kept and any gaps are filled from the source system.
 */
@Singleton
class MergeManager @Inject() (repositoryUtils: RepositoryUtils,
                              systemManager: SystemManager,
                              organizationManager: OrganizationManager,
                              testResultManager: TestResultManager,
                              dbConfigProvider: DatabaseConfigProvider)
                             (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  /**
   * Processes the provided request for the source organisation.
   *
   * @return The ID of the organisation to show once the operation completes (the target organisation when merging
   *         organisations, otherwise the source organisation).
   */
  def process(sourceOrganisationId: Long, request: MergeRequest): Future[Long] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      sourceOrganisation <- PersistenceSchema.organizations.filter(_.id === sourceOrganisationId).result.headOption
      targetOrganisation <- {
        if (request.operation == MergeOperation.MERGE_SYSTEMS) {
          DBIO.successful(sourceOrganisation)
        } else if (request.target.isDefined) {
          PersistenceSchema.organizations.filter(_.id === request.target.get).result.headOption
        } else {
          DBIO.successful(None)
        }
      }
      sourceSystems <- PersistenceSchema.systems.filter(_.owner === sourceOrganisationId).result.map(_.toList)
      targetSystems <- {
        if (targetOrganisation.isDefined && targetOrganisation.get.id != sourceOrganisationId) {
          PersistenceSchema.systems.filter(_.owner === targetOrganisation.get.id).result.map(_.toList)
        } else {
          DBIO.successful(sourceSystems)
        }
      }
      _ <- {
        validate(sourceOrganisation, targetOrganisation, sourceSystems, targetSystems, request)
        DBIO.successful(())
      }
      _ <- execute(sourceOrganisation.get, targetOrganisation.get, request, onSuccessCalls)
    } yield targetOrganisation.get.id
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  private def invalid(message: String): Nothing = {
    throw InvalidRequestException(ErrorCodes.INVALID_REQUEST, message)
  }

  private def validate(sourceOrganisation: Option[Organizations], targetOrganisation: Option[Organizations], sourceSystems: List[Systems], targetSystems: List[Systems], request: MergeRequest): Unit = {
    if (sourceOrganisation.isEmpty) invalid("The source organisation could not be found")
    if (!Set(MergeOperation.MERGE_ORGANISATIONS, MergeOperation.MERGE_SYSTEMS, MergeOperation.MOVE_SYSTEMS).contains(request.operation)) invalid("Unsupported operation")
    if (sourceOrganisation.get.adminOrganization || sourceOrganisation.get.template) invalid("The source organisation cannot be the administration or a template organisation")
    val sameOrganisation = request.operation == MergeOperation.MERGE_SYSTEMS
    if (!sameOrganisation) {
      if (targetOrganisation.isEmpty) invalid("The target organisation could not be found")
      if (targetOrganisation.get.id == sourceOrganisation.get.id) invalid("The target organisation must be different from the source organisation")
      if (targetOrganisation.get.community != sourceOrganisation.get.community) invalid("The target organisation must belong to the same community")
      if (targetOrganisation.get.adminOrganization || targetOrganisation.get.template) invalid("The target organisation cannot be the administration or a template organisation")
    }
    // Systems.
    val sourceSystemIds = sourceSystems.map(_.id).toSet
    val targetSystemIds = targetSystems.map(_.id).toSet
    val actionSystemIds = request.systems.map(_.systemId)
    if (actionSystemIds.toSet.size != actionSystemIds.size) invalid("A system was referenced multiple times")
    request.systems.foreach { systemAction =>
      if (!sourceSystemIds.contains(systemAction.systemId)) invalid("A referenced system does not belong to the source organisation")
      val allowedActions = request.operation match {
        case MergeOperation.MERGE_ORGANISATIONS => Set(MergeSystemActionType.MOVE, MergeSystemActionType.MERGE, MergeSystemActionType.DELETE)
        case MergeOperation.MERGE_SYSTEMS => Set(MergeSystemActionType.KEEP, MergeSystemActionType.MERGE)
        case _ => Set(MergeSystemActionType.KEEP, MergeSystemActionType.MOVE, MergeSystemActionType.MERGE)
      }
      if (!allowedActions.contains(systemAction.action)) invalid("An action is not supported for the requested operation")
      if (systemAction.action == MergeSystemActionType.MERGE) {
        if (systemAction.target.isEmpty) invalid("A system to merge into was not provided")
        if (systemAction.target.get == systemAction.systemId) invalid("A system cannot be merged into itself")
        if (!targetSystemIds.contains(systemAction.target.get)) invalid("A system to merge into does not belong to the target organisation")
      }
    }
    if (request.operation == MergeOperation.MERGE_ORGANISATIONS) {
      // All systems of the organisation being deleted must be explicitly handled.
      if (sourceSystemIds != actionSystemIds.toSet) invalid("An action must be defined for each system of the source organisation")
    } else {
      if (!request.systems.exists(_.action != MergeSystemActionType.KEEP)) invalid("No changes were requested")
    }
    if (request.operation == MergeOperation.MERGE_SYSTEMS) {
      // A system that is merged into another cannot also be the target of a merge.
      val mergedAway = request.systems.filter(_.action == MergeSystemActionType.MERGE).map(_.systemId).toSet
      if (request.systems.exists(x => x.action == MergeSystemActionType.MERGE && mergedAway.contains(x.target.get))) invalid("A system that is merged into another cannot also be the target of a merge")
    }
  }

  private def execute(sourceOrganisation: Organizations, targetOrganisation: Organizations, request: MergeRequest, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    val systemActions = request.systems.flatMap { systemAction =>
      systemAction.action match {
        case MergeSystemActionType.MOVE => Some(moveSystem(systemAction.systemId, targetOrganisation))
        case MergeSystemActionType.MERGE => Some(mergeSystemInto(systemAction.systemId, systemAction.target.get, targetOrganisation, onSuccess))
        case MergeSystemActionType.DELETE => Some(systemManager.deleteSystem(systemAction.systemId, onSuccess))
        case _ => None
      }
    }
    for {
      _ <- DBIO.seq(systemActions.map(a => a: DBIO[_]): _*)
      _ <- {
        if (request.operation == MergeOperation.MERGE_ORGANISATIONS) {
          // The users, remaining configuration and snapshot references of the source organisation are handled by its deletion.
          for {
            _ <- mergeOrganisationProperties(sourceOrganisation.id, targetOrganisation.id, onSuccess)
            _ <- organizationManager.deleteOrganization(sourceOrganisation.id, onSuccess)
          } yield ()
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
  }

  /**
   * Moves a system unchanged (statements, configuration, keys) to the target organisation.
   */
  private def moveSystem(systemId: Long, targetOrganisation: Organizations): DBIO[_] = {
    for {
      _ <- PersistenceSchema.systems.filter(_.id === systemId).map(_.owner).update(targetOrganisation.id)
      // The sessions follow the system, including the organisation name they display.
      _ <- PersistenceSchema.testResults.filter(_.sutId === systemId).map(x => (x.organizationId, x.organization)).update((Some(targetOrganisation.id), Some(targetOrganisation.shortname)))
    } yield ()
  }

  /**
   * Merges the source system into the target system and deletes the source system. The target system's configuration
   * is kept, with any gaps filled from the source system.
   */
  private def mergeSystemInto(sourceSystemId: Long, targetSystemId: Long, targetOrganisation: Organizations, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      targetSystemName <- PersistenceSchema.systems.filter(_.id === targetSystemId).map(_.shortname).result.head
      // Test cases for which the source system has test sessions (to recalculate the target's results).
      sessionTestCaseIds <- PersistenceSchema.testResults.filter(_.sutId === sourceSystemId).filter(_.testCaseId.isDefined).map(_.testCaseId).distinct.result.map(_.flatten.toSet)
      // Reassign the test sessions, which also take the names of the target system and organisation.
      _ <- PersistenceSchema.testResults.filter(_.sutId === sourceSystemId).map(x => (x.sutId, x.sut, x.organizationId, x.organization))
        .update((Some(targetSystemId), Some(targetSystemName), Some(targetOrganisation.id), Some(targetOrganisation.shortname)))
      // Conformance statements.
      sourceStatements <- PersistenceSchema.systemImplementsActors.filter(_.systemId === sourceSystemId).result
      targetStatements <- PersistenceSchema.systemImplementsActors.filter(_.systemId === targetSystemId).map(x => (x.specId, x.actorId)).result.map(_.toSet)
      movedStatements = sourceStatements.filter(x => !targetStatements.contains((x._2, x._3)))
      movedTestCaseIds <- {
        if (movedStatements.nonEmpty) {
          val movedKeys = movedStatements.map(x => (x._2, x._3)).toSet
          PersistenceSchema.conformanceResults.filter(_.sut === sourceSystemId).map(x => (x.spec, x.actor, x.testcase)).result.map { rows =>
            rows.filter(x => movedKeys.contains((x._1, x._2))).map(_._3).toSet
          }
        } else {
          DBIO.successful(Set.empty[Long])
        }
      }
      // Statements that only exist for the source system are moved over. For statements that exist in both, the source system's results are dropped when it is deleted.
      _ <- DBIO.seq(movedStatements.map { statement =>
        for {
          _ <- PersistenceSchema.conformanceResults.filter(x => x.sut === sourceSystemId && x.spec === statement._2 && x.actor === statement._3).map(_.sut).update(targetSystemId)
          _ <- PersistenceSchema.systemImplementsActors.filter(x => x.systemId === sourceSystemId && x.specId === statement._2 && x.actorId === statement._3).map(_.systemId).update(targetSystemId)
        } yield ()
      }: _*)
      // Configuration (statement parameters and system properties).
      _ <- mergeStatementParameters(sourceSystemId, targetSystemId, onSuccess)
      _ <- mergeSystemProperties(sourceSystemId, targetSystemId, onSuccess)
      // Recalculate the conformance results for the target system based on its expanded set of sessions.
      _ <- recalculateConformanceResults(targetSystemId, sessionTestCaseIds ++ movedTestCaseIds)
      // Delete the source system (its remaining statements, configuration and snapshot references).
      _ <- systemManager.deleteSystem(sourceSystemId, onSuccess)
    } yield ()
  }

  private def recalculateConformanceResults(systemId: Long, testCaseIds: Set[Long]): DBIO[_] = {
    DBIO.seq(testCaseIds.toList.map { testCaseId =>
      for {
        latestSession <- testResultManager.latestConsideredSession(systemId, testCaseId, None)
        _ <- {
          val updateQuery = for { c <- PersistenceSchema.conformanceResults.filter(_.sut === systemId).filter(_.testcase === testCaseId) } yield (c.testsession, c.result, c.outputMessage, c.updateTime)
          if (latestSession.isDefined) {
            updateQuery.update(Some(latestSession.get.sessionId), latestSession.get.result, latestSession.get.outputMessage, latestSession.get.endTime.orElse(Some(latestSession.get.startTime)))
          } else {
            updateQuery.update(None, TestResultStatus.UNDEFINED.toString, None, None)
          }
        }
      } yield ()
    }: _*)
  }

  private def mergeStatementParameters(sourceSystemId: Long, targetSystemId: Long, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      sourceValues <- PersistenceSchema.configs.filter(_.system === sourceSystemId).result
      existingKeys <- PersistenceSchema.configs.filter(_.system === targetSystemId).map(x => (x.parameter, x.endpoint)).result.map(_.toSet)
      _ <- DBIO.seq(sourceValues.filter(x => !existingKeys.contains((x.parameter, x.endpoint))).map { value =>
        onSuccess += (() => repositoryUtils.setStatementParameterFile(value.parameter, targetSystemId, repositoryUtils.getStatementParameterFile(value.parameter, sourceSystemId), copy = true))
        PersistenceSchema.configs += Configs(targetSystemId, value.parameter, value.endpoint, value.value, value.contentType)
      }: _*)
    } yield ()
  }

  private def mergeSystemProperties(sourceSystemId: Long, targetSystemId: Long, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      sourceValues <- PersistenceSchema.systemParameterValues.filter(_.system === sourceSystemId).result
      existingKeys <- PersistenceSchema.systemParameterValues.filter(_.system === targetSystemId).map(_.parameter).result.map(_.toSet)
      _ <- DBIO.seq(sourceValues.filter(x => !existingKeys.contains(x.parameter)).map { value =>
        onSuccess += (() => repositoryUtils.setSystemPropertyFile(value.parameter, targetSystemId, repositoryUtils.getSystemPropertyFile(value.parameter, sourceSystemId), copy = true))
        PersistenceSchema.systemParameterValues += SystemParameterValues(targetSystemId, value.parameter, value.value, value.contentType)
      }: _*)
    } yield ()
  }

  private def mergeOrganisationProperties(sourceOrganisationId: Long, targetOrganisationId: Long, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      sourceValues <- PersistenceSchema.organisationParameterValues.filter(_.organisation === sourceOrganisationId).result
      existingKeys <- PersistenceSchema.organisationParameterValues.filter(_.organisation === targetOrganisationId).map(_.parameter).result.map(_.toSet)
      _ <- DBIO.seq(sourceValues.filter(x => !existingKeys.contains(x.parameter)).map { value =>
        onSuccess += (() => repositoryUtils.setOrganisationPropertyFile(value.parameter, targetOrganisationId, repositoryUtils.getOrganisationPropertyFile(value.parameter, sourceOrganisationId), copy = true))
        PersistenceSchema.organisationParameterValues += OrganisationParameterValues(targetOrganisationId, value.parameter, value.value, value.contentType)
      }: _*)
    } yield ()
  }

}
