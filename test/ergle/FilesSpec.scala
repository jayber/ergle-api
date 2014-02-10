package ergle

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._
import scala.xml.XML
import org.specs2.mock.Mockito
import utils.Global
import controllers.{DataStore, FilesController}
import java.io.File
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson.BSONValue

@RunWith(classOf[JUnitRunner])
class FilesSpec extends Specification with Mockito {

  "Application" should {

    "send 404 on a bad request" in new WithApplication {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "allow files to be PUT" in new WithApplication {

      val filesController = Global.ctx.getBean(classOf[FilesController])
      val dataStore = mock[DataStore]
      filesController.dataStore = dataStore

      dataStore.save(any[File], anyString) returns Future("id")
      val response = route(FakeRequest(PUT, "/files/")).get

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/plain")
      (contentAsString(response) must not).beEmpty

    }

    "retrieve list of files" in new WithApplication {

      val filesController = Global.ctx.getBean(classOf[FilesController])
      val dataStore = mock[DataStore]
      filesController.dataStore = dataStore
      val futureFile = mock[Future[List[ReadFile[BSONValue]]]]
      dataStore.listFiles returns futureFile
      futureFile.map()

      val response = route(FakeRequest(GET, "/files/").withHeaders(("Accept", "text/html"))).get

      val ul = XML.loadString(contentAsString(response)) \\ "ul"

      val matchUl = {
        (ul \ "@id").text match {
          case "fileList" => Some(ul)
          case _ => None
        }
      }

      matchUl.map {
        _ \\ "li"
      } must beSome
    }

  }
}
