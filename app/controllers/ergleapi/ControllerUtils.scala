package controllers.ergleapi

import play.api.mvc.{AnyContent, Request}
import play.api.templates.HtmlFormat
import java.util.regex.{Matcher, Pattern}

trait ControllerUtils {

  val cookieName = "email"

  def getEmail[T](implicit request: Request[T]): Option[String] = {
    request.cookies.get(cookieName).map {
      _.value
    }
  }
}

object ControllerUtils {
  def formatUserInput(text: String) = {
    val sb = new StringBuilder(text.length)
    text.foreach {
      case '<' => sb.append("&lt;")
      case '>' => sb.append("&gt;")
      case '"' => sb.append("&quot;")
      case '\'' => sb.append("&#x27;")
      case '&' => sb.append("&amp;")
      case c => sb += c
    }
    convertLinksToHyperlinks(sb.replaceAllLiterally("\n","<br>").toString)
  }

  def convertLinksToHyperlinks(text: String): String = {
    val patt: Pattern = Pattern.compile("(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:\'\".,<>???“”‘’]))")
    val matcher: Matcher = patt.matcher(text)
    matcher.find match {
      case true if matcher.group(1).startsWith("http://") =>
        matcher.replaceAll("<a target=\"_blank\" href=\"$1\">$1</a>")
      case true =>
        matcher.replaceAll("<a target=\"_blank\" href=\"http://$1\">$1</a>")
      case false => text
    }
  }
}
