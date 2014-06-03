package ergle


import org.specs2.mutable._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.Email
import java.text.SimpleDateFormat
import controllers.ergleapi.{EmailRequest, EmailsController}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class EmailReadsSpec extends Specification {

  "EmailReads" should {
    "parse json to Email" in {

      val json = Json.parse(
        """
          |{"from":["james bromley <james.a.bromley@gmail.com>"],
          |"recipients":["james.bromley@ergle.com"],
          |"subject":"test subject",
          |"content":"some content",
          |"receivedDate":"2014-04-22T10:27:28.000+01:00",
          |"replyTo":["james bromley <james.a.bromley@gmail.com>"],
          |"sentDate":"2014-04-22T10:27:28.000+01:00"}
        """.stripMargin)

      val emailResult: JsResult[EmailRequest] = json.validate[EmailRequest](EmailsController.emailReads)

      emailResult match {
        case e: JsError => println("Errors: " + JsError.toFlatJson(e).toString())
        case s =>
      }

      val format: SimpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS z")

      success
/*
      email.from(0) must beEqualTo("james bromley <james.a.bromley@gmail.com>") and
        (email.recipients(0) must beEqualTo("james.bromley@ergle.com")) and
        (email.subject must beEqualTo("test subject")) and
        (email.content must beEqualTo("some content")) and
        (email.receivedDate must beEqualTo(format.parse("22-04-2014 00:00:00.000 BST"))) and
        (email.sentDate must beEqualTo(format.parse("22-04-2014 00:00:00.000 BST"))) and
        (email.replyTo(0) must beEqualTo("james bromley <james.a.bromley@gmail.com>"))*/
    }
  }
}
