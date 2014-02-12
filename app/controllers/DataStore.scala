package controllers

import reactivemongo.api._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.{Singleton, Named}
import scala.concurrent.{ExecutionContext, Future}
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

  def listFiles: Future[List[ReadFile[BSONValue]]] = {
    val foundFile = gridFS.find(BSONDocument())
    foundFile.collect[List]().recover {
      case _ => List()
    }
  }

  def save(file: File, name: String) = {

    val fileToSave = DefaultFileToSave(name, Some("application/octet-stream"))
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
