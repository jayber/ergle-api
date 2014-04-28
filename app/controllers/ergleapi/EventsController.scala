package controllers.ergleapi

import play.api.mvc.{Controller, SimpleResult, Action}
import scala.concurrent.Future
import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson.{BSONObjectID, BSONDateTime, BSONDocument, BSONValue}
import javax.inject.{Singleton, Named, Inject}
import services.{EmailDataStore, FileDataStore}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.Event
import java.util.Date

@Named
@Singleton
class EventsController extends Controller {

  @Inject
  var fileDataStore: FileDataStore = null
  @Inject
  var emailsDataStore: EmailDataStore = null

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

  def makeEvents(emails: List[BSONDocument], files: List[ReadFile[BSONValue]]): List[Event] = {
    (emails.map {
      emailDocument => Event("email", emailDocument.getAs[String]("subject").get, new Date(emailDocument.getAs[BSONDateTime]("receivedDate").get.value), s"""/emails/${emailDocument.getAs[BSONObjectID]("_id").get.stringify}""")
    } ++
      files.map {
        file => Event("file", file.filename, new Date(file.metadata.getAs[BSONDateTime]("lastModified").get.value), s"/files/${FilesController.fileIdFileName(file)}")
      }).sortBy(_.sortDate)
  }

  def getListEventsResult(email: String): Future[SimpleResult] = {
    val events =
      for {
        emails <- emailsDataStore.listEmails(email)
        files <- fileDataStore.listFiles(Some(email))
      } yield makeEvents(emails, files)

    events.map(displayResults)
  }

  def displayResults(results: List[Event]) = {
    Ok(views.html.eventList(results.reverse))
  }
}
