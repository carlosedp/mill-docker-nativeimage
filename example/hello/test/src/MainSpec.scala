package com.domain.Main

import zio.*
import zio.http.*
import zio.test.*

object MainSpec extends ZIOSpecDefault:

    def spec =
        suite("Main application")(
            test("should greet world"):
                for
                    response <- HomeApp.runZIO(Request.get(URL(Root)))
                    body     <- response.body.asString
                yield assertTrue(
                    response.status == Status.Ok,
                    body == "Hello World",
                )
        )
