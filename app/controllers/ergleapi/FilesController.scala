package controllers.ergleapi

import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson.{BSONObjectID, BSONValue}
import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject.{Singleton, Named, Inject}
import services.FileDataStore
import java.io.File
import play.api.Logger
import play.api.libs.iteratee.Iteratee

object FilesController {
  def idFromFileName(fileName: String) = {
    val split = fileName.split( """\.""")
    (split(0), split(1).toLowerCase)
  }

  def fileIdFileName(file: ReadFile[BSONValue]) = {
    new StringBuilder(file.id match {
      case theId: BSONObjectID => theId.stringify
      case _ => throw new RuntimeException("ReadFile.id is not a BSONObjectID")
    }) + "." + extension(file.filename)
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

  def put = {
    val tempFile = File.createTempFile("ergle","tmp")
    Logger.debug(s"temp file: ${tempFile.getAbsolutePath}")
    Action.async(parse.file(tempFile)) {
    request =>
      dataStore.save(request.body,
        request.getQueryString("filename").getOrElse("unknown-file"),
        request.getQueryString("email").get,
        request.getQueryString("lastModified").map(_.toLong),
        request.getQueryString("source")
      ).map {
        id =>
          tempFile.delete()
          Ok(id)
      }
  }}

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
