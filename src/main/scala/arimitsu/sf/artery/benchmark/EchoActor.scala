package arimitsu.sf.artery.benchmark

import akka.actor.{ Actor, ActorLogging, Props }

class EchoActor extends Actor with ActorLogging {
  def receive = {
    case msg: String =>
      sender() ! msg
  }
}

object EchoActor {
  val name: String = "echo"
  def props: Props = Props[EchoActor]
}
