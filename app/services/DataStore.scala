package services

import reactivemongo.api.MongoDriver
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.api.collections.default.BSONCollection
import java.util.Date
import reactivemongo.bson.{BSONDateTime, BSONDocument}

trait DataStore {
  val driver = new MongoDriver
  val connection = driver.connection(List("localhost"))
  val db = connection("ergle")

  val eventsCollection = db[BSONCollection]("events")

  def saveEvent(ownerEmail: String, date: Option[Long], title: String, eventType: String, link: String, tag: Option[String]) = {
    eventsCollection.insert(BSONDocument(
      "ownerEmail" -> ownerEmail,
      "date" -> BSONDateTime(date.getOrElse(System.currentTimeMillis())),
      "title" -> title,
      "tag" -> tag,
      "eventType" -> eventType,
      "link" -> link
    ))
  }

}
