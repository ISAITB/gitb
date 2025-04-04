package controllers.dto

import models.{OrganisationParameters, SystemParameters}

case class ParameterInfo(orgDefinitions: List[OrganisationParameters], orgValues: Map[Long, Map[Long, String]], sysDefinitions: List[SystemParameters], sysValues: Map[Long, Map[Long, String]])
