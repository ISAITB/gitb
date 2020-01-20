package managers

import controllers.util.{ParameterExtractor, RequestWithAttributes}
import javax.inject.{Inject, Singleton}
import models.Enums.LabelType.LabelType
import models.{CommunityLabels, Enums}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

@Singleton
class CommunityLabelManager @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def getLabel(request: RequestWithAttributes[_], labelType: LabelType): String = {
    getLabel(getLabels(request), labelType, true, false)
  }

  def getLabel(labels: Map[Short, CommunityLabels], labelType: LabelType): String = {
    getLabel(labels, labelType, true, false)
  }

  def getLabel(request: RequestWithAttributes[_], labelType: LabelType, single: Boolean, lowercase: Boolean): String = {
    getLabel(getLabels(request), labelType, single, lowercase)
  }

  def getLabel(labels: Map[Short, CommunityLabels], labelType: LabelType, single: Boolean, lowercase: Boolean): String = {
    val label = labels(labelType.id.toShort)
    var labelText: String = null
    if (single) {
      labelText = label.singularForm
    } else {
      labelText = label.pluralForm
    }
    if (lowercase && label.fixedCase || !lowercase) {
      labelText
    } else {
      labelText.toLowerCase
    }
  }

  def getLabels(request: RequestWithAttributes[_]): Map[Short, CommunityLabels]  = {
    val userId = ParameterExtractor.extractUserId(request)
    val communityId = exec(PersistenceSchema.users
      .join(PersistenceSchema.organizations).on(_.organization === _.id)
      .filter(_._1.id === userId)
      .map(x => x._2.community)
      .result).head
    getLabels(communityId)
  }

  def getLabels(communityId: Long): Map[Short, CommunityLabels] = {
    val labelMap: scala.collection.mutable.Map[Short, CommunityLabels] = scala.collection.mutable.Map.empty[Short, CommunityLabels]
    exec(PersistenceSchema.communityLabels.filter(_.community === communityId).result).map(label => {
      labelMap += (label.labelType -> label)
    })
    checkToAddDefault(labelMap, Enums.LabelType.Domain.id.toShort, "Domain", "Domains", false)
    checkToAddDefault(labelMap, Enums.LabelType.Specification.id.toShort, "Specification", "Specifications", false)
    checkToAddDefault(labelMap, Enums.LabelType.Actor.id.toShort, "Actor", "Actors", false)
    checkToAddDefault(labelMap, Enums.LabelType.Endpoint.id.toShort, "Endpoint", "Endpoints", false)
    checkToAddDefault(labelMap, Enums.LabelType.Organisation.id.toShort, "Organisation", "Organisations", false)
    checkToAddDefault(labelMap, Enums.LabelType.System.id.toShort, "System", "Systems", false)
    labelMap.toMap
  }

  private def checkToAddDefault(labelMap: scala.collection.mutable.Map[Short, CommunityLabels], labelType: Short, singularForm: String, pluralForm: String, fixedCase: Boolean) = {
    if (!labelMap.contains(labelType)) {
      labelMap += (labelType -> CommunityLabels(-1, labelType, singularForm , pluralForm, fixedCase))
    }
  }

}
