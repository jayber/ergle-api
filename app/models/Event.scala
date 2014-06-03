package models

import java.util.{Calendar, Date}
import java.text.SimpleDateFormat

case class Event(eventType: String, title: String, sortDate: Date, link: String, tag: Option[String]) {
  final val dateFormat: SimpleDateFormat = new SimpleDateFormat("d MMM ''yy")
  def dateCategory = {
    val instance: Calendar = Calendar.getInstance
    instance.setTime(sortDate)
    instance.set(Calendar.HOUR_OF_DAY, 0)
    instance.set(Calendar.MINUTE, 0)
    instance.set(Calendar.SECOND, 0)
    instance.set(Calendar.MILLISECOND, 0)

    val today: Calendar = Calendar.getInstance
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    val yesterday: Calendar = Calendar.getInstance
    yesterday.setTime(today.getTime)
    yesterday.add(Calendar.DATE, -1)

    instance match {
      case `today` => "Today"
      case `yesterday` => "Yesterday"
      case _ => dateFormat.format(instance.getTime)
    }
  }
}

