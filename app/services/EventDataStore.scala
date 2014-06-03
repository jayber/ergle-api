package services

import javax.inject.{Singleton, Named}
import reactivemongo.bson.{BSONDateTime, BSONDocumentReader, BSONDocument}
import models.Event
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global

@Named
@Singleton
class EventDataStore extends DataStore {
  def listEvents(email: String) = {

    eventsCollection.find(BSONDocument(
      "ownerEmail" -> email)).sort(BSONDocument("date" -> 0)).cursor[Event].collect[List]()
  }

  implicit val eventDocumentReader: BSONDocumentReader[Event] = new BSONDocumentReader[Event] {
    override def read(bson: BSONDocument): Event = {
      Event(bson.getAs[String]("eventType").get,
        bson.getAs[String]("title").get,
        new Date(bson.getAs[BSONDateTime]("date").get.value),
        bson.getAs[String]("link").get,
        bson.getAs[String]("tag"))
    }
  }
}
