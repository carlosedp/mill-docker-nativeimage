package com.domain.Main

import zio.*
import zio.http.*

object MainApp extends ZIOAppDefault:
  // Define ZIO-http server
  val server: ZIO[Any, Throwable, Nothing] = Server
    .serve(HomeApp)
    .provide(
      Server.default
    )

  // Run main application
  def run = Console.printLine(s"Server started on http://localhost:8080") *> server

// Define ZIO-http route
val HomeApp: Http[Any, Nothing, Request, Response] =
  Http.collectZIO[Request]:
    case Method.GET -> Root =>
      ZIO.succeed(Response.text(s"Hello World"))
