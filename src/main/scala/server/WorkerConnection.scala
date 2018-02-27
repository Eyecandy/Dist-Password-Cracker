package server


import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import server.DispatchServer.system
import server.WorkerConnection.{CheckLastResponse, Ping, Register, SendJobToWorkerClient, ShutdownMessage, UpdateLastResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/*
  What WorkerConnectionActor (child of SuperVisorActor) does:
    - Register: nodeName for the worker client's connection is registered
    - SendJobToWorkerClient:
      saves all info to inside the WorkerConnection
      then send (password,from, to) to the actual worker client
    - Ping:send ping to worker
    - UpdateLastResponse: update time of last response from worker
    - CheckLastResponse: scheduled to check current time vs last response time
 */
class WorkerConnection extends  Actor{
  var nodeName_ =""
  var range = Range("","")
  var idle = true
  var currentPasswordJob= ""
  val log = Logging(context.system, this)
  var lastResponse = System.currentTimeMillis()
  var requestClient_ : ActorRef = ActorRef.noSender
  implicit val materializer = DispatchServer.materializer
  implicit val dispatcher =  DispatchServer.dispatcher


  val port = 8081

  def httpRequestWorkerClient(password:String,from:String,to:String, nodeName:String, worker:ActorRef): Unit = {
    val workerAddress = s"http://${nodeName}:${port}/worker-client?password=${password}&from=${from}&to=${to}"
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = workerAddress))
    responseFuture
      .onComplete {
        case Success(res) => {
          val marshalFuture: Future[String] = Unmarshal(res.entity).to[String]
          val marshalResult: Option[Try[String]] = marshalFuture.value
          if (marshalResult.isDefined && marshalResult.get.isSuccess) {
            println(res.entity)
            DispatchServer.superVisorActor ! SuperVisor.ReportJobCompletion(marshalResult.get.get,worker)
          }
          else {
            throw new Exception("Marshalling failed & you might need to redo this job")
          }
        }
        case Failure(_) => sys.error("something wrong")
      }
  }

  def httpRequestWorkerClientPing(nodeName:String,worker:ActorRef): Unit = {
    val workerAddress = s"http://${nodeName}:${port}/worker-client-ping"
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = workerAddress))
    responseFuture
      .onComplete {
        case Success(res) => worker ! UpdateLastResponse()
        case Failure(_) => throw new Exception("WorkerConnection Ping fails!")
      }
  }

  def httpRequestWorkerClientShutdown(nodeName:String): Unit = {
    val workerAddress = s"http://${nodeName}:${port}/worker-client-shutdown"
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = workerAddress))
    responseFuture
      .onComplete {
        case Success(res) => println(res)
        case Failure(_) => throw new Exception("WorkerConnection Ping fails!")
      }
  }

  override def receive: Receive = {
    case Register(nodeName) => nodeName_ = nodeName
    case SendJobToWorkerClient(password,from,to,requestClient:ActorRef) => {
      requestClient_ = requestClient
      currentPasswordJob = password
      range = Range(from,to)
      idle = false
      log.info(s"Dispatcher assigns ${nodeName_} the job: psw: ${password} & range:${from}-${to}")
      httpRequestWorkerClient(password,from,to,nodeName_,self)
    }
    case Ping() => httpRequestWorkerClientPing(nodeName_,self)
    case UpdateLastResponse() => lastResponse = System.currentTimeMillis()
    case CheckLastResponse() => {
      if (System.currentTimeMillis() - lastResponse > 15000) {
        requestClient_ ! RequestConnection.WorkerDied(range)
        SuperVisor.singletonSuperVisorActor ! SuperVisor.WorkerConnectionDied(self)
      }
    }
    case ShutdownMessage() => httpRequestWorkerClientShutdown(nodeName_)

  }
}

object WorkerConnection {

def props = Props(new WorkerConnection)
  case class SendJobToWorkerClient(password:String, from:String, to:String,requestClient:ActorRef)
  case class Register(nodeName:String)
  case class Ping()
  case class UpdateLastResponse()
  case class CheckLastResponse()
  case class ShutdownMessage()


}
