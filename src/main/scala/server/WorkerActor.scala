package server


import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.{Http, unmarshalling}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import server.WebServer.system
import server.WorkerActor.{Register, Send}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


class WorkerActor extends  Actor{
  var nodeName_ =""
  val port = 0
  var active = false;
  var job = ""
  val log = Logging(context.system, this)

  override def receive: Receive = {
    case Register(nodeName) => nodeName_ = nodeName
    case Send(password,from,to) => {
      WorkerActor.httpRequestWorkerClient(Send(password,from,to),nodeName_,self)
    }
  }
}

object WorkerActor {

def props = Props(new WorkerActor)
  case class Send(password:String,from:String,to:String)
  case class Register(nodeName:String)
  implicit val materializer = ActorMaterializer()
  implicit val dispatcher = system.dispatcher
  val port = "8081"

  def httpRequestWorkerClient(send:Send,nodeName:String,worker:ActorRef): Unit = {
    val workerAddress = s"http://${nodeName}:${port}/worker-client?password=${send.password}&from=${send.from}&to=${send.to}"
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = workerAddress))
    responseFuture
      .onComplete {
        case Success(res) => {
          val marshalFuture: Future[String] = Unmarshal(res.entity).to[String]
          val marshalResult: Option[Try[String]] = marshalFuture.value
          if (marshalResult.isDefined && marshalResult.get.isSuccess) {
            WebServer.superVisorWorkerActor ! SupervisorWorkerActor.ReportJobCompletion(marshalResult.get.get,worker)
          }
          else {
            throw new Exception("Marshalling failed & you might need to redo this job")
          }
        }
        case Failure(_) => sys.error("something wrong")
      }
  }
}
