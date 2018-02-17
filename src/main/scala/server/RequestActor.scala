package server

import akka.actor
import akka.actor.{Actor, PoisonPill, Props}
import akka.event.Logging

class RequestActor(nodeName: String, password: String, pingTime: Long) extends Actor {
  var lastPingTime = pingTime;
  val log = Logging(context.system, this)
  import RequestActor._

  override def receive: Receive = {
    case ping() => {
      lastPingTime = System.currentTimeMillis()
    }
    case currentTime() => {
      val nanoSecSinceLastPing = System.currentTimeMillis() - lastPingTime;
      if (nanoSecSinceLastPing > 15000) {
        log.error(s"Request client connection appears to be lost, Killing actor: ${self.path}")
        self ! PoisonPill
      }
    }
    case _ => throw new Exception("Don't understand what is being sent to me!!")
  }
}

object RequestActor {
  def props(nodeName: String, password: String, datetime: Long): actor.Props = Props(new RequestActor(nodeName, password, datetime))
  case class currentTime()
  case class ping()
}









