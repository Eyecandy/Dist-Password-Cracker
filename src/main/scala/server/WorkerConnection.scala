package server


import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import server.WebServer.system
import server.WorkerConnectionActor.{GetIdleStatus, Register, SendJobToWorkerClient}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/*
  What WorkerConnectionActor (child of SuperVisorActor) does:
    - Register nodeName for the worker client's connection
    - sends (password,from, to) to actual worker client
    - !!!!Should be able to get and set idle status!!!!
    - !!!Should be able to set and get from and to (range)!!!
 */
class WorkerConnectionActor extends  Actor{

  var nodeName_ =""
  var range = Range("","")
  var idle = true
  var currentPasswordJob= ""
  val log = Logging(context.system, this)

  override def receive: Receive = {
    case Register(nodeName) => nodeName_ = nodeName
    case SendJobToWorkerClient(password,from,to) => {
      currentPasswordJob = password
      range = Range(from,to)
      idle = false
      WorkerConnectionActor.httpRequestWorkerClient(SendJobToWorkerClient(password,from,to),nodeName_,self)
    }
    case GetIdleStatus() => idle
  }
}

object WorkerConnectionActor {

def props = Props(new WorkerConnectionActor)
  case class SendJobToWorkerClient(password:String, from:String, to:String)
  case class Register(nodeName:String)
  case class GetIdleStatus()
  implicit val materializer = ActorMaterializer()
  implicit val dispatcher = system.dispatcher
  val port = 8081
  
  def httpRequestWorkerClient(send:SendJobToWorkerClient, nodeName:String, worker:ActorRef): Unit = {
    val workerAddress = s"http://${nodeName}:${port}/worker-client?password=${send.password}&from=${send.from}&to=${send.to}"
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = workerAddress))
    responseFuture
      .onComplete {
        case Success(res) => {
          println(res)
          val marshalFuture: Future[String] = Unmarshal(res.entity).to[String]
          val marshalResult: Option[Try[String]] = marshalFuture.value
          if (marshalResult.isDefined && marshalResult.get.isSuccess) {
            println(marshalResult.get.get)
            WebServer.superVisorActor ! SupervisorActor.ReportJobCompletion(marshalResult.get.get,worker)
          }
          else {
            throw new Exception("Marshalling failed & you might need to redo this job")
          }
        }
        case Failure(_) => sys.error("something wrong")
      }
  }
  /*
  Ping worker at schedule
   */
  /*
  def httpRequestWorkerClientPing(send:Send,nodeName:String,worker:ActorRef): Unit = {
    val workerAddress = s"http://${nodeName}:${port}/worker-client-ping"
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = workerAddress))
    responseFuture
      .onComplete {


      }
  }
  */
}
