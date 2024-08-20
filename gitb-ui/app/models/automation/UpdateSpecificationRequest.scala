package models.automation

case class UpdateSpecificationRequest(specificationApiKey: String,
                                      shortName: Option[String],
                                      fullName: Option[String],
                                      description: Option[Option[String]],
                                      hidden: Option[Boolean],
                                      displayOrder: Option[Short],
                                      groupApiKey: Option[Option[String]],
                                      communityApiKey: String) {
 }
