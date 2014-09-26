package services

import javax.inject.{Inject, Singleton, Named}
import models._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Date
import org.apache.james.mime4j.dom.{SingleBody, Entity, Multipart, Message}
import java.io.{InputStream, ByteArrayOutputStream}
import scala.collection.JavaConverters._
import reactivemongo.api.collections.default.BSONCollection
import org.apache.james.mime4j.dom.address.AddressList
import scala.util.{Failure, Success}
import play.api.libs.ws.WS
import play.api.Logger
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import reactivemongo.bson.BSONDateTime
import reactivemongo.api.collections.default.BSONCollection
import play.api.http.Writeable
import scala.concurrent.duration._
import reactivemongo.bson.BSONDateTime
import models.Email
import models.InlineEmailBody
import reactivemongo.api.collections.default.BSONCollection
import play.api.Play.current


@Named
@Singleton
class EmailDataStore extends DataStore {

  @Inject
  var configProvider: ConfigProvider = null

  val collection = db[BSONCollection]("emails")

  def find(id: String) = {
    val cursor = collection.find(BSONDocument("_id" -> BSONObjectID(id))).cursor[Email]
    cursor.headOption
  }

  def listEmails(email: String) = {
    collection.find(BSONDocument(
      "ownerEmail" -> email)).cursor[BSONDocument].collect[List]()
  }

  def save(email: (String, Message)) = {
    val (owner, message) = email
    val id = BSONObjectID.generate
    collection.insert(BSONDocument(
      "_id" -> id,
      "ownerEmail" -> owner,
      "from" -> message.getFrom.get(0).getAddress,
      "subject" -> message.getSubject,
      "receivedDate" -> BSONDateTime(message.getDate.getTime),
      "replyTo" -> message.getReplyTo,
      "to" -> message.getTo,
      "cc" -> message.getCc,
      "body" -> parseContent(message, owner)
    )).flatMap(lastError =>
        saveEvent(owner,Some(message.getDate.getTime),Some(message.getSubject),"email",
          Some(s"""/emails/${id.stringify}"""), None, Seq(owner), None, None)
      )
  }

  def parseContent(message: Entity, owner: String): Array[BSONDocument] = {
    message.getBody match {
      case multipart: Multipart =>
        var parts = Array[BSONDocument]()
        for (part: Entity <- multipart.getBodyParts.asScala) {
          parts = parts ++ parseContent(part, owner)
        }
        parts
      case single: SingleBody if message.getDispositionType == null ||
        message.getDispositionType.startsWith("inline") =>
        Array(BSONDocument(
          "mimeType" -> message.getMimeType,
          "disposition" -> {
            message.getDispositionType match {
              case null => ""
              case result => result
            }
          },
          "content" -> {
            val output: ByteArrayOutputStream = new ByteArrayOutputStream()
            single.writeTo(output)
            output.toString("UTF-8")
          }
        ))
      case single: SingleBody if message.getDispositionType.startsWith("attachment") =>
        val requestHolder = WS.url(configProvider.config.getString(ConfigProvider.apiUrlKey) + "/files/").withRequestTimeout(1000 * 10)
        val putFuture = requestHolder.withHeaders(("Accept","text/plain")).withQueryString(
          ("filename", message.getFilename),
          ("email", owner),
          ("source", "email")).put({
            val output: ByteArrayOutputStream = new ByteArrayOutputStream()
            single.writeTo(output)
            output.toByteArray
          })
        Array(BSONDocument(
          "disposition" -> message.getDispositionType,
          "filename" -> message.getFilename,
          "fileId" -> Await.result(putFuture, 10 seconds).body
        ))
    }
  }

  implicit val addressListWriter: BSONWriter[AddressList, BSONArray] = new BSONWriter[AddressList, BSONArray] {
    override def write(addresses: AddressList): BSONArray = {
      var array = BSONArray()
      if (addresses != null) {
        for (address <- addresses.flatten().asScala) {
          array = array.add(address.getAddress)
        }
      }
      array
    }
  }

  implicit val emailDocumentReader: BSONDocumentReader[Email] = new BSONDocumentReader[Email] {
    override def read(bson: BSONDocument): Email = {
      Email(
        bson.getAs[String]("ownerEmail").get,
        bson.getAs[String]("from").get,
        bson.getAs[Array[String]]("to").get,
        bson.getAs[Array[String]]("cc"),
        bson.getAs[String]("subject").get,
        readBody(bson),
        new Date(bson.getAs[BSONDateTime]("receivedDate").get.value),
        bson.getAs[Array[String]]("replyTo")
      )
    }
  }

  def readBody(bson: BSONDocument): Array[AnyRef] = {
    val bodies = bson.getAs[Array[BSONDocument]]("body").get
    bodies.map{
      body =>
        body.getAs[String]("disposition") match {
          case Some("attachment") => Attachment(body.getAs[String]("fileId").get, body.getAs[String]("filename").get)
          case _ => InlineEmailBody(body.getAs[String]("mimeType").get, body.getAs[String]("content").get)
        }
    }
  }
}
