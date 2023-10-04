package com.akkaEssentials.Actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.akkaEssentials.Actors.ActorCapabilities.{FunMessage, PrepareFood, RemindMySelf}

class FunFactActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case "Prepare some tea. For 10 people!" => log.info("I will prepare some tea for 10 people")
    case PrepareFood(message, ref)  =>
    log.info(s"$message")
      ref forward message + ". For 10 people!"
    case message: String   => log.info(s"$message")
    case number: Int       => log.info(s"Here is a type Int: $number")
    case FunMessage(msg)   => log.info(s"$msg")
    case RemindMySelf(msg) => self ! msg // an actor sending a message to itself
  }

}

object ActorCapabilities extends App {
  implicit lazy val system =  ActorSystem("actorCapabilities-Sys")
  val simpleActor = system.actorOf(Props[FunFactActor], "funfactActor")

  // messages must be Immutable
  // messages must be Serializable: the JVM can transform it in to a bytestream and send it to another JVM for instance

  // messages can be any type
  simpleActor ! "I am good at swimming!" 

  simpleActor ! 55

  case class FunMessage(msg: String)
  simpleActor ! FunMessage("I love travelling!")

  case class RemindMySelf(msg: String)

  simpleActor ! RemindMySelf("Go to the gym!")

  // two actors communicating

  val chefActor =system.actorOf(Props[FunFactActor], "chefActor")
  val waiterActor =system.actorOf(Props[FunFactActor], "waiterActor")

  case class PrepareFood(content: String, ref: ActorRef)

  waiterActor ! PrepareFood("Prepare some tea", chefActor)


}
