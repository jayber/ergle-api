package controllers

import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson.{BSONObjectID, BSONDocument, BSONValue}
import scala.concurrent.{Await, Future}
import play.api.mvc.{SimpleResult, AnyContent, Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject.{Singleton, Named, Inject}
import play.api.libs.iteratee.Iteratee
import reactivemongo.api.gridfs.Implicits.DefaultReadFileReader
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import play.mvc.Http.Request
import play.mvc.Result

object FilesController {
  def idFromFileName(fileName: String) = {
    val split = fileName.split( """\.""")
    (split(0), split(1).toLowerCase)
  }

  def fileIdFileName(file: ReadFile[BSONValue]) = {
    new StringBuilder(file.id match {
      case theId: BSONObjectID => theId.stringify
      case _ => throw new RuntimeException("ReadFile.id is not a BSONObjectID")
    }) + "." + file.filename.split( """\.""")(1)
  }
}

@Named
@Singleton
class FilesController extends Controller {

  @Inject
  var dataStore: DataStore = null

  def put = Action.async(parse.temporaryFile) {
    request =>
      dataStore.save(request.body.file, request.getQueryString("filename").getOrElse("unknown-file"), request.getQueryString("email").get, request.getQueryString("lastModified").get.toLong).map {
        id =>
          Ok(id)
      }
  }

  def listFiles = Action.async { request =>
    request.cookies.get("email") match {
      case Some(cookie) => getListFilesResult(cookie.value)
      case _ => Future(NotFound(""))
    }
  }

  def listFilesForEmail(email: String) = Action.async {
    getListFilesResult(email)
  }


  def getListFilesResult(email: String): Future[SimpleResult] = {
    dataStore.listFiles(Some(email)).map(displayResults)
  }

  def displayResults(results: List[ReadFile[BSONValue]]) = {
    Ok(views.html.fileList(results.reverse))
  }

  def wrapper(fileName: String) = Action.async {
    request =>
      val (id, extension) = FilesController.idFromFileName(fileName)
      extension match {
        case "jpg" => imageWrapper(id, extension)
        case "jpeg" => imageWrapper(id, extension)
        case "png" => imageWrapper(id, extension)
        case "bmp" => imageWrapper(id, extension)
        case "gif" => imageWrapper(id, extension)
        case "svg" => imageWrapper(id, extension)
        case "txt" =>
          val cursor = dataStore.gridFS.find(BSONDocument("_id" -> BSONObjectID(id)))
          cursor.headOption.map({
            file =>
              Ok(views.html.textWrapper(fileText(file)))
          })
        case _ => Future {
          NotFound("")
        }
      }
  }


  def fileText(file: Option[ReadFile[BSONValue]]): String = {
    file match {
      case Some(realFile) =>
        val fileText = new StringBuilder
        Await.result(dataStore.gridFS.enumerate(realFile).apply {
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
      case None => "no file found"
    }
  }

  def imageWrapper(id: String, extension: String) = {
    Future {
      Ok(views.html.imageWraper(id, extension))
    }
  }
}
