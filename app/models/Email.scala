package models

import java.util.Date

case class Email(owner: String,
                 from: String,
                 to: Array[String],
                 cc: Option[Array[String]],
                 subject: String,
                 body: Array[AnyRef],
                 receivedDate: Date,
                 replyTo: Option[Array[String]])

case class InlineEmailBody(mimeType: String,
                      content: String)
