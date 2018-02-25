package server

import akka.actor
import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.event.Logging
import server.WorkerConnection.SendJobToWorkerClient

/*
  what RequesConnectionActor does:
    - Stores last time received ping from client
    - Updates last time pinged variable when pinged.
    - Is scheduled by SuperVisorActor to check last time pinged vs currentTime.
      if time since last ping is greater than 15 sec. Mark this connections as dead.
    - Worker comes in:
      send this request connections password job to it and update this request connections range.
      if failed work is present. I.e a worker died that performed a range from this request client.
      then range gets priority.
 */

class RequestConnection(nodeName: String, password: String, pingTime: Long) extends Actor {
  var range = Range("AAAAAAAAA","AABAAAAA")
  var lastPingTime = pingTime;
  val log = Logging(context.system, this)
  var failedWork = List[Range]()
  import RequestConnection._

  override def receive: Receive = {
    case currentTime() => {
      val nanoSecSinceLastPing = System.currentTimeMillis() - lastPingTime;
      if (nanoSecSinceLastPing > 15000) {
        log.error(s"Request client connection appears to be lost, Killing actor: ${self.path}")
        self ! PoisonPill
      }
    }
    case ping() => lastPingTime = System.currentTimeMillis()

    case WorkerReadyForWork(worker) =>
      if (failedWork.nonEmpty) {
        val redoWork  = failedWork.head
        failedWork = failedWork.tail
        worker ! SendJobToWorkerClient(password,redoWork.from,redoWork.to,self)
      }
      else {
        worker ! SendJobToWorkerClient(password,range.from,range.to,self)
        val newTo = RangeUpdater.start(range.to)
        newTo.isDefined match {
          case true => range = Range(range.to,newTo.get)
          case false => log.error("RangeUpdate FAILED, NONE Value returned")
        }
      }

    case WorkerDied(range:Range) => range :: failedWork

    case _ => throw new Exception("Don't understand what is being sent to me!!")
  }
}

object RequestConnection {
  def props(nodeName: String, password: String, datetime: Long): actor.Props = Props(new RequestConnection(nodeName, password, datetime))
  case class currentTime()
  case class ping()
  case class WorkerReadyForWork(worker:ActorRef)
  case class WorkerDied(range:Range)

}









