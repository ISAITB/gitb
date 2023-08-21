package models

import models.Enums.ConformanceStatementItemType.ConformanceStatementItemType
import models.statement.ConformanceStatementResults

import scala.collection.immutable.Seq

case class ConformanceStatementItem(id: Long, name: String, description: Option[String], itemType: ConformanceStatementItemType, items: Option[Seq[ConformanceStatementItem]], displayOrder: Short, results: Option[ConformanceStatementResults] = None, actorToShow: Boolean = true) {

  def withChildren(children: Seq[ConformanceStatementItem]): ConformanceStatementItem = {
    ConformanceStatementItem(this.id, this.name, this.description, this.itemType, Some(children), this.displayOrder, this.results)
  }
}
