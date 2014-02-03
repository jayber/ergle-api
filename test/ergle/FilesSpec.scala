package ergle

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class FilesSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "allow files to be PUT" in new WithApplication {
      val response = route(FakeRequest(PUT, "/files/")).get

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/plain")
      (contentAsString(response) must not).beEmpty
    }
  }
}
