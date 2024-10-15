package models.automation

import models.Domain
import utils.CryptoUtil

case class CreateDomainRequest(
                                shortName: String,
                                fullName: String,
                                description: Option[String],
                                reportMetadata: Option[String],
                                apiKey: Option[String]) {

  def toDomain(): Domain = {
    Domain(0, shortName, fullName, description, reportMetadata, apiKey.getOrElse(CryptoUtil.generateApiKey()))
  }
}
