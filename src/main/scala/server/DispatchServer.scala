package server

import java.net.InetAddress

import akka.actor.{ActorRef, ActorSelection, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Failure, Success}
import akka.http.scaladsl.server.Directives._
import server.SuperVisor.WorkerRequestToJoin
/*
  What the DispatchServer does:
    - Opens a port for clients to connect to.
    - Receives request-client connections and forwards the information to  RequestConnectionManager.
    - Receives worker-client connections and forwards the information  to the SuperVisor Actor.
 */

object DispatchServer  {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val dispatcher = system.dispatcher
  val localhost: InetAddress = InetAddress.getLocalHost
  val localIpAddress: String = localhost.getHostAddress
  val superVisorActor: ActorRef = SuperVisor.singletonSuperVisorActor
  val requestConnectionManager: ActorRef =  RequestConnectionManager.singletonRequestConnectionManger
  def start() {
    val shutdown = system.actorOf(ShutDown.props)
    shutdown ! ShutDown.WaitForInput()
    val route: Route = {
      get {
        path("request-client") {
          parameters("nodeName", "password") { (nodeName, password) ⇒
            requestConnectionManager ! RequestConnectionManager.Register(nodeName, password)
            complete(s"Server registered you $nodeName and you password job: ${password}")
          } ~
            parameter("nodeName") { nodeName ⇒
              val nodeActor: ActorSelection = system.actorSelection(s"akka://default/user/RequestConnectionManager/${nodeName}")
              nodeActor ! RequestConnection.ping()
              complete(s"Ping (${nodeActor}) was registered by server.")
            }
        } ~
          path("worker-client") {
            parameters("nodeName") { (nodeName) =>
              superVisorActor ! WorkerRequestToJoin(nodeName)
              complete(s"worker registered")
            }
          }
      }
    }

    Http().bindAndHandleAsync(Route.asyncHandler(route), "localhost", 8080)
      .onComplete {
        case Success(_) ⇒
          println(s"Server started on ${localIpAddress} port 8080. Type ENTER to terminate.")
          StdIn.readLine()
          system.terminate()
        case Failure(e) ⇒
          println("Binding failed.")
          e.printStackTrace
          system.terminate()
      }
  }
}