package arimitsu.sf.artery.benchmark

import java.util.UUID

import akka.actor.ActorSystem
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.cluster.Cluster
import akka.cluster.client.{ClusterClient, ClusterClientReceptionist, ClusterClientSettings}
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
import org.apache.commons.text.{CharacterPredicates, RandomStringGenerator}

import scala.concurrent.Future
import scala.concurrent.duration._

object Bootstrap {
  private val config = ConfigFactory.load
  private implicit val timeout = Timeout(3.seconds)

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("benchmark-system")
    onRole("cluster") {
      val cluster = Cluster(system)
      val echoRef = cluster.system.actorOf(EchoActor.props, EchoActor.name)
      ClusterClientReceptionist(cluster.system).registerService(echoRef)
    }
    onRole("http") {
      implicit val materializer = ActorMaterializer()
      import system.dispatcher
      val clientRef = system.actorOf(ClusterClient.props(ClusterClientSettings(system)), "client")
      val size = config.getInt("benchmark.http.message-size")
      def genString = UUID.randomUUID().toString
      def askToEchoServer: Future[String] = {
        clientRef.ask(ClusterClient.Send("/user/echo", genString, localAffinity = false)).mapTo[String].map(x => s"success. $x")
      }
      val route = (get & path("echo"))(complete(askToEchoServer))
      Http().bindAndHandleAsync(
        Route.asyncHandler(route),
        config.getString("benchmark.http.hostname"),
        config.getInt("benchmark.http.port"),
        parallelism = config.getInt("benchmark.http.parallelism"))
    }
  }
  private def onRole(role: String)(fn: => Unit) = if (hasRole(role)) fn
  private def hasRole(role: String): Boolean = role == config.getString("benchmark.role")
}