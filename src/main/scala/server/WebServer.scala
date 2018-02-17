package server

import akka.actor.{ActorRef, ActorSelection, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{DateTime, _}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import server.SupervisorWorkerActor.workerConnection


object WebServer extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val dispatcher = system.dispatcher
  val superVisorWorkerActor: ActorRef = system.actorOf(SupervisorWorkerActor.props)
  val route: Route = {
    get {
      path("request-client") {
        parameters("nodeName", "password") { (nodeName, password) ⇒
          val requestClientActor: ActorRef = system.actorOf(RequestActor.props(nodeName, password, System.currentTimeMillis()), nodeName)
          superVisorWorkerActor ! SupervisorWorkerActor.ClientRequest(nodeName,password)
          system.scheduler.schedule(5 seconds, 5 seconds, requestClientActor, RequestActor.currentTime())
          complete(s"Server registered you $nodeName and you password job: ${password}")
        } ~
          parameter("nodeName") { nodeName ⇒
            val nodeActor: ActorSelection = system.actorSelection(s"akka://default/user/${nodeName}")
            nodeActor ! RequestActor.ping()
            complete(s"Ping (${nodeActor}) was registered by server.")
          }
      } ~
      path("worker-client") {
        parameters("nodeName")  { (nodeName) => superVisorWorkerActor ! workerConnection(nodeName)
          complete(s"worker registered")
        }
      }
    }
  }

  Http().bindAndHandleAsync(Route.asyncHandler(route), "localhost", 8080)
    .onComplete {
      case Success(_) ⇒
        println("Server started on port 8080. Type ENTER to terminate.")
        StdIn.readLine()
        system.terminate()
      case Failure(e) ⇒
        println("Binding failed.")
        e.printStackTrace
        system.terminate()
    }
}