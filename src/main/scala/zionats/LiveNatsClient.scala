package zionats

import io.nats.client.{Connection, ConnectionListener, Dispatcher, Nats}
import zio._

object LiveNatsClientWrapper {
  trait Service {
    def publish(subject: String, message: String): Task[Unit]
    def subscribe(subject: String)(handler: String => Task[Unit]): Task[Unit]
  }

  val layer: ZLayer[Any, Throwable, LiveNatsClientWrapper.Service] = ZLayer.scoped {
    for {
      conn <- ZIO.acquireRelease(
        ZIO.attempt(Nats.connect())
      )(c => ZIO.attempt(c.close()).orDie)
    } yield new Service {
      override def publish(subject: String, message: String): Task[Unit] =
        ZIO.attempt(conn.publish(subject, message.getBytes("UTF-8")))

      override def subscribe(subject: String)(handler: String => Task[Unit]): Task[Unit] =
        ZIO.attempt {
          val dispatcher: Dispatcher = conn.createDispatcher(msg => {
            val s = new String(msg.getData, "UTF-8")
            Unsafe.unsafe { implicit u =>
              Runtime.default.unsafe.run(handler(s)).getOrThrowFiberFailure()
            }
          })
          dispatcher.subscribe(subject)
        }
    }
  }
}
