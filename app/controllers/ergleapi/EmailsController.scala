package controllers.ergleapi

import javax.inject.{Inject, Singleton, Named}
import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.Email
import java.util.Date
import play.api.libs.functional.syntax._
import services.EmailDataStore

object EmailsController {

  val ownerPath = JsPath \ "owner"
  val fromAddressPath = JsPath \ "from"
  val recipientsAddressPath = JsPath \ "recipients"
  val subjectPath = JsPath \ "subject"
  val contentPath = JsPath \ "content"
  val receivedDatePath = JsPath \ "receivedDate"
  val replyToPath = JsPath \ "replyTo"
  val sentDatePath = JsPath \ "sentDate"

  implicit val emailReads: Reads[Email] = (ownerPath.read[String] and
    fromAddressPath.read[Array[String]] and
    recipientsAddressPath.read[Array[String]] and
    subjectPath.read[String] and
    contentPath.read[String] and
    receivedDatePath.read[Date] and
    replyToPath.read[Array[String]] and
    sentDatePath.read[Date]
    )(Email.apply _)
}

@Named
@Singleton
class EmailsController extends Controller {

  @Inject
  var dataStore: EmailDataStore = null

  def put = Action.async(parse.json) {request =>
    Future {
      request.body.validate[Email](EmailsController.emailReads) match {
        case s: JsSuccess[Email] => {
          val email: Email = s.get
          dataStore.save(email)
          Ok("")
        }
        case e: JsError => {
          BadRequest(JsError.toFlatJson(e).toString())
        }
      }
    }
  }

  def wrapper(id: String) = Action.async {
    dataStore.find(id).map {
      case Some(email) => Ok(views.html.emailWrapper(email))
      case None => NotFound(s"id $id not found")
    }
  }

}
