package controllers.ergleapi

import javax.inject.{Inject, Singleton, Named}
import play.api.mvc.{Controller, Action}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.{EmailDataStore, FileDataStore}
import scala.collection.SortedSet


@Named
@Singleton
class ContactsController extends Controller {

  @Inject
  var fileDataStore: FileDataStore = null

  @Inject
  var emailDataStore: EmailDataStore = null

  def contacts = Action.async { request =>
    request.cookies.get("email") match {
      case Some(cookie) => getContacts(cookie.value, request.getQueryString("zoom"))
      case _ => Future(NotFound(""))
    }
  }

  def contactsForEmail(email: String) = Action.async { request =>
    getContacts(email, request.getQueryString("zoom"))
  }

  def getContacts(email: String, zoom: Option[String]) = {
    val contactsFuture = emailDataStore.listContacts(email).map {set =>
      set.toList.sorted
    }

    contactsFuture.map { contacts =>
      Ok(views.html.contacts(contacts, zoom))
    }
  }
}
