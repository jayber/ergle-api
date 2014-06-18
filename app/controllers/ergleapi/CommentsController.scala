package controllers.ergleapi

import play.api.mvc.{Controller, SimpleResult, Action}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject.{Inject, Singleton, Named}
import services.EventDataStore
import java.util.Date


@Named
@Singleton
class CommentsController extends Controller with ControllerUtils {

  @Inject
  var dataStore: EventDataStore = null

  def post(eventId: String) = Action.async(parse.text) { implicit request =>
    val saveFuture = dataStore.saveComment(eventId, getEmail.get, request.body)
    saveFuture.map{ lastError =>
      lastError.ok match {
        case true => Ok(views.html.comment("Today",new Date(),getEmail.get,request.body))
        case false => InternalServerError(lastError.message)
      }
    }
  }

}
