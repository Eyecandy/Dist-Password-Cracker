package clients


import java.net._

import akka.actor.{Actor, ActorSystem, LightArrayRevolverScheduler}
import akka.http.javadsl.model.HttpEntity.Strict
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{complete, parameters, path}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import server.DispatchServer

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}
import sys.process._
import scala.language.postfixOps


/*
what Client does:
 - Sends the ip of the node it is running to the server.
 - Pings the server every 5 seconds .
 - opens a port for server to send a the decrypted password to. Stops pinging upon recv.


*/

object RequestClient  {
  def start(): Unit = {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    val localhost: InetAddress = InetAddress.getLocalHost
    val localIpAddress = localhost.getHostAddress
    println("type in server's address to connect to it.")
    val serverHostname = StdIn.readLine()
    println("Type in encrypted password: (compile encrypt & type in a password to get a valid hashed password):")
    val encryptedPsw = StdIn.readLine()
    val serverPort = "8080"
    val name = localIpAddress
    var doPing = true;

    val route: Route = {
      path("dec_psw") {
        parameters("decrypted_password") { (decrypted_psw) =>
          println(s"I RECEIVED: ${decrypted_psw}")
          doPing = false
          complete(s"REQUEST CLIENT RECEIVED PWS ${decrypted_psw}")
        }
      }
    }
    Http().bindAndHandleAsync(Route.asyncHandler(route), localIpAddress, 8082)
      .onComplete {
        case Success(_) ⇒
          println(s"RequestClient started on ${localIpAddress}, port 8080 . Type ENTER to terminate.")
          StdIn.readLine()
          system.terminate()
        case Failure(e) ⇒
          println("Binding failed.")
          e.printStackTrace
          system.terminate()
      }




    val nodeNameAndPsw = s"http://${serverHostname}:${serverPort}/request-client?nodeName=${name}&password=${encryptedPsw}"
    val pinging = s"http://${serverHostname}:${serverPort}/request-client?nodeName=${name}"
    var registrationComplete = false
    while (!registrationComplete) {
      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = nodeNameAndPsw))
      responseFuture
        .onComplete {
          case Success(res) => println("Successful registration"); registrationComplete = true;
          case Failure(_) => throw new Exception("Failed to connect to server")
        }
      Thread.sleep(5000)
    }

    while (doPing) {
      val responseFuture1: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = pinging))
      responseFuture1
        .onComplete {
          case Success(res) => println(res.entity)
          case Failure(_) => sys.error("something wrong")
        }
      Thread.sleep(5000)
    }
  }





}