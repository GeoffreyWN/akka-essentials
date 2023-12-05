package com.akkaEssentials.infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object TimersSchedulers extends App {
  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = { case message =>
      log.info(message.toString)
    }
  }

  val system      = ActorSystem("SchedulersTimersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])

//  system.log.info("Scheduling reminder for SimpleActor")

  import system.dispatcher
//  system.scheduler.scheduleOnce(2 second) {
//    simpleActor ! "reminder"
//  }
//
//  val routine = system.scheduler.scheduleWithFixedDelay(1 second, 2 seconds, simpleActor, "Heartbeat")
//
//
//  system.scheduler.scheduleOnce(5 seconds) {
//    routine.cancel()
//  }

  class SelfClosingActor extends Actor with ActorLogging {

    private var schedule: Cancellable = createTimeoutWindow()

    private def  createTimeoutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second) {
        self ! "timeout"
      }
    }

    override def receive: Receive = {
      case "timeout" =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>
        log.info(s"Received $message, we keep going my guy!")
        schedule.cancel()
        schedule = createTimeoutWindow()
    }
  }

//  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "selfClosingActor")
//  system.scheduler.scheduleOnce(250 millis) {
//    selfClosingActor ! "ping"
//  }
//
//  system.scheduler.scheduleOnce(2 seconds) {
//    system.log.info("Sending pong to the self closing actor!")
//    selfClosingActor ! "pong"
//  }

  /**
   * Timers
   */
  case object TimerKey
  case object Start
  case object Reminder
  case object Stop

  class TimerBasedHeartbeatActor extends  Actor with ActorLogging with Timers {

    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case  Start =>
        log.info("Bootstrapping")
        timers.startTimerWithFixedDelay(TimerKey, Reminder, 1 second)
      case Reminder =>
        log.info("I am alive!")
      case Stop =>
        log.warning("Stopping!")
        timers.cancel(TimerKey)
        context.stop(self)

    }
  }

  val timerHeartbeatActor = system.actorOf(Props[TimerBasedHeartbeatActor], "TimerBasedHeartbeatActor")
  system.scheduler.scheduleOnce(5 seconds) {
    timerHeartbeatActor ! Stop
  }


}
