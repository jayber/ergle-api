package controllers

import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson.{BSONObjectID, BSONDocument, BSONValue}
import scala.concurrent.{Await, Future}
import play.api.mvc.{SimpleResult, Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject.{Singleton, Named, Inject}
import play.api.libs.iteratee.Iteratee
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import reactivemongo.api.gridfs.Implicits.DefaultReadFileReader
import services.FileDataStore
import java.io.{ByteArrayInputStream, File}
import play.api.Logger

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
  var dataStore: FileDataStore = null

  def put = {
    val file = File.createTempFile("ergle","tmp")
    Logger.debug(s"temp file: ${file.getAbsolutePath}")
    Action.async(parse.file(file)) {
    request =>
      dataStore.save(request.body,
        request.getQueryString("filename").getOrElse("unknown-file"),
        request.getQueryString("email").get,
        request.getQueryString("lastModified").get.toLong).map {
        id =>
          file.delete()
          Ok(id)
      }
  }}

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
          val fileFuture = dataStore.findFileById(id)
          fileFuture.map {
            file =>
              Ok(views.html.textWrapper(dataStore.fileText(file)))
          }
        case _ => Future {
          NotFound("")
        }
      }
  }

  def imageWrapper(id: String, extension: String) = {
    Future {
      Ok(views.html.imageWrapper(id, extension))
    }
  }
}
