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
import play.api.Logger
import java.text.{ParseException, SimpleDateFormat}

@Named
@Singleton
class EventsController extends Controller {

  @Inject
  var eventsDataStore: EventDataStore = null

  def noContent = Action {
    Ok("")
  }

  def post = Action.async(parse.urlFormEncoded) {
    request =>
      request.cookies.get("email") match {
        case Some(cookie) =>
          val df = new SimpleDateFormat("dd/MM/yyyy HH:mm")
          try {
            val date = request.body.get("date").map { value =>
              df.parse(value(0)).getTime
            }
            eventsDataStore.saveEvent(
              cookie.value,
              date,
              request.body.get("title").map(_(0)).get,
              "intent",
              "events/nocontent",
              request.body.get("tag").map(_(0)) match {
                case Some("") => None
                case other => other
              }).map {
              lastError =>
                lastError.ok match {
                  case true => Redirect("/"+cookie.value)
                  case false => InternalServerError(lastError.message)
                }
            }
          } catch {
            case _: ParseException => Future { BadRequest("bad date format") }
          }
        case _ => Future(BadRequest("not logged in"))
      }
  }

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
    Ok(views.html.eventList(results))
  }
}
