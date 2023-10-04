package com.akkaEssentials.Actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

// sending  message to an actor with constructor args (include a companion object where the props will be called from)

object SayHello {
  // method to return the props object
  def props(name: String): Props = Props(new SayHello(name))

}

class SayHello(name: String) extends Actor with ActorLogging {
  log.info(s"About to say Hello to $name")

  def receive: Receive = {
    case "Hello" => log.info(s"Hello, $name")
    case _ => log.info(s"Sorry, I could not greet ${name}!")
  }
}



class WordCountActor extends Actor with ActorLogging {
  var totalWords = 0

  log.info("About to receive a message and count words")

  def receive: Receive = {
    case message: String =>
      log.info(s"I have received a message ${message}")
      totalWords += message.split(" ").length
      log.info(s"TotalWords=> $totalWords")
    case _ => log.info("I could not handle this message")
  }
}

object ActorsIntro extends App{
  implicit lazy val actorSystem: ActorSystem = ActorSystem("akkaIntro-Sys")
  import actorSystem.dispatcher

  val wordCounter1 = actorSystem.actorOf(Props[WordCountActor], "wordCounter1")
  val wordCounter2 = actorSystem.actorOf(Props[WordCountActor], "wordCounter2")

  wordCounter1 ! "My name is Akka and I am pretty awesome. I send messages asynchronously. Here I am using the tell and forget pattern!"
  wordCounter2 ! "An actor has its own state and behaviour. Communication between actors is strictly via messages"


  val sayHelloActor = actorSystem.actorOf(SayHello.props("Mary"), "sayHelloActor")
  sayHelloActor ! "Hi!"
  sayHelloActor ! "Hello"

}