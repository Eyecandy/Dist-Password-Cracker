package server

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import server.RequestConnectionManager.{LookingForJob, Register}
import server.DispatchServer.system
import scala.collection.immutable
import scala.concurrent.duration._

class RequestConnectionManager extends Actor {
  implicit val dispatcher = system.dispatcher
  val supervisor = SuperVisor.singletonSuperVisorActor
  val log = Logging(context.system, this)
  override def receive: Receive = {

    case Register(nodeName,password) =>
      val requestClientActor: ActorRef = context.actorOf(RequestConnection.props(nodeName,password,System.currentTimeMillis()),nodeName)
      system.scheduler.schedule(5 seconds, 5 seconds, requestClientActor, RequestConnection.currentTime())
      supervisor ! SuperVisor.FindMeAWorker(requestClientActor)

    case LookingForJob(worker) =>
      val requestClients: immutable.Iterable[ActorRef] = context.children.seq
      requestClients.nonEmpty match {
        case true =>
          val requestClient = requestClients.head
          requestClient ! RequestConnection.WorkerReadyForWork(worker)
        case false =>
          supervisor ! SuperVisor.QueueWorkerAsIdle(worker)
      }
  }

}

object RequestConnectionManager {
  val singletonRequestConnectionManger = system.actorOf(Props(new RequestConnectionManager()),"RequestConnectionManager")
  case class Register(nodeName:String,password:String)
  case class LookingForJob(worker:ActorRef)

}
