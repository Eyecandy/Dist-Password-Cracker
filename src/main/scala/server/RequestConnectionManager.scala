package server

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import server.RequestConnectionQueueActor.{LookingForJob, Register}
import server.WebServer.system

import scala.collection.immutable
import scala.concurrent.duration._

class RequestConnectionQueueActor extends Actor {
  implicit val dispatcher = system.dispatcher
  val supervisorActor = SupervisorActor.singletonSuperVisorActor
  val log = Logging(context.system, this)
  override def receive: Receive = {

    case Register(nodeName,password) =>
      val requestClientActor: ActorRef = context.actorOf(RequestConnectionActor.props(nodeName,password,System.currentTimeMillis()),nodeName)
      system.scheduler.schedule(5 seconds, 5 seconds, requestClientActor, RequestConnectionActor.currentTime())
      supervisorActor ! SupervisorActor.FindMeAWorker(requestClientActor)

    case LookingForJob(worker) =>
      val requestClients: immutable.Iterable[ActorRef] = context.children.seq
      requestClients.nonEmpty match {
        case true =>
          log.info("work found")
          val requestClient = requestClients.head
          requestClient ! RequestConnectionActor.WorkerReadyForWork(worker)
        case false =>
          log.info("idle")
          supervisorActor ! SupervisorActor.QueueWorkerAsIdle(worker)
      }
  }

}

object RequestConnectionQueueActor {
  val singletonRequestConnectionQueueActor = system.actorOf(Props(new RequestConnectionQueueActor()),"RequestConnectionQueueActor")
  case class Register(nodeName:String,password:String)
  case class LookingForJob(worker:ActorRef)

}
