package controllers

import play.api.mvc.{Action, Controller}
import javax.inject.{Inject, Singleton, Named}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson.BSONValue
import scala.concurrent.Future

object FilesController {
  def idFromFileName(fileName: String) = {
    val split = fileName.split( """\.""")
    (split(0), split(1))
  }
}

@Named
@Singleton
class FilesController extends Controller {

  @Inject
  var dataStore: DataStore = null

  def put = Action.async(parse.temporaryFile) {
    request =>
      dataStore.save(request.body.file, request.getQueryString("filename").getOrElse("unknown-file")).map {
        id =>
          Ok(id)
      }
  }

  def listFiles = Action.async {
    dataStore.listFiles.map(mapResult)
  }


  def mapResult(results: List[ReadFile[BSONValue]]) = {
    Ok(views.html.fileList(results))
  }

  def wrapper(fileName: String) = Action.async {
    request => Future {
      val (id, extension) = FilesController.idFromFileName(fileName)
      Ok(views.html.imageWraper(id, extension))
    }
  }

}
