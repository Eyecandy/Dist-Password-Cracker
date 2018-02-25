package server

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.event.Logging
import server.WorkerConnection.Register
import DispatchServer._

import scala.concurrent.duration._
import scala.collection.mutable

/*
  What the SuperVisorActor does:
    - Receives worker-client connection from WebServer and creates a creates WorkerActor (child)
        schedules for pinging workerClient and schedules for checking (last response time vs current time)
    -


 */

class SuperVisor extends Actor {
  import server.SuperVisor._
  val log = Logging(context.system, this)
  var idleWorkers = List[ActorRef]()

  override def receive: Receive = {
    case WorkerRequestToJoin(nodeName) => {
      log.info(s"${nodeName} Requests to join DispatchServer")
      val worker: ActorRef = context.actorOf(WorkerConnection.props,nodeName)
      worker ! Register(nodeName)
      system.scheduler.schedule(0 seconds,5 seconds,worker,WorkerConnection.Ping())
      system.scheduler.schedule(3 seconds,5 seconds,worker,WorkerConnection.CheckLastResponse())
      requestConnectionManager ! RequestConnectionManager.LookingForJob(worker)
    }
    case QueueWorkerAsIdle(worker:ActorRef) => idleWorkers = worker :: idleWorkers
    case ReportJobCompletion(result:String,worker:ActorRef)  => {
      requestConnectionManager ! RequestConnectionManager.LookingForJob(worker)
    }
    case FindMeAWorker(requestConnection) => {
      idleWorkers.foreach(
        (worker => requestConnection ! RequestConnection.WorkerReadyForWork(worker)))
    }
    case WorkerConnectionDied(worker) => {
      idleWorkers = idleWorkers.filter(w => w != worker)
      worker ! PoisonPill
    }
  }
}
object SuperVisor {
  val workerQueue = mutable.Queue[ActorRef]()
  val singletonSuperVisorActor = system.actorOf(Props(new SuperVisor()),"SuperVisor")
  case class WorkerRequestToJoin(nodeName: String)
  case class ReportJobCompletion(result:String,worker:ActorRef)
  case class FindMeAWorker(requestConnection: ActorRef)
  case class QueueWorkerAsIdle(worker:ActorRef)
  case class WorkerConnectionDied(worker:ActorRef)
}
