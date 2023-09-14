package utils

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZoneOffset, ZonedDateTime}
import java.util.{Calendar, Date, TimeZone}

object TimeUtil {

  val MS_IN_A_DAY = 24 * 60 * 60 * 1000
  val MS_IN_AN_HOUR = 60 * 60 * 1000
  val MS_IN_A_SECOND = 1000
  private val DATE_FORMATTER_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
  private val UTC_ZONE = ZoneId.of("UTC")

  val formatUTC = {
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    format.setTimeZone(TimeZone.getTimeZone("UTC"))
    format
  }

  val formatDate = {
    val format = new SimpleDateFormat("yyyy-MM-dd")
    format
  }

  def serializeTimestamp(t:Timestamp): String = {
    new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date(t.getTime))
  }

  def serializeTimestampUTC(t:Timestamp): String = {
    ZonedDateTime.of(t.toLocalDateTime, ZoneId.systemDefault())
      .withZoneSameInstant(UTC_ZONE)
      .format(DATE_FORMATTER_UTC)
  }

  def parseTimestamp(timestamp:String): Timestamp = {
    new Timestamp(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse(timestamp).getTime())
  }

  def getCurrentTimestamp(): Timestamp = {
    new Timestamp(System.currentTimeMillis)
  }

  def copyTimestamp(source: Option[Timestamp]): Option[Timestamp] = {
    if (source.isDefined) {
      Some(new Timestamp(source.get.getTime))
    } else {
      None
    }
  }

  def getCurrentTime():String = {
    val time = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime()
    formatUTC.format(time)
  }

  def getCurrentDate():String = {
    val time = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime()
    formatDate.format(time)
  }

  def serializeUTCDatetime(datetime:Date):String = {
      formatUTC.format(datetime)
  }

  def serializeDate(date:Date):String = {
    formatDate.format(date)
  }

  def parseDate(date:String): Date = {
    formatDate.parse(date)
  }

  def getPreviousDate(previous:Int):String = {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.add(Calendar.DATE, -1*previous);
    formatDate.format(cal.getTime())
  }

  def getNextDate(next:Int):String = {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.add(Calendar.DATE, next);
    formatDate.format(cal.getTime())
  }

  def addDayToDate(date:String, add:Int):String = { //adds day to "date"
  val d = formatDate.parse(date)
    val cal = Calendar.getInstance()
    cal.setTime(d)
    cal.add(Calendar.DATE, add)
    formatDate.format(cal.getTime())
  }

  def addDayToDatetime(date:String, add:Int):String = {   //adds day to "datetime"
    val d = formatUTC.parse(date)
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.setTime(d)
    cal.add(Calendar.DATE, add)
    formatUTC.format(cal.getTime())
  }

  def getTimeDifferenceInDays(timestamp:String):Int = {
    getTimeDifference(timestamp) / MS_IN_A_DAY
  }

  def getTimeDifferenceInHours(timestamp:String):Int = {
    getTimeDifference(timestamp) / MS_IN_AN_HOUR
  }

  def getTimeDifferenceInSeconds(timestamp:String):Int = {
    getTimeDifference(timestamp) / MS_IN_A_SECOND
  }

  def getTimeDifferenceInSeconds(timestamp:Timestamp):Int = {
    (getCurrentTimestamp().getTime - timestamp.getTime).toInt / MS_IN_A_SECOND
  }

  def getTimeDifference(timestamp:String):Int = {
    val d = formatUTC.parse(timestamp)
    val curr = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime()
    (curr.getTime() - d.getTime()).toInt
  }

}
