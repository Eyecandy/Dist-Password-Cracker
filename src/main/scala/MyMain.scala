
import scala.io.StdIn

object MyMain extends  App{

    println("worker start: w","distpatch server start: d","request client start: r")
    val rawInput = StdIn.readLine()
    rawInput match {
      case "d" => server.DispatchServer.start()
      case "w" =>  clients.WorkerClient.start()
      case "r" => clients.RequestClient.start()

    }



}
