package controllers

import play.api.mvc.{Action, Controller}
import javax.inject.{Inject, Singleton, Named}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson.BSONValue
import java.util.Date


@Named
@Singleton
class FilesController extends Controller {

  @Inject
  var dataStore: DataStore = null

  def put = Action.async(parse.temporaryFile) {
    request =>
      dataStore.save(request.body.file, request.getQueryString("filename").getOrElse("unknown-file")).map {
        id =>
          Ok(id)
      }
  }

  def get = Action.async {
    dataStore.listFiles.map (mapResult)
  }

  def mapResult(results: List[ReadFile[BSONValue]]) = {
        Ok(<html>
          <body>
            <div id="files">
              <ul id="fileList">
                {results.map {
                file: ReadFile[BSONValue] =>
                  <li>
                    {file.filename}
                    ,
                    {file.id}
                    ,
                    {file.uploadDate.map(_.asInstanceOf[Date])}
                  </li>
              }}
              </ul>
            </div>
          </body>
        </html>
        ).as("text/html")
  }
}
