package controllers.ergleapi

import play.api.mvc.{Controller, SimpleResult, Action}
import scala.concurrent.Future
import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson.{BSONObjectID, BSONDateTime, BSONDocument, BSONValue}
import javax.inject.{Singleton, Named, Inject}
import services.{EventDataStore, EmailDataStore, FileDataStore}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.Event
import java.util.Date
import scala.collection.SortedMap

@Named
@Singleton
class EventsController extends Controller {

  @Inject
  var eventsDataStore: EventDataStore = null

  def listEvents = Action.async {
    request =>
      request.cookies.get("email") match {
        case Some(cookie) => getListEventsResult(cookie.value)
        case _ => Future(NotFound(""))
      }
  }

  def listEventsForEmail(email: String) = Action.async {
    getListEventsResult(email)
  }

  def getListEventsResult(email: String): Future[SimpleResult] = {
    val eventsFuture = eventsDataStore.listEvents(email)

    eventsFuture.map(events => displayResults(events))
  }

  def displayResults(results: List[Event]) = {
    Ok(views.html.eventList(results.reverse))
  }
}
