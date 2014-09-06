package controllers.ergleapi

import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson.{BSONObjectID, BSONValue}
import scala.concurrent.{Await, Future}
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject.{Singleton, Named, Inject}
import services.FileDataStore
import java.io.File
import play.api.Logger
import scala.concurrent.duration._

object FilesController {
  def idFromFileName(fileName: String) = {
    val split = fileName.split( """\.""")
    (split(0), split(1).toLowerCase)
  }

  def fileIdFileName(file: ReadFile[BSONValue]): String = {
    fileIdFileName(file.id match {
      case theId: BSONObjectID => theId.stringify
      case _ => throw new RuntimeException("ReadFile.id is not a BSONObjectID")
    }, file.filename)
  }

  def fileIdFileName(id: String, filename: String): String = {
    new StringBuilder(id) + "." + extension(filename)
  }

  def extension(filename: String) = filename.split( """\.""").last

  def synthesizeFilename(id: String, filename: String) = {
    id + "." + extension(filename)
  }
}

@Named
@Singleton
class FilesController extends Controller {

  @Inject
  var dataStore: FileDataStore = null

  val bodyParser = BodyParser({
    header: RequestHeader =>
      Await.result(dataStore.exists(
        header.getQueryString("email").get,
        header.getQueryString("filename").get,
        header.getQueryString("lastModified").map(_.toLong).getOrElse(0)
      ), 1 minute) match {
        case false =>
          val tempFile = File.createTempFile("ergle", "tmp")
          Logger.debug(s"temp file: ${tempFile.getAbsolutePath}")
          parse.file(tempFile)(header)
        case true =>
          Logger.debug(s"duplicate file received: ${header.getQueryString("filename").get}, ${header.getQueryString("email").get}, ${header.getQueryString("lastModified").get}")
          parse.error(Future {
            BadRequest("duplicate file, do not resend")
          })(header)
      }
  })

  def putBare(email: String) = {
    doSave(email, dataStore.saveFileOnly)
  }

  def put(email: String) = {
    doSave(email, dataStore.saveFileEvent)
  }

  private def doSave(email: String, savingFunction: (File,String,String,Option[Long],Option[String],Option[String],Option[String]) => Future[String]) = {
    Action.async(bodyParser) { request =>
      savingFunction(request.body,
        request.getQueryString("filename").getOrElse("unknown-file"),
        email,
        request.getQueryString("lastModified").map(_.toLong),
        request.getQueryString("source"),
        request.getQueryString("tag"),
        request.getQueryString("shareTo")).map {
        id =>
          request.body.delete()
          Ok(id)
      }
    }
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
              Ok(views.html.wrapper(views.html.textWrapper(dataStore.fileText(file))))
          }
        case _ => Future {
          NotFound("")
        }
      }
  }

  def imageWrapper(id: String, extension: String) = {
    Future {
      Ok(views.html.wrapper(views.html.imageWrapper(id, extension)))
    }
  }
}
