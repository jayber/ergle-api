package controllers.ergleapi

import play.api.mvc._
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
          saveLoggedInPost(request, cookie.value)
        case _ => Future(BadRequest("not logged in"))
      }
  }

  def saveLoggedInPost(request: Request[Map[String, Seq[String]]], loggedInEmail: String): Future[Result] = {
    val df = new SimpleDateFormat("yyyy/MM/dd HH:mm")

    def getSubmittedTag: Option[String] = {
      request.body.get("tag").map(_(0)) match {
        case Some("") => None
        case other => other
      }
    }

    def getSubmittedDate: Option[Long] = {
      request.body.get("date").map { value =>
        df.parse(value(0)).getTime
      }
    }

    def getSubmittedTo: Seq[String] = {
      request.body.get("shareTo") match {
        case Some(to) => to
        case None => Seq()
      }
    }

    try {
      request.body.get("type") match {
        case Some(types) => (types(0) match {
          case "intent" =>
            eventsDataStore.saveEvent(
              loggedInEmail,
              getSubmittedDate,
              None,
              "intent",
              None,
              getSubmittedTag,
              loggedInEmail +: getSubmittedTo,
              request.body.get("title").map(_(0))
            )
          case "message" =>
            eventsDataStore.saveEvent(
              loggedInEmail,
              None,
              None,
              "message",
              None,
              getSubmittedTag,
              loggedInEmail +: getSubmittedTo,
              request.body.get("message").map(_(0))
            )
        }).map {
          lastError =>
            lastError.ok match {
              case true => Redirect("/" + loggedInEmail)
              case false => InternalServerError(lastError.message)
            }
        }
        case None => Future {
          BadRequest("event type missing")
        }
      }
    } catch {
      case _: ParseException => Future {
        BadRequest("bad date format")
      }
    }
  }

  def listEvents = Action.async { implicit request =>
      println(request.getQueryString("zoom"))
      request.cookies.get("email") match {
        case Some(cookie) => getListEventsResult(cookie.value)
        case _ => Future(NotFound(""))
      }
  }

  def listEventsForEmail(email: String) = Action.async { implicit request =>
    getListEventsResult(email)
  }

  def getListEventsResult(email: String)(implicit request: Request[AnyContent]): Future[Result] = {
    val eventsFuture = eventsDataStore.listEvents(email)

    eventsFuture.map(events => displayResults(events, email))
  }

  def displayResults(results: List[Event], email: String)(implicit request: Request[AnyContent]) = {
    Ok(
      request.getQueryString("zoom") match {
        case Some("day") => views.html.eventListByDay(results, email)
        case _ => views.html.eventList(results, email)
      })
  }
}
