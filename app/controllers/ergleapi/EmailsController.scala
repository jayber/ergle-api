package controllers.ergleapi

import javax.inject.{Inject, Singleton, Named}
import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import services.EmailDataStore
import org.apache.james.mime4j.message.DefaultMessageBuilder
import java.io.ByteArrayInputStream
import play.api.Logger

object EmailsController {

  val ownerPath = JsPath \ "owner"
  val contentPath = JsPath \ "content"

  implicit val emailReads: Reads[EmailRequest] = (
    ownerPath.read[String] and
      contentPath.read[String]
    )(EmailRequest.apply _)
}

case class EmailRequest(owner: String,
                        content: String)

@Named
@Singleton
class EmailsController extends Controller {

  @Inject
  var dataStore: EmailDataStore = null

  def parseContent(emailRequest: EmailRequest) = {
    val input: ByteArrayInputStream = new ByteArrayInputStream(emailRequest.content.getBytes("UTF-8"))
    val message = (new DefaultMessageBuilder).parseMessage(input)
    (emailRequest.owner, message)
  }

  def put = Action.async(parse.json) {
    request =>
      request.body.validate[EmailRequest](EmailsController.emailReads) match {
        case s: JsSuccess[EmailRequest] => {
          val emailRequest: EmailRequest = s.get
          dataStore.save(parseContent(emailRequest)).onFailure {
            case t: Throwable => Logger.error("save email error", t)
          }

          Future {
            Ok("")
          }
        }
        case e: JsError => {
          Future {
            BadRequest(JsError.toFlatJson(e).toString())
          }
        }
      }
  }


def wrapper (id: String) = Action.async {
dataStore.find (id).map {
case Some (email) => Ok (views.html.emailWrapper (email) )
case None => NotFound (s"id $id not found")
}
}
}
