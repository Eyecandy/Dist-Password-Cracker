package server

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import server.WorkerActor.{Register, Send}

import scala.collection.mutable

class SupervisorWorkerActor extends Actor {

  import server.SupervisorWorkerActor._

  val log = Logging(context.system, this)

  override def receive: Receive = {
    case workerConnection(nodeName) => {
      val newWorker: ActorRef = context.actorOf(WorkerActor.props)
      newWorker ! Register(nodeName)
      workerJoins(newWorker)
      log.info(s"worker with nodeName: ${nodeName} arrived")
    }
    case ClientRequest(nodeName, password) => {
      jobComesIn(ClientRequest(nodeName,password))
      log.info(s"${nodeName},${password} arrived")
    }
    case ReportJobCompletion(result:String,worker:ActorRef)  => {
      log.info("job completed reported")
      workerJoins(worker)
    }
  }
}

object SupervisorWorkerActor {
  val workerQueue = mutable.Queue[ActorRef]()
  var jobQueue = mutable.Queue[ClientRequest]()
  val props = Props(new SupervisorWorkerActor())
  case class workerConnection(nodeName: String)
  case class ClientRequest(nodeName: String, password: String)
  case class ReportJobCompletion(result:String,worker:ActorRef)

  var currentRange = "AAAAAAAA"

  def jobComesIn(clientRequest: ClientRequest): Unit = {
    println(jobQueue)
    jobQueue.enqueue(clientRequest)
  }

  def workerJoins(worker: ActorRef): Unit = {
    val job = jobQueue.front.password
    val from = currentRange
    val to = Range.start(currentRange)
    to.isDefined match {
      case true => {
        worker ! Send(job, from, to.get)
        currentRange = to.get
      }
      case false => {
        jobQueue.dequeue()
        var currentRange = "AAAAAAAA"
        workerJoins(worker)
      }
    }
  }
}
