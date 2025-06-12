package managers

import managers.CommunityHelper.MemberIds
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object CommunityHelper {

  case class MemberIds(organisationIds: Option[Iterable[Long]], systemIds: Option[Iterable[Long]])

}

@Singleton
class CommunityHelper @Inject() (dbConfigProvider: DatabaseConfigProvider)
                                (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  private[managers] def memberIdsToUse(orgIds: Option[List[Long]], sysIds: Option[List[Long]], orgParameters: Option[Map[Long, Set[String]]], sysParameters: Option[Map[Long, Set[String]]]): Future[MemberIds]  = {
    organisationIdsToUse(orgIds, orgParameters).zip(
      systemIdsToUse(sysIds, sysParameters)
    ).map { results =>
      MemberIds(results._1, results._2)
    }
  }

  private[managers] def organisationIdsToUse(organisationIds: Option[Iterable[Long]], orgParameters: Option[Map[Long, Set[String]]]): Future[Option[Iterable[Long]]] = {
    val matchingIds = organisationIds.map(_.toSet)
    if (orgParameters.isDefined) {
      // As we have organisation parameters we will check to see if we should restrict the organisation IDs based on parameter filtering.
      // We start with the organisation IDs and progressively filter based on the parameters.
      orgParameters.get.foldLeft(Future.successful(matchingIds)) { (previous, next) =>
        previous.flatMap { matchingIds =>
          if (matchingIds.exists(_.isEmpty)) {
            // No matching IDs. Return immediately without checking other parameters.
            Future.successful(Some(Set[Long]()))
          } else {
            organisationIdsForParameterValues(matchingIds, next._1, next._2).map { idsMatchingParams =>
              // For the next iteration consider the IDS matched based on the provided parameters.
              Some(idsMatchingParams)
            }
          }
        }
      }
    } else {
      Future.successful(matchingIds)
    }
  }

  private def organisationIdsForParameterValues(organisationIds: Option[Iterable[Long]], parameterId: Long, values: Iterable[String]): Future[Set[Long]] = {
    DB.run(
      PersistenceSchema.organisationParameterValues
        .filterOpt(organisationIds)((table, ids) => table.organisation inSet ids)
        .filter(_.parameter === parameterId)
        .filter(_.value inSet values)
        .map(x => x.organisation)
        .result
    ).map(_.toSet)
  }

  private[managers] def systemIdsToUse(systemIds: Option[Iterable[Long]], sysParameters: Option[Map[Long, Set[String]]]): Future[Option[Iterable[Long]]] = {
    // As we have organisation parameters we will check to see if we should restrict the organisation IDs based on parameter filtering.
    // We start with the organisation IDs and progressively filter based on the parameters.
    val matchingIds = systemIds.map(_.toSet)
    if (sysParameters.isDefined) {
      // As we have organisation parameters we will check to see if we should restrict the organisation IDs based on parameter filtering.
      // We start with the organisation IDs and progressively filter based on the parameters.
      sysParameters.get.foldLeft(Future.successful(matchingIds)) { (previous, next) =>
        previous.flatMap { matchingIds =>
          if (matchingIds.exists(_.isEmpty)) {
            // No matching IDs. Return immediately without checking other parameters.
            Future.successful(Some(Set[Long]()))
          } else {
            systemIdsForParameterValues(matchingIds, next._1, next._2).map { idsMatchingParams =>
              // For the next iteration consider the IDS matched based on the provided parameters.
              Some(idsMatchingParams)
            }
          }
        }
      }
    } else {
      Future.successful(matchingIds)
    }
  }

  private def systemIdsForParameterValues(systemIds: Option[Iterable[Long]], parameterId: Long, values: Iterable[String]): Future[Set[Long]] = {
    DB.run(
      PersistenceSchema.systemParameterValues
        .filterOpt(systemIds)((table, ids) => table.system inSet ids)
        .filter(_.parameter === parameterId)
        .filter(_.value inSet values)
        .map(x => x.system)
        .result
    ).map(_.toSet)
  }

}
