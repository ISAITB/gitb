package managers

import managers.breadcrumb.{BreadcrumbLabelRequest, BreadcrumbLabelResponse}
import models.Constants
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BreadcrumbManager @Inject() (dbConfigProvider: DatabaseConfigProvider)
                                  (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def getLabels(ids: BreadcrumbLabelRequest): Future[BreadcrumbLabelResponse] = {
    DB.run(for {
      // User info to apply restrictions.
      userInfo <- PersistenceSchema.users.filter(_.id === ids.userId)
        .join(PersistenceSchema.organizations).on(_.organization === _.id)
        .join(PersistenceSchema.communities).on(_._2.community === _.id)
        .map(x => (x._1._2.id, x._2.id, x._2.domain, x._1._2.adminOrganization)) // 1: Organisation ID, 2: Community ID, 3: Domain ID, 4: Admin organisation
        .result.head
      // Domain.
      domain <- {
        if (ids.domain.isDefined && (userInfo._3.isEmpty || userInfo._3.get == ids.domain.get)) {
          PersistenceSchema.domains.filter(_.id === ids.domain.get).map(_.shortname).result.headOption
        } else {
          DBIO.successful(None)
        }
      }
      // Specification group.
      specificationGroup <- {
        if (ids.specificationGroup.isDefined) {
          PersistenceSchema.specificationGroups
            .filter(_.id === ids.specificationGroup.get)
            // Filter also by the user's assigned domain unless no domain is assigned (e.g. for a Test Bed admin).
            .filterOpt(userInfo._3)((q, id) => q.domain === id)
            .map(_.shortname).result.headOption
        } else {
          DBIO.successful(None)
        }
      }
      // Specification.
      specification <- {
        if (ids.specification.isDefined) {
          PersistenceSchema.specifications
            .joinLeft(PersistenceSchema.specificationGroups).on(_.group === _.id)
            .filter(_._1.id === ids.specification.get)
            // Filter also by the user's assigned domain unless no domain is assigned (e.g. for a Test Bed admin).
            .filterOpt(userInfo._3)((q, id) => q._1.domain === id)
            .map(x => (x._1.shortname, x._2.map(_.shortname))).result.headOption
            .map(x => {
              if (x.isDefined) {
                if (x.get._2.isDefined) {
                  Some(x.get._2.get + " - " + x.get._1)
                } else {
                  Some(x.get._1)
                }
              } else {
                None
              }
            })
        } else {
          DBIO.successful(None)
        }
      }
      // Actor.
      actor <- {
        if (ids.actor.isDefined) {
          PersistenceSchema.actors
            .filter(_.id === ids.actor.get)
            // Filter also by the user's assigned domain unless no domain is assigned (e.g. for a Test Bed admin).
            .filterOpt(userInfo._3)((q, id) => q.domain === id)
            .map(_.actorId).result.headOption
        } else {
          DBIO.successful(None)
        }
      }
      // Community.
      community <- {
        if (ids.community.isDefined && (ids.community.get == userInfo._2 || userInfo._2 == Constants.DefaultCommunityId)) {
          // Return own community, or other community only if this is a Test Bed admin.
          PersistenceSchema.communities
            .filter(_.id === ids.community)
            .map(_.shortname).result.headOption
        } else {
          DBIO.successful(None)
        }
      }
      // Organisation.
      organisation <- {
        if (ids.organisation.isDefined && (ids.organisation.get == userInfo._1 || userInfo._4)) {
          // Return own organisation, or other organisation is this is a Test Bed or community admin.
          PersistenceSchema.organizations
            .filter(_.id === ids.organisation.get)
            // If not a Test Bed amin filter by the user's own community.
            .filterIf(userInfo._2 != Constants.DefaultCommunityId)(_.community === userInfo._2)
            .map(_.shortname).result.headOption
        } else {
          DBIO.successful(None)
        }
      }
      // System.
      system <- {
        if (ids.system.isDefined) {
          PersistenceSchema.systems
            .join(PersistenceSchema.organizations).on(_.owner === _.id)
            .filter(_._1.id === ids.system.get)
            // If this is not the Test Bed admin filter by the user's own community.
            .filterIf(userInfo._2 != Constants.DefaultCommunityId)(_._2.community === userInfo._2)
            // If this is not a Test Bed or community admin filter by the user's own organisation.
            .filterIf(!userInfo._4)(_._1.owner === userInfo._1)
            .map(_._1.shortname).result.headOption
        } else {
          DBIO.successful(None)
        }
      }
    } yield BreadcrumbLabelResponse(domain, specificationGroup, specification, actor, community, organisation, system))
  }

}
