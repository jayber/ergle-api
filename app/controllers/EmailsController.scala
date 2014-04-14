package controllers

import javax.inject.{Singleton, Named}
import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext


@Named
@Singleton
class EmailsController extends Controller {

  def put = Action.async {request =>
    Future {
      println(request.body)
      Ok("ok")
    }
  }

}
