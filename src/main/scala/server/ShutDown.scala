package server

import akka.actor.{Actor, Props}
import server.ShutDown.WaitForInput
import scala.io.StdIn

class ShutDown extends Actor{
  override def receive: Receive = {
    case WaitForInput() => {
      val shutdownNumber = 123
      while (true) {
        val input = StdIn.readInt()
        if (input == shutdownNumber){
          SuperVisor.singletonSuperVisorActor ! SuperVisor.Shutdown()
          println("shutdown in 10 seconds")
          Thread.sleep(10)
          println("shutdown")
          System.exit(0)
        }

      }
    }
  }
}

object ShutDown {
  val props = Props(new ShutDown)
  case class WaitForInput()
}
