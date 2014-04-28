package services

import javax.inject.{Singleton, Named}
import models.{Event, Email}
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocumentReader, BSONObjectID, BSONDateTime, BSONDocument}
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Date
import play.api.Logger


@Named
@Singleton
class EmailDataStore extends DataStore {

  val collection = db[BSONCollection]("emails")

  def listContacts(email: String) = {
    collection.find(BSONDocument(), BSONDocument(("ownerEmail",1))).cursor[BSONDocument].collect[Set]().map(_.map(_.getAs[String]("ownerEmail").get))
  }

  def find(id: String) = {
    val cursor = collection.find(BSONDocument("_id" -> BSONObjectID(id))).cursor[Email]
    cursor.headOption
  }

  def listEmails(email: String) = {
    collection.find(BSONDocument(
      "ownerEmail" -> email)).cursor[BSONDocument].collect[List]()
  }

  def save(email: Email) {
    collection.insert(BSONDocument(
      "ownerEmail" -> email.owner,
      "from" -> email.from,
      "subject" -> email.subject,
      "receivedDate" -> BSONDateTime(email.receivedDate.getTime),
      "sentDate" -> BSONDateTime(email.sentDate.getTime),
      "replyTo" -> email.replyTo,
      "recipients" -> email.recipients,
      "content" -> email.content
    ))
  }

  implicit val emailDocumentReader:  BSONDocumentReader[Email] = new BSONDocumentReader[Email] {
    override def read(bson: BSONDocument): Email = {
      Email(
        bson.getAs[String]("ownerEmail").get,
        bson.getAs[Array[String]]("from").get,
        bson.getAs[Array[String]]("recipients").get,
        bson.getAs[String]("subject").get,
        bson.getAs[String]("content").get,
        new Date(bson.getAs[BSONDateTime]("receivedDate").get.value),
        bson.getAs[Array[String]]("replyTo").get,
        new Date(bson.getAs[BSONDateTime]("sentDate").get.value)
      )
    }
  }
}
