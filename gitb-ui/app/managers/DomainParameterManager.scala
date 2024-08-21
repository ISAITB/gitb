package managers

import models.DomainParameter
import models.Enums.TriggerDataType
import models.automation.DomainParameterInfo
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.RepositoryUtils

import java.io.File
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class DomainParameterManager @Inject()(repositoryUtils: RepositoryUtils,
                                       triggerManager: TriggerManager,
                                       dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def createDomainParameterInternal(parameter: DomainParameter, fileToStore: Option[File], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Long] = {
    for {
      id <- PersistenceSchema.domainParameters.returning(PersistenceSchema.domainParameters.map(_.id)) += parameter
      _ <- {
        if (fileToStore.isDefined) {
          onSuccessCalls += (() => repositoryUtils.setDomainParameterFile(parameter.domain, id, fileToStore.get))
        }
        DBIO.successful(())
      }
    } yield id
  }

  def createDomainParameter(parameter: DomainParameter, fileToStore: Option[File]) = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = createDomainParameterInternal(parameter, fileToStore, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def updateDomainParameter(domainId: Long, parameterId: Long, name: String, description: Option[String], kind: String, value: Option[String], inTests: Boolean, contentType: Option[String], fileToStore: Option[File]) = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = updateDomainParameterInternal(domainId, parameterId, name, description, kind, value, inTests, contentType, fileToStore, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def updateDomainParameterInternal(domainId: Long, parameterId: Long, name: String, description: Option[String], kind: String, value: Option[String], inTests: Boolean, contentType: Option[String], fileToStore: Option[File], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    if (kind == "BINARY") {
      if (fileToStore.isDefined) {
        onSuccessCalls += (() => repositoryUtils.setDomainParameterFile(domainId, parameterId, fileToStore.get))
        PersistenceSchema.domainParameters.filter(_.id === parameterId)
          .map(x => (x.name, x.desc, x.kind, x.inTests, x.value, x.contentType))
          .update((name, description, kind, inTests, value, contentType))
      } else {
        PersistenceSchema.domainParameters.filter(_.id === parameterId)
          .map(x => (x.name, x.desc, x.kind, x.inTests, x.value))
          .update((name, description, kind, inTests, value))
      }
    } else {
      onSuccessCalls += (() => repositoryUtils.deleteDomainParameterFile(domainId, parameterId))
      if (kind == "SIMPLE" || (kind == "HIDDEN" && value.isDefined)) {
        PersistenceSchema.domainParameters.filter(_.id === parameterId)
          .map(x => (x.name, x.desc, x.kind, x.inTests, x.value, x.contentType))
          .update((name, description, kind, inTests, value, None))
      } else { // HIDDEN no value
        PersistenceSchema.domainParameters.filter(_.id === parameterId)
          .map(x => (x.name, x.desc, x.kind, x.inTests, x.contentType))
          .update((name, description, kind, inTests, None))
      }
    }
  }

  def deleteDomainParameterWrapper(domainId: Long, domainParameter: Long) = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteDomainParameter(domainId, domainParameter, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def deleteDomainParameter(domainId: Long, domainParameter: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    onSuccessCalls += (() => repositoryUtils.deleteDomainParameterFile(domainId, domainParameter))
    triggerManager.deleteTriggerDataByDataType(domainParameter, TriggerDataType.DomainParameter) andThen
      PersistenceSchema.domainParameters.filter(_.id === domainParameter).delete
  }

  def getDomainParameter(domainParameterId: Long) = {
    exec(PersistenceSchema.domainParameters.filter(_.id === domainParameterId).result.head)
  }

  def getDomainParametersByCommunityIdInternal(communityId: Long, onlySimple: Boolean, loadValues: Boolean): DBIO[Seq[DomainParameter]] = {
    for {
      domainId <- PersistenceSchema.communities.filter(_.id === communityId).map(x => x.domain).result.head
      domainParameters <- {
        if (domainId.isDefined) {
          val query = PersistenceSchema.domainParameters
            .filter(_.domain === domainId.get)
            .filterIf(onlySimple)(_.kind === "SIMPLE")
          if (loadValues) {
            query.map(x => (x.id, x.name, x.kind, x.desc, x.value))
              .sortBy(_._2.asc)
              .result
              .map(_.toList.map(x => DomainParameter(x._1, x._2, x._4, x._3, x._5, inTests = false, None, domainId.get)))
          } else {
            query.map(x => (x.id, x.name, x.kind, x.desc))
              .sortBy(_._2.asc)
              .result
              .map(_.toList.map(x => DomainParameter(x._1, x._2, x._4, x._3, None, inTests = false, None, domainId.get)))

          }
        } else {
          DBIO.successful(List[DomainParameter]())
        }
      }
    } yield domainParameters
  }

  def getDomainParametersByCommunityId(communityId: Long, onlySimple: Boolean, loadValues: Boolean): List[DomainParameter] = {
    exec(getDomainParametersByCommunityIdInternal(communityId, onlySimple, loadValues)).toList
  }

  def getDomainParameters(domainId: Long, loadValues: Boolean, onlyForTests: Option[Boolean], onlySimple: Boolean): List[DomainParameter] = {
    val query = PersistenceSchema.domainParameters.filter(_.domain === domainId)
      .filterOpt(onlyForTests)((table, filterValue) => table.inTests === filterValue)
      .filterIf(onlySimple)(_.kind === "SIMPLE")
      .sortBy(_.name.asc)
    if (loadValues) {
      exec(
        query
          .result
          .map(_.toList)
      )
    } else {
      exec(
        query
          .map(x => (x.id, x.name, x.desc, x.kind, x.inTests, x.contentType))
          .result
      ).map(x => DomainParameter(x._1, x._2, x._3, x._4, None, x._5, x._6, domainId)).toList
    }
  }

  def getDomainParameters(domainId: Long): List[DomainParameter] = {
    getDomainParameters(domainId, loadValues = true, None, onlySimple = false)
  }

  def getDomainParameterByDomainAndName(domainId: Long, name: String) = {
    exec(
      PersistenceSchema.domainParameters
        .filter(_.domain === domainId)
        .filter(_.name === name)
        .result
        .headOption
    )
  }

  def deleteDomainParameters(domainId: Long, onSuccessCalls: ListBuffer[() => _]) = {
    (for {
      ids <- PersistenceSchema.domainParameters.filter(_.domain === domainId).map(_.id).result
      _ <- DBIO.seq(ids.map(id => deleteDomainParameter(domainId, id, onSuccessCalls)): _*)
    } yield ()).transactionally
  }

  def updateDomainParametersViaApi(domainId: Option[Long], updates: List[DomainParameterInfo], warnings: ListBuffer[String]): DBIO[_] = {
    for {
      existingDomainProperties <- {
        if (updates.nonEmpty) {
          PersistenceSchema.domainParameters
            .join(PersistenceSchema.domains).on(_.domain === _.id)
            .filterOpt(domainId)((q, id) => q._2.id === id)
            .map(x => (x._1.name, x._1.id, x._1.kind, x._2.apiKey, x._2.id))
            .result
            .map { properties =>
              val keyMap = new mutable.HashMap[String, ListBuffer[(Long, String, String, Long)]]() // Key to (ID, type, domainApiKey, domainID)
              properties.foreach { property =>
                var propertyList = keyMap.get(property._1)
                if (propertyList.isEmpty) {
                  propertyList = Some(new ListBuffer[(Long, String, String, Long)])
                  keyMap.put(property._1, propertyList.get)
                }
                propertyList.get.append((property._2, property._3, property._4, property._5))
              }
              keyMap.map(x => (x._1, x._2.toList)).toMap
            }
        } else {
          DBIO.successful(Map.empty[String, List[(Long, String, String, Long)]])
        }
      }
      // Update domain properties
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        updates.foreach { propertyData =>
          if (existingDomainProperties.contains(propertyData.parameterInfo.key)) {
            val matchingProperties = existingDomainProperties(propertyData.parameterInfo.key)
              .filter(prop => propertyData.domainApiKey.isEmpty || propertyData.domainApiKey.get.equals(prop._3))
            if (matchingProperties.size == 1) {
              val matchingPropertyInfo = matchingProperties.head
              if (matchingPropertyInfo._2 == "SIMPLE") {
                if (propertyData.parameterInfo.value.isDefined) {
                  // Update
                  actions += PersistenceSchema.domainParameters
                    .filter(_.id === matchingPropertyInfo._1)
                    .map(_.value)
                    .update(propertyData.parameterInfo.value)
                } else {
                  // Delete
                  warnings += "Ignoring deletion for domain property [%s]. Domain properties cannot be deleted via automation API.".formatted(propertyData.parameterInfo.key)
                }
              } else {
                warnings += "Ignoring update for domain property [%s]. Only simple properties can be updated via the automation API.".formatted(propertyData.parameterInfo.key)
              }
            } else if (matchingProperties.size > 1) {
              warnings += "Ignoring update for domain property [%s]. Multiple properties were found matching the provided key from different domains. Please specify the domain API key to identify the specific property to update.".formatted(propertyData.parameterInfo.key)
            } else {
              // This case normally never occurs. A property that is not found is never recorded with an empty list.
              warnings += "Ignoring update for domain property [%s]. Property was not found.".formatted(propertyData.parameterInfo.key)
            }
          } else {
            warnings += "Ignoring update for domain property [%s]. Property was not found.".formatted(propertyData.parameterInfo.key)
          }
        }
        toDBIO(actions)
      }
    } yield ()
  }

}
