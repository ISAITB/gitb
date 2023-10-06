package managers.breadcrumb

case class BreadcrumbLabelRequest(
  userId: Long,
  domain: Option[Long],
  specification: Option[Long],
  specificationGroup: Option[Long],
  actor: Option[Long],
  community: Option[Long],
  organisation: Option[Long],
  system: Option[Long]
)