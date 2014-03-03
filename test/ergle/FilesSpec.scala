package ergle

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mock.Mockito
import utils.Global
import java.io.File
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson.BSONValue
import play.api.mvc.{Cookie, Action}
import controllers.ergleapi.DataStore
import controllers.FilesController

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

      dataStore.save(any[File], anyString, anyString, anyLong) returns Future("id")
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
      val email = "email"
      dataStore.listFiles(Some(email)) returns futureFile

      val response = route(FakeRequest(GET, "/files/").withHeaders(("Accept", "text/html")).withCookies(new Cookie("email",email))).get

      there was one(dataStore).listFiles(Some(email)) andThen one(futureFile).map(any)(any)
    }

    "output html wrapper for images" in new WithApplication {
      val response = route(FakeRequest(GET, "/files/123A.jpg/wrapper")).get

      status(response) must equalTo(OK)
      contentAsString(response).trim must beEqualTo("""<img src="/files/123A.jpg">""")
    }

    "output html wrapper for text files" in new WithApplication {
      val filesController = Global.ctx.getBean(classOf[FilesController])
      val dataStore = mock[DataStore]
      filesController.dataStore = dataStore
      val future = mock[Future[Option[Nothing]]]
      dataStore.findFileById(anyString) returns future
      dataStore.fileText(any) returns "this is the file text"
      val response = route(FakeRequest(GET, "/files/123A.txt/wrapper")).get

      status(response) must equalTo(OK)
      contentAsString(response).trim must beEqualTo("""<span>this is the file text</span>""")
    }

  }

}
