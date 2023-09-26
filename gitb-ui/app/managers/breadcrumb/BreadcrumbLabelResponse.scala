package managers.breadcrumb

case class BreadcrumbLabelResponse(
  domain: Option[String],
  specificationGroup: Option[String],
  specification: Option[String],
  actor: Option[String],
  community: Option[String],
  organisation: Option[String],
  system: Option[String]
)
