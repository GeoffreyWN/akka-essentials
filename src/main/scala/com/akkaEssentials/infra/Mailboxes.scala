package com.akkaEssentials.infra

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Mailboxes extends App {
  val system = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo") )

  class SimpleActor extends Actor with ActorLogging {

    override def receive: Receive = { case message =>
      log.info(message.toString)
    }
  }

  /** #1 custom priority mailbox
    * P0 -> most important
    * P1 -> most important
    * P2 -> most important
    * P3 -> most important
    */
// step 1 - mailbox definition
  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config)
      extends UnboundedPriorityMailbox(
        PriorityGenerator {
          case message: String if message.startsWith("[P0]") => 0
          case message: String if message.startsWith("[P1]") => 1
          case message: String if message.startsWith("[P2]") => 2
          case message: String if message.startsWith("[P3]") => 3
          case _                                             => 4
        }
      )

  // step 2- make it known in the config (attach mailbox to dispatcher in the config file)
  // step 3- attach the dispatcher to an actor

  val supportTicketLogger = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
//  supportTicketLogger ! "[P3] nice to have"
//  supportTicketLogger ! "[P0] high stakes task"
//  supportTicketLogger ! "[P1] do this when later in the day."

  /** #1 control aware mailbox
   * using UnboundedControlAwareMailbox
   */

  // step 1 - mark important messages as control messages
  case object ManagementTicket extends ControlMessage

  // step 2 - configure who gets the mailbox (make the actor attach to the mailbox )
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))

//  controlAwareActor ! "[P0] high stakes task"
//  controlAwareActor ! "[P1] do this when later in the day."
//  controlAwareActor ! "[P3] high stakes task"
//  controlAwareActor ! ManagementTicket

  // using deployment config
  val controlAwareActorV2 = system.actorOf(Props[SimpleActor],"controlAwareActorV2" )

  controlAwareActorV2 ! "[P0] high stakes task"
  controlAwareActorV2 ! "[P3] high stakes task"
  controlAwareActorV2 ! "[P1] do this when later in the day."
  controlAwareActorV2 ! ManagementTicket



}
