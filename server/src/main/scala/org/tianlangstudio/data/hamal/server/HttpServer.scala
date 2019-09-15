package org.tianlangstudio.data.hamal.server

import _root_.akka.actor.ActorSystem
import _root_.akka.stream.ActorMaterializer
import org.tianlangstudio.data.hamal.common.Logging

/**
  *
  * Created by zhuhq on 17-4-6.
  */
class HttpServer(jobHandler: IJobHandler, dataxConf: DataxConf) (implicit val actorSystem: ActorSystem) extends Logging {

  implicit val executionContext = actorSystem.dispatcher
  implicit val mat = ActorMaterializer()
  implicit object TaskResultJsonFormat extends RootJsonWriter[TaskResult] {
    override def write(taskResult: TaskResult): JsValue = {
      JsObject(
        "success" -> JsBoolean(taskResult.success),
        "msg" -> JsString(taskResult.msg)
      )
    }
  }
  implicit object TaskCostJsonFormat extends RootJsonWriter[TaskCost] {
    override def write(taskCost: TaskCost): JsValue = {
      JsObject(
        "beginTime" -> JsString(taskCost.beginTime),
        "endTime" -> JsString(taskCost.endTime),
        "cost" -> JsString(taskCost.cost)
      )
    }
  }

  val host = dataxConf.getString(Constants.HTTP_SERVER_HOST, "127.0.0.1")
  val port = dataxConf.getInt(Constants.HTTP_SERVER_PORT, 9808)

  val pathRoot = dataxConf.getString(Constants.HTTP_SERVER_ROOT, "dataxserver")

  val route: Route =

    pathPrefix(s"$pathRoot" / "job") {
      get {
        pathPrefix(Segment) { jobId =>
          val taskResult = jobHandler.getJobResult(jobId)
          complete(taskResult)
        }
      } ~
      get {
        path("status" / Segment) { jobId =>
          val taskStatus = jobHandler.getJobStatus(jobId)
          complete(taskStatus)
        }
      } ~
      get {
        path("cost" / Segment) { jobId =>
          val taskCost = jobHandler.getJobCost(jobId)
          complete(taskCost)
        }
      } ~
      post {
        entity(as[String]) { jobDesc =>
          parameterMap {parameterMap =>

            complete(jobHandler.submitJobWithParams(jobDesc, parameterMap))
          }
        }
      }
    }

  logger.info(s"http server host:$host,port:$port, rootPath:$pathRoot")
  val bindingFuture = Http().bindAndHandle(route, host, port)
  logger.info(s"http server is running on $host:$port/$pathRoot")

  def stop(): Unit = {
    bindingFuture.flatMap(_.unbind())
  }



}
