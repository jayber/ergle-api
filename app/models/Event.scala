package models

import java.util.{Calendar, Date}
import java.text.SimpleDateFormat

object Event {
  final val dateFormat: SimpleDateFormat = new SimpleDateFormat("d MMM ''yy")
  def categoriseDate(sortDate: Date): String = {
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

    val tomorrow: Calendar = Calendar.getInstance
    tomorrow.setTime(today.getTime)
    tomorrow.add(Calendar.DATE, 1)

    instance match {
      case `today` => "Today"
      case `yesterday` => "Yesterday"
      case `tomorrow` => "Tomorrow"
      case _ => dateFormat.format(instance.getTime)
    }
  }
}

case class Event(id:String, eventType: String, title: Option[String], sortDate: Date, link: Option[String], tag: Option[String], to: Seq[String], content: Option[String], comments: Option[Array[Comment]]) {
  def dateCategory = {
    Event.categoriseDate(sortDate)
  }

  def withOthers(currentEmail: String) = {
    to.filterNot {email =>
      email == currentEmail
    }
  }
}

case class Comment(createDate: Date, email: String, content: String) {
  def dateCategory = {
    Event.categoriseDate(createDate)
  }
}
