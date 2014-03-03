package controllers.ergleapi

import javax.inject.{Inject, Singleton, Named}
import play.api.mvc.{Controller, Action}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext


@Named
@Singleton
class ContactsController extends Controller {

  @Inject
  var dataStore: DataStore = null

  def contacts = Action.async { request =>
    request.cookies.get("email") match {
      case Some(cookie) => getContacts(cookie.value)
      case _ => Future(NotFound(""))
    }
  }

  def contactsForEmail(email: String) = Action.async {
    getContacts(email)
  }

  def getContacts(email: String) = {
    dataStore.listContacts(email).map {
      contacts =>
      Ok(views.html.contacts(contacts))
    }
  }
}
