
import scala.io.StdIn

object MyMain extends  App{
    def entryPoint() {
    println("worker start: w","distpatch server start: d","request client start: r")
    val rawInput = StdIn.readLine()
    rawInput match {
      case "d" => server.DispatchServer.start()
      case "w" => clients.WorkerClient.start()
      case "r" => clients.RequestClient.start()
      case _ =>
        println("Bad command")
        entryPoint()
      }
    }
  entryPoint()
}
