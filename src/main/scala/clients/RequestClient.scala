package clients


import java.net._

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}

/*
what Client does:
 - Sends the ip of the node it is running to the server.
 - Pings the server every 5 seconds .
*/

object RequestClient {
  def start(): Unit = {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    val localhost: InetAddress = InetAddress.getLocalHost
    val localIpAddress = localhost.getHostAddress
    val serverHostname = StdIn.readLine();
    val serverPort = "8080"
    val name = localIpAddress
    val psw = "password123"
    val nodeNameAndPsw = s"http://${serverHostname}:${serverPort}/request-client?nodeName=${name}&password=${psw}"
    val pinging = s"http://${serverHostname}:${serverPort}/request-client?nodeName=${name}"
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = nodeNameAndPsw ))

    responseFuture
      .onComplete {
        case Success(res) => println(res)
        case Failure(_) => sys.error("something wrong")
      }
    while (true) {
      val responseFuture1: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = pinging))
      responseFuture1
        .onComplete {
          case Success(res) => println(res)
          case Failure(_) => sys.error("something wrong")
        }
      Thread.sleep(5000)
    }
  }
}