package server

import akka.actor.{Actor, Props}
import server.ShutDown.WaitForInput
import scala.io.StdIn

class ShutDown extends Actor{
  override def receive: Receive = {
    case WaitForInput() => {

      while (true) {
        val input = StdIn.readInt()
        input match {
          case 123 =>
            SuperVisor.singletonSuperVisorActor ! SuperVisor.Shutdown()
            println("shutdown in 10 seconds")
            Thread.sleep(10)
            println("shutdown")
            System.exit(0)
          case _ => println("Bad input")
        }


      }
    }
  }
}

object ShutDown {
  val props = Props(new ShutDown)
  case class WaitForInput()

}
