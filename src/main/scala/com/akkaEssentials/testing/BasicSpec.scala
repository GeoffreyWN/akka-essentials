package com.akkaEssentials.testing

import akka.actor.{ Actor, ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  // set up
  override def beforeAll(): Unit = println("I will run before all tests")

  override def afterAll(): Unit = {
    println("I will run after all tests")
    TestKit.shutdownActorSystem(system)
  }

  // test suite
  import BasicSpec._
  "A SimpleActor" should {
    // test
    "send back the same message" in {
      val echoActor = system.actorOf(Props[SimpleActor])
      val message   = "hello test"

      echoActor ! message

      expectMsg(message)
    }
  }

  "A BlackholeActor" should {
    "send back some message" in {
      val blackholeActor = system.actorOf(Props[BlackholeActor])
      val message        = "hello test"

      blackholeActor ! message

      expectNoMessage(1 second)
    }
  }

  // message assertions
  "A LabTestActor" should {
    val labTestActor = system.actorOf(
      Props[LabTestActor]
    ) // created outside the test but inside the test suite to allow for reuse (for stateful actors create them inside every test)

    "turn a string into an uppercase string" in {
      labTestActor ! "Akka is awesome, right?"
      val reply = expectMsgType[String]

      assert(reply == "AKKA IS AWESOME, RIGHT?")
    }

    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("Hello champ", "Hi kiongos!")
    }

    "reply with favorite tech" in {
      labTestActor ! "favoriteTech"
      expectMsgAllOf("Scala", "Akka")
    }

    "reply with cool tech in a different way" in {
      labTestActor ! "favoriteTech"
      val messages = receiveN(2) // Seq[Any]

      // more assertions can be implemented
      assert(messages.length == 2)
    }

    "reply with cool tech in a fancy way" in {
      labTestActor ! "favoriteTech"

      expectMsgPF() {
        case "Scala" => // only care that the PF (partial function) is defined
        case "Akka"  =>
      }
    }

  }

}

object BasicSpec {
  class SimpleActor extends Actor {
    override def receive: Receive = { case message =>
      sender() ! message
    }
  }

  class BlackholeActor extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random()

    override def receive: Receive = {
      case "greeting"      =>
        if (random.nextBoolean()) sender() ! "Hello champ" else sender() ! "Hi kiongos!"
      case "favoriteTech"  =>
        sender() ! "Scala"
        sender() ! "Akka"
      case message: String => sender() ! message.toUpperCase()
    }

  }
}
