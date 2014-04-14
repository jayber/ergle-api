package services

import reactivemongo.api.MongoDriver
import scala.concurrent.ExecutionContext.Implicits.global

trait DataStore {
  val driver = new MongoDriver
  val connection = driver.connection(List("localhost"))
  val db = connection("ergle")

}
