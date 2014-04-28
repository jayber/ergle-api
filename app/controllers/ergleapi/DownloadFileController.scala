package controllers.ergleapi

import play.modules.reactivemongo.MongoController
import play.api.mvc.{Action, Controller}
import reactivemongo.bson.{BSONObjectID, BSONDocument}
import reactivemongo.api.gridfs.Implicits.DefaultReadFileReader
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject.{Singleton, Named, Inject}
import services.FileDataStore

/**
 * I just copied this from reactivemongo example 'cos I couldn't figure the fucking thing out...
 */

@Named
@Singleton
class DownloadFileController extends Controller with MongoController{

  @Inject
  var dataStore: FileDataStore = null

  def getAttachment(fileName: String) = Action.async { request =>
    val (id, extension) = FilesController.idFromFileName(fileName)
  // find the matching attachment, if any, and streams it to the client
    val file = dataStore.gridFS.find(BSONDocument("_id" -> BSONObjectID(id)))
    serve(dataStore.gridFS, file, CONTENT_DISPOSITION_INLINE)
  }
}
