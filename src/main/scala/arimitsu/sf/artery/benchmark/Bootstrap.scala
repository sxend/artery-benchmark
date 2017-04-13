package arimitsu.sf.artery.benchmark

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.util.{Failure, Success}
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
    onRole("stressor") {
      stressing(CounterActor.startProxy(system))(system)
    }
  }
  private def onRole(role: String)(fn: => Unit) = if (hasRole(role)) fn

  private def hasRole(role: String): Boolean = roles.contains(role)
  private val STRESS_NUM = 100000
  private def stressing(ref: ActorRef)(system: ActorSystem): Unit = {
    import system.dispatcher
    val checker = system.actorOf(Props(classOf[ResponseChecker]))
    println("start stressing")
    (1 to STRESS_NUM) foreach { _ =>
      val msg = java.util.UUID.randomUUID().toString
      val start = System.currentTimeMillis()
      ref.ask(msg).mapTo[String].onComplete {
        case Success(count) =>
          checker ! (System.currentTimeMillis() - start)
        case Failure(t) => system.log.error(t, t.getMessage)
      }
    }
  }
  class ResponseChecker extends Actor with ActorLogging {
    private val count = new AtomicLong(0)
    private val sum = new AtomicLong(0)
    def receive = {
      case time: Long =>
        val _count = count.incrementAndGet()
        val _sum = sum.addAndGet(time)
        if (_count % 1000 == 0) {
          log.info(s"sum: ${_sum}, count: ${_count}, avg: ${_sum.toDouble / _count.toDouble}ms")
        }
    }
  }
}