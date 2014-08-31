package services

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
import controllers.ergleapi.FilesController

@Named
@Singleton
class FileDataStore extends DataStore {

  val gridFS = new GridFS(db, "attachments")

  def exists(email: String, filename: String, lastModified: Long) = {
    gridFS.find(BSONDocument(
      ("metadata.email", email),
      ("filename", filename),
      ("metadata.lastModified", BSONDateTime(lastModified)))).headOption.map {
      case Some(_) => true
      case None => false
    }
  }

  //todo: can't this be done with gridfs.readToOputputStream more easily instead?
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

  def listFiles(emailOpt: Option[String]): Future[List[ReadFile[BSONValue]]] = {
    var query = emailOpt match {
      case Some(email) => BSONDocument("metadata.email" -> email)
      case _ => BSONDocument()
    }
    query = query ++ ("metadata.source" -> "event")
    val sort = BSONDocument(("metadata.lastModified", 1))
    val foundFile = gridFS.find(BSONDocument(("$query", query), ("$orderby", sort)))
    foundFile.collect[List]().recover {
      case _ => List()
    }
  }

  def saveFileEvent(file: File, name: String, email: String, lastModifiedDate: Option[Long], source: Option[String], tag: Option[String], shareTo: Option[String]): Future[String] = {
    saveFileOnly(file, name, email, lastModifiedDate, source, tag, shareTo).flatMap(id =>
      saveEvent(
        email,
        lastModifiedDate,
        Some(name),
        "file",
        Some(s"/files/${FilesController.fileIdFileName(id, name)}"),
        tag,
        shareTo match {
          case Some(to) => Seq(email, to)
          case None => Seq(email)
        },
        None
      ).map(_ => id)
    )
  }

  def saveFileOnly(file: File, name: String, email: String, lastModifiedDate: Option[Long], source: Option[String], tag: Option[String], shareTo: Option[String]) = {
    val fileToSave = DefaultFileToSave(name, Some("application/octet-stream"), Some(System.currentTimeMillis()),
      BSONDocument(
        ("email", email),
        ("lastModified", BSONDateTime(lastModifiedDate.getOrElse(System.currentTimeMillis()))),
        ("source", source.getOrElse("event")),
        ("tag", tag)
      )
    )

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
