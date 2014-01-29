package controllers

import play.api.mvc.{Action, Controller}
import javax.inject.{Inject, Singleton, Named}
import play.api.libs.concurrent.Execution.Implicits.defaultContext


@Named
@Singleton
class FilesController extends Controller {

  @Inject
  var dataStore: DataStore = null

  def put = Action.async(parse.temporaryFile) {
    request =>
      dataStore.save(request.body.file, "filename").map {
        id =>
          Ok(id)
      }
  }
}
