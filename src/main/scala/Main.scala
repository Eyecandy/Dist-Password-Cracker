package server

import scala.io.StdIn

class Main {
  println("worker start: w","distpatch server start: d","request client start: r")
  val rawInput = StdIn.readLine()
  rawInput match {
    case "d" => server.DispatchServer.start()
    case "w" =>  clients.WorkerClient.start()
    case "r" => clients.RequestClient.start()

  }

}
