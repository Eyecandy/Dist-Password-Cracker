package server

import akka.actor
import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.event.Logging
import server.WorkerConnectionActor.SendJobToWorkerClient


/*
  what RequesConnectionActor does:
    - Stores last time received ping from client
    - Updates last time pinged variable when pinged.
    - Is scheduled by SuperVisorActor to check last time pinged vs currentTime.
 */

class RequestConnectionActor(nodeName: String, password: String, pingTime: Long) extends Actor {
  val range = Range("AAAAAAAAA","AABAAAAA")
  var lastPingTime = pingTime;
  val log = Logging(context.system, this)
  import RequestConnectionActor._

  override def receive: Receive = {

    case currentTime() => {
      val nanoSecSinceLastPing = System.currentTimeMillis() - lastPingTime;
      if (nanoSecSinceLastPing > 15000) {
        log.error(s"Request client connection appears to be lost, Killing actor: ${self.path}")
        self ! PoisonPill
      }
    }
    case ping() => lastPingTime = System.currentTimeMillis()
    case WorkerReadyForWork(worker) => worker ! SendJobToWorkerClient(password,range.from,range.to)
    case _ => throw new Exception("Don't understand what is being sent to me!!")
  }
}

object RequestConnectionActor {
  def props(nodeName: String, password: String, datetime: Long): actor.Props = Props(new RequestConnectionActor(nodeName, password, datetime))
  case class currentTime()
  case class ping()
  case class WorkerReadyForWork(worker:ActorRef)

}









