package managers.`export`

case class DeletionsImportPreviewData(communityDeletions: Seq[(Long, String)],
                                      domainDeletions: Seq[(Long, String)])
