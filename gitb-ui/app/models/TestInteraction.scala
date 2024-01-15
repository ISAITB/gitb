package models

import java.sql.Timestamp

case class TestInteraction(sessionId: String, stepId: String, admin: Boolean, createTime: Timestamp, tpl:String)