package com.akkaEssentials.testing

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

import akka.actor.{ Actor, ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import com.typesafe.config.ConfigFactory

import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class TimedAssertionsSpec
    extends TestKit(ActorSystem("TimedAssertionsSpec", ConfigFactory.load().getConfig("specialTimedAssertionsConfig")))
    with ImplicitSender
    with AnyWordSpecLike
    with BeforeAndAfterAll {

  override def beforeAll(): Unit = println("I will run before all tests")

  override def afterAll(): Unit = {
    println("I will run after all tests")
    TestKit.shutdownActorSystem(system)
  }

  import TimedAssertionsSpec._

  "A worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])
    "reply with the meaning of life in a timely manner" in {
      within(500 millis, 1 second) {
        workerActor ! "work"

        expectMsg(WorkResult(42))
      }
    }

    "reply with valid work at a resonable cadence" in {
      within(1 second) {
        workerActor ! "workSequence"

        val results: Seq[Int] = receiveWhile[Int](max = 2 seconds, idle = 500 millis, messages = 20) { case WorkResult(result) =>
          result
        }
        assert(results.sum > 5)
      }
    }

    "reply to a testprobe in a timely manner" in {
      within(1 second) {
        val probe = TestProbe()
        probe.send(workerActor, "work")
        probe.expectMsg(WorkResult(42)) // timeout of 10 seconds as per "specialTimedAssertionsConfig"
      }

    }
  }

}

object TimedAssertionsSpec {

  case class WorkResult(result: Int)

  class WorkerActor extends Actor {
    override def receive: Receive = {
      case "work"         =>
        // long computation
        Thread.sleep(500)
        sender() ! WorkResult(42)
      case "workSequence" =>
        val randomNum = new Random()

        for (_ <- 1 to 20) {
          Thread.sleep(randomNum.nextInt(50))
          sender() ! WorkResult(2)
        }
    }
  }
}
