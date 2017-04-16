package arimitsu.sf.artery.benchmark

import java.util.UUID

import akka.actor.ActorRef
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.util.{ Failure, Success }
import scala.concurrent.duration._

object Bootstrap {
  private val config = ConfigFactory.load
  private val roles = config.getStringList("akka.cluster.roles").toList
  private implicit val timeout = Timeout(120.seconds)

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("benchmark-system")
    onRole("seed") {
      CounterActor.startProxy(system)
    }
    onRole("node") {
      CounterActor.startSharding(system)
    }
    onRole("server") {
      startServer(CounterActor.startProxy(system))
    }
  }
  private def onRole(role: String)(fn: => Unit) = if (hasRole(role)) fn

  private def hasRole(role: String): Boolean = roles.contains(role)
  private def startServer(ref: ActorRef)(implicit system: ActorSystem): Unit = {
    import system.dispatcher
    implicit val materializer = ActorMaterializer()
    val route =
      path("") {
        get {
          val uuid = UUID.randomUUID().toString
          onComplete(ref.ask(uuid)) {
            case Success((path, count, _)) => complete(s"request: $uuid, path: $path, count: $count")
            case Failure(t)                => failWith(t)
            case x                         => failWith(new RuntimeException(s"unhandled response: $x"))
          }
        }
      }
    Http().bindAndHandle(route, "localhost", 8080)
  }
}