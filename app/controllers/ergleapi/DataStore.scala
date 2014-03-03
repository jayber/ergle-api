package controllers.ergleapi

import reactivemongo.api._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.{Singleton, Named}
import scala.concurrent.Future
import java.io.{FileInputStream, File}
import reactivemongo.api.gridfs._
import reactivemongo.bson._
import reactivemongo.api.gridfs.Implicits.DefaultReadFileReader
import reactivemongo.api.gridfs.{GridFS, ReadFile}

@Named
@Singleton
class DataStore {
  val driver = new MongoDriver
  val connection = driver.connection(List("localhost"))
  val db = connection("ergle")
  val gridFS = new GridFS(db, "attachments")

  def listContacts(email: String) = {
    listFiles(None).map {
      files => files.map {
        file => file.metadata.get("email") match {
          case Some(field: BSONString) => field.value
          case a => a.toString
        }
      }.toSet
    }
  }

  def listFiles(emailOpt: Option[String]): Future[List[ReadFile[BSONValue]]] = {
    val query = emailOpt match {
      case Some(email) => BSONDocument("metadata.email" -> email)
      case _ => BSONDocument()
    }
    val sort = BSONDocument(("metadata.lastModified", 1))
    val foundFile = gridFS.find(BSONDocument(("$query", query), ("$orderby", sort)))
    foundFile.collect[List]().recover {
      case _ => List()
    }
  }

  def save(file: File, name: String, email: String, lastModifiedDate: Long) = {
    val fileToSave = DefaultFileToSave(name, Some("application/octet-stream"), Some(System.currentTimeMillis()),
      BSONDocument(("email", email), ("lastModified", BSONDateTime(lastModifiedDate))))
    val futureResult: Future[ReadFile[BSONValue]] = gridFS.writeFromInputStream(fileToSave, new FileInputStream(file))
    futureResult.map {
      readFile =>
        readFile.id match {
          case theId: BSONObjectID => theId.stringify
          case _ => throw new RuntimeException("ReadFile.id is not a BSONObjectID")
        }
    }
  }
}
