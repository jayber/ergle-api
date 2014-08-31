package services

import javax.inject.{Singleton, Named}
import reactivemongo.bson.{BSONObjectID, BSONDateTime, BSONDocumentReader, BSONDocument}
import models.{Comment, Event}
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global

@Named
@Singleton
class EventDataStore extends DataStore {

  def saveComment(eventId: String, email: String, comment: String) = {
    eventsCollection.update(BSONDocument("_id" -> BSONObjectID(eventId)),BSONDocument(
      "$push" -> BSONDocument(
        "comments" -> BSONDocument(
          "email" -> email,
          "createdDate" -> BSONDateTime(System.currentTimeMillis),
          "comment" -> comment))
    ))
  }

  def listEvents(email: String) = {
    eventsCollection.find(BSONDocument(
      "to" -> email)).sort(BSONDocument("date" -> -1)).cursor[Event].collect[List]()
  }

  implicit val eventDocumentReader: BSONDocumentReader[Event] = new BSONDocumentReader[Event] {
    override def read(bson: BSONDocument): Event = {
      Event(
        bson.getAs[BSONObjectID]("_id").get.stringify,
        bson.getAs[String]("eventType").get,
        bson.getAs[String]("title"),
        new Date(bson.getAs[BSONDateTime]("date").get.value),
        bson.getAs[String]("link"),
        bson.getAs[String]("tag"),
        bson.getAs[Seq[String]]("to").get,
        bson.getAs[String]("content"),
        bson.getAs[Array[Comment]]("comments")
      )
    }
  }

  implicit val commentDocumentReader: BSONDocumentReader[Comment] = new BSONDocumentReader[Comment] {
    override def read(bson: BSONDocument): Comment = {
      Comment(
        new Date(bson.getAs[BSONDateTime]("createdDate").get.value),
        bson.getAs[String]("email").get,
        bson.getAs[String]("comment").get
      )
    }
  }
}
