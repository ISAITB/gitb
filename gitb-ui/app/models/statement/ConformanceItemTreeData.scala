package models.statement

import models.ConformanceStatement

case class ConformanceItemTreeData(statements: Iterable[ConformanceStatement], actorIdsToDisplay: Option[Set[Long]])
