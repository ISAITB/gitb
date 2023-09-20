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
    getLabel(getLabelsByUserId(ParameterExtractor.extractUserId(request)), labelType, single = true, lowercase = false)
  }

  def getLabel(labels: Map[Short, CommunityLabels], labelType: LabelType): String = {
    getLabel(labels, labelType, single = true, lowercase = false)
  }

  def getLabel(request: RequestWithAttributes[_], labelType: LabelType, single: Boolean, lowercase: Boolean): String = {
    getLabel(getLabelsByUserId(ParameterExtractor.extractUserId(request)), labelType, single, lowercase)
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

  def getLabelsByUserId(userId: Long): Map[Short, CommunityLabels]  = {
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
    checkToAddDefault(labelMap, Enums.LabelType.Domain.id.toShort, "Domain", "Domains", fixedCase = false)
    checkToAddDefault(labelMap, Enums.LabelType.Specification.id.toShort, "Specification", "Specifications", fixedCase = false)
    checkToAddDefault(labelMap, Enums.LabelType.Actor.id.toShort, "Actor", "Actors", fixedCase = false)
    checkToAddDefault(labelMap, Enums.LabelType.Endpoint.id.toShort, "Endpoint", "Endpoints", fixedCase = false)
    checkToAddDefault(labelMap, Enums.LabelType.Organisation.id.toShort, "Organisation", "Organisations", fixedCase = false)
    checkToAddDefault(labelMap, Enums.LabelType.System.id.toShort, "System", "Systems", fixedCase = false)
    checkToAddDefault(labelMap, Enums.LabelType.SpecificationInGroup.id.toShort, "Option", "Options", fixedCase = false)
    checkToAddDefault(labelMap, Enums.LabelType.SpecificationGroup.id.toShort, "Specification group", "Specification groups", fixedCase = false)
    labelMap.toMap
  }

  private def checkToAddDefault(labelMap: scala.collection.mutable.Map[Short, CommunityLabels], labelType: Short, singularForm: String, pluralForm: String, fixedCase: Boolean) = {
    if (!labelMap.contains(labelType)) {
      labelMap += (labelType -> CommunityLabels(-1, labelType, singularForm , pluralForm, fixedCase))
    }
  }

}
