package zionats

import zio._

object ExampleApp extends ZIOAppDefault {

  override def run: URIO[ZEnv, ExitCode] =
    val program = for {
      _ <- LiveNatsClientWrapper.layer.build.use { client =>
        val service = client.get
        for {
          _ <- service.publish("my-subject", "Hello from ZIO NATS!")
          _ <- service.subscribe("my-subject")(msg => 
                 ZIO.succeed(println(s"Received: $msg")))
          _ <- ZIO.sleep(2.seconds) // Give time to receive message
        } yield ()
      }
    } yield ()

    program.exitCode
}
LiveNatsClientWrapper