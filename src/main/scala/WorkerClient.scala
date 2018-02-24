import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{complete, get, parameters, path}

import scala.concurrent.Future
import java.net._

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Failure, Success}
import akka.http.scaladsl.server.Directives._

import scala.language.postfixOps
/*
What the worker does:
  - Opens http port for server
  - Receives password to crack and the range to work on and then responds with found or not found.
  - Calls C program cracker. The cracker does the actual job of cracking and returns the result to ther worker.
  - Receives ping from server and responds to it.
 */

object WorkerClient extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  val localhost: InetAddress = InetAddress.getLocalHost
  val localIpAddress: String = localhost.getHostAddress
  val workerPort = 8081
  val serverHostname = "localhost"
  val serverPort = 8080
  val name = localIpAddress
  val serverAddress = s"http://${serverHostname}:${serverPort}/worker-client?nodeName=${name}"

  val route:Route = {
    get {
      path("worker-client") {
        parameters("password","from","to")  { (password,from,to) =>
          println("ack job")
          complete(callCracker(password,from,to))
        }
      } ~
        path("worker-client-ping") {
          println("Worker Pinged")
          complete(s"Worker: ${localIpAddress} Pinged Successfully")
        }
    }
  }
  def callCracker(password:String,from:String,to:String): String = {
    println(s"Received Cracker Job: Password: ${password} & Range: ${from} - ${to}")
    //Thread.sleep(10000)
    return s"cracker is done with this result: ${from}, ${to}"
  }

  Http().bindAndHandleAsync(Route.asyncHandler(route), localIpAddress, workerPort)
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

  val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = serverAddress ))
  responseFuture
    .onComplete {
      case Success(res) => println(res)
      case Failure(_) => sys.error("something wrong")
    }

}
