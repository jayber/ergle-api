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

  def saveEvent(ownerEmail: String, date: Option[Long], title: Option[String], eventType: String, link: Option[String], tag: Option[String], to: Seq[String], content: Option[String]) = {
    eventsCollection.insert(BSONDocument(
      "ownerEmail" -> ownerEmail,
      "to" -> to,
      "date" -> BSONDateTime(date.getOrElse(System.currentTimeMillis())),
      "title" -> title,
      "tag" -> tag,
      "eventType" -> eventType,
      "link" -> link,
      "content" -> content
    ))
  }

  def listContacts(email: String) = {
    eventsCollection.find(BSONDocument(), BSONDocument(("to", 1))).cursor[BSONDocument].collect[Set]().
      map { set =>
        set.map { bdoc =>
          bdoc.getAs[Seq[String]]("to").get
      }.flatten
    }
  }

}
