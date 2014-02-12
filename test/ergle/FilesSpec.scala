package ergle

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._
import scala.xml.{NodeSeq, XML}
import org.specs2.mock.Mockito
import utils.Global
import controllers.{FilesController, DataStore}
import java.io.File
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson.BSONValue
import play.api.mvc.Action
import java.util.Date

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


      val response = route(FakeRequest(GET, "/files/").withHeaders(("Accept", "text/html"))).get

      there was one(dataStore).listFiles andThen one(futureFile).map(any)(any)
    }

    "output html wrapper for images" in new WithApplication {
      val response = route(FakeRequest(GET, "/files/123A.jpg/wrapper")).get

      status(response) must equalTo(OK)
      contentAsString(response).trim must beEqualTo("""<img src="/files/123A.jpg">""")
    }

    "output html wrapper for text files" in new WithApplication {
      val response = route(FakeRequest(GET, "/files/123A.txt/wrapper")).get

      status(response) must equalTo(OK)
      contentAsString(response).trim must beEqualTo("""<span>this is the file text</span>""")
    }

  }

  "Files mapping function" should {
    "work" in new WithApplication {
      val controller = new FilesController
      val files = mock[List[ReadFile[BSONValue]]]
      files.map[NodeSeq,List[NodeSeq]](any)(any) returns List(<li></li>)

      val result = Action {controller.mapResult(files)}.apply(FakeRequest(GET, "/files/").withHeaders(("Accept", "text/html")))

      val ul = XML.loadString(contentAsString(result)) \\ "ul"

      val matchUl =
        (ul \ "@id").text match {
          case "fileList" => Some(ul)
          case _ => None

      }

      val matchLi: Boolean = matchUl match {
        case Some(result) => (result \\ "li").size match {
          case 0 => false
          case _ => true
        }
        case _ => false
      }

      matchLi must beTrue
    }

  }
}
