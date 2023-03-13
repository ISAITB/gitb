package models

import models.Enums.ConformanceStatementItemType.ConformanceStatementItemType

case class ConformanceStatementItem(id: Long, name: String, description: Option[String], itemType: ConformanceStatementItemType, items: Option[Seq[ConformanceStatementItem]])
