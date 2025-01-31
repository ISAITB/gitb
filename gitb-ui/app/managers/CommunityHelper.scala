package managers

import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import javax.inject.{Inject, Singleton}

@Singleton
class CommunityHelper @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  private[managers] def organisationIdsToUse(organisationIds: Option[Iterable[Long]], orgParameters: Option[Map[Long, Set[String]]]): Option[Iterable[Long]] = {
    var matchingIds: Option[Iterable[Long]] = None
    if (organisationIds.isDefined) {
      matchingIds = Some(organisationIds.get)
    }
    if (orgParameters.isDefined) {
      orgParameters.get.foreach { entry =>
        matchingIds = Some(organisationIdsForParameterValues(matchingIds, entry._1, entry._2))
        if (matchingIds.get.isEmpty) {
          // No matching IDs. Return immediately without checking other parameters.
          return Some(Set[Long]())
        }
      }
    }
    matchingIds
  }

  private def organisationIdsForParameterValues(organisationIds: Option[Iterable[Long]], parameterId: Long, values: Iterable[String]): Set[Long] = {
    exec(
      PersistenceSchema.organisationParameterValues
        .filterOpt(organisationIds)((table, ids) => table.organisation inSet ids)
        .filter(_.parameter === parameterId)
        .filter(_.value inSet values)
        .map(x => x.organisation)
        .result
    ).toSet
  }

  private[managers] def systemIdsToUse(systemIds: Option[Iterable[Long]], sysParameters: Option[Map[Long, Set[String]]]): Option[Iterable[Long]] = {
    var matchingIds: Option[Iterable[Long]] = None
    if (systemIds.isDefined) {
      matchingIds = Some(systemIds.get)
    }
    if (sysParameters.isDefined) {
      sysParameters.get.foreach { entry =>
        matchingIds = Some(systemIdsForParameterValues(matchingIds, entry._1, entry._2))
        if (matchingIds.get.isEmpty) {
          // No matching IDs. Return immediately without checking other parameters.
          return Some(Set[Long]())
        }
      }
    }
    matchingIds
  }

  private def systemIdsForParameterValues(systemIds: Option[Iterable[Long]], parameterId: Long, values: Iterable[String]): Set[Long] = {
    exec(
      PersistenceSchema.systemParameterValues
        .filterOpt(systemIds)((table, ids) => table.system inSet ids)
        .filter(_.parameter === parameterId)
        .filter(_.value inSet values)
        .map(x => x.system)
        .result
    ).toSet
  }

}
