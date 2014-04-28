package models

import java.util.Date

case class Email(owner: String,
                 from: Array[String],
                 recipients: Array[String],
                 subject: String,
                 content: String ,
                 receivedDate: Date,
                 replyTo: Array[String],
                 sentDate: Date)
