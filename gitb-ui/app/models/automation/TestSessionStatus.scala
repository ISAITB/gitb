package models.automation

import java.sql.Timestamp

case class TestSessionStatus(sessionId: String, startTime: Timestamp, endTime: Option[Timestamp], result: String, outputMessage: Option[String], logs: Option[List[String]], report: Option[String])
