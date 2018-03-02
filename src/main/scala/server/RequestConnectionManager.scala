package server

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import server.RequestConnectionManager.{Dequeue, LookingForJob, Register}
import server.DispatchServer.system

import scala.collection.immutable
import scala.concurrent.duration._


/*
  What the RequestConnectionManager does:
    - Register:
        Create a requestConnection actor
        schedules to check (Last Ping Time vs Current Time)
        Asks SuperVisor for idle workers
    - LookingForJob:
      Worker comes in looking for job i.e  if a requestConnection exist a job must exist
      if it exists send worker reference to the job
 */

class RequestConnectionManager extends Actor {
  implicit val dispatcher = system.dispatcher
  val supervisor = SuperVisor.singletonSuperVisorActor
  val log = Logging(context.system, this)
  val requestClientPort = 8082

  override def receive: Receive = {
    case Register(nodeName,password) =>
      log.info(s"$nodeName with $password was registerd by dispatch server.")
      val requestConnection: ActorRef = context.actorOf(RequestConnection.props(nodeName,password,System.currentTimeMillis()),nodeName)
      system.scheduler.schedule(5 seconds, 5 seconds, requestConnection, RequestConnection.currentTime())
      supervisor ! SuperVisor.FindMeAWorker(requestConnection)

    case LookingForJob(worker) =>
      val requestClients: immutable.Iterable[ActorRef] = context.children.seq
      requestClients.nonEmpty match {
        case true =>
          val requestClient = requestClients.head
          requestClient ! RequestConnection.WorkerReadyForWork(worker)
        case false =>
          supervisor ! SuperVisor.QueueWorkerAsIdle(worker)
      }
    case Dequeue(psw) => context.children.head ! RequestConnection.DecryptedPassword(psw)

  }

}

object RequestConnectionManager {
  val singletonRequestConnectionManger = system.actorOf(Props(new RequestConnectionManager()),"RequestConnectionManager")
  case class Register(nodeName:String,password:String)
  case class LookingForJob(worker:ActorRef)
  case class Dequeue(psw:String)

}
