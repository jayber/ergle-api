package services

import reactivemongo.api._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.{Singleton, Named}
import scala.concurrent.{Await, Future}
import java.io.{FileInputStream, File}
import reactivemongo.api.gridfs._
import reactivemongo.bson._
import reactivemongo.api.gridfs.Implicits.DefaultReadFileReader
import reactivemongo.api.gridfs.{GridFS, ReadFile}
import reactivemongo.api.gridfs.DefaultFileToSave
import reactivemongo.bson.BSONDateTime
import reactivemongo.bson.BSONString
import scala.Some
import play.api.libs.iteratee.Iteratee
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

@Named
@Singleton
class FileDataStore extends DataStore{
  val gridFS = new GridFS(db, "attachments")


  def fileText(file: Option[ReadFile[BSONValue]]): String = {
    file match {
      case None => "no file found"
      case Some(realFile) =>
        val fileText = new StringBuilder
        Await.result(gridFS.enumerate(realFile).apply {
          Iteratee.fold[Array[Byte], StringBuilder](fileText) {
            (text, chunk) => {
              chunk.map {
                x =>
                  text.append(x.toChar)
              }
              text // this is kind of a cheat, should probably use fold or collect to get accumulated values
            }
          }
        }, Duration(10, TimeUnit.SECONDS))
        fileText.toString()
    }
  }

  def findFileById(id: String) = {
    val cursor = gridFS.find(BSONDocument("_id" -> BSONObjectID(id)))
    cursor.headOption
  }

  def listContacts(email: String) = {
    listFiles(None).map {
      files => files.map {
            //todo: should use projection instead of post hoc mapping
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
