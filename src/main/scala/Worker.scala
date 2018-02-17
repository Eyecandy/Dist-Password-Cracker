import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{complete, get, parameters, path}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}
import java.net._

object Worker extends App {
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
  val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = serverAddress ))
  responseFuture
    .onComplete {
      case Success(res) => println(res)
      case Failure(_) => sys.error("something wrong")
    }
  val route:Route = {
    get {
      path("worker-client") {
        parameters("password","from","to")  { (password,from,to) =>
          println(from,to)
          complete(callCracker(password,from,to))
        }
      }
    }
  }
  def callCracker(password:String,from:String,to:String): String = {
    println("callCracker called")
    Thread.sleep(1)
    return "cracker is done with this result: blah blah"
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
}
