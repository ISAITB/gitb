package utils

import java.text.SimpleDateFormat
import java.util.{Date, Calendar, TimeZone}

object TimeUtil {

  val MS_IN_A_DAY = 24 * 60 * 60 * 1000
  val MS_IN_AN_HOUR = 60 * 60 * 1000

  val formatUTC = {
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    format.setTimeZone(TimeZone.getTimeZone("UTC"))
    format
  }
  val formatDate = {
    val format = new SimpleDateFormat("yyyy-MM-dd")
    format
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

  def parseUTCDatetime(datetime:String): Date = {
    formatUTC.parse(datetime)
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
    val d = formatUTC.parse(timestamp)
    val curr = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime()
    ((curr.getTime() - d.getTime()) / MS_IN_A_DAY).toInt
  }

  def getTimeDifferenceInHours(timestamp:String):Int = {
    val d = formatUTC.parse(timestamp)
    val curr = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime()
    ((curr.getTime() - d.getTime()) / MS_IN_AN_HOUR).toInt
  }

}
