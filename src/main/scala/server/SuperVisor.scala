package server

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import server.WorkerConnectionActor.{Register}
import WebServer._

import scala.collection.mutable

/*
  What the SuperVisorActor does:
    - Receives worker-client connection from WebServer and creates a creates WorkerActor (child)
    Registers the worker then calls a function called worker joins.
    - Receives ClientRequest from WebServer and queues it. !!!---CHANGE THIS--------!!!
    -
 */

class SupervisorActor extends Actor {
  import server.SupervisorActor._
  val log = Logging(context.system, this)
  val idleWorkers = List(ActorRef)

  override def receive: Receive = {
    case WorkerRequestToJoin(nodeName) => {
      println("worker Actor created in supervisor")
      val worker: ActorRef = context.actorOf(WorkerConnectionActor.props,nodeName)
      worker ! Register(nodeName)
      requestConnectionQueueActor ! RequestConnectionQueueActor.LookingForJob(worker)
    }
    case QueueWorkerAsIdle(worker:ActorRef) => worker::idleWorkers
    case ReportJobCompletion(result:String,worker:ActorRef)  => {
      log.info("job completed reported")
    }
  }
}

object SupervisorActor {
  val workerQueue = mutable.Queue[ActorRef]()
  val singletonSuperVisorActor = system.actorOf(Props(new SupervisorActor()),"SuperVisorActor")
  case class WorkerRequestToJoin(nodeName: String)
  case class ReportJobCompletion(result:String,worker:ActorRef)
  case class FindMeAWorker(requestConnectionActor: ActorRef)
  case class QueueWorkerAsIdle(worker:ActorRef)




}
