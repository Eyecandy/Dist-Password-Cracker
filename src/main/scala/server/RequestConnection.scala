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
 */

class RequestConnection(nodeName: String, password: String, pingTime: Long) extends Actor {
  var range = Range("AAAAAAAAA","AABAAAAA")
  var lastPingTime = pingTime;
  val log = Logging(context.system, this)
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
      log.info("job is sent to workerConnection")
      worker ! SendJobToWorkerClient(password,range.from,range.to)
      val newTo = RangeUpdater.start(range.to)
      newTo.isDefined match {
        case true => range = Range(range.to,newTo.get)
        case false => log.error("RangeUpdate FAILED, NONE Value returned")
      }

    case _ => throw new Exception("Don't understand what is being sent to me!!")
  }
}

object RequestConnection {
  def props(nodeName: String, password: String, datetime: Long): actor.Props = Props(new RequestConnection(nodeName, password, datetime))
  case class currentTime()
  case class ping()
  case class WorkerReadyForWork(worker:ActorRef)

}









