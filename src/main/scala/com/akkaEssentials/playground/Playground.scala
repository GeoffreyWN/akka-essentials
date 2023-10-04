package com.akkaEssentials.playground

import akka.actor.ActorSystem

object Playground extends App{
  implicit lazy val actorSystem: ActorSystem = ActorSystem("akkaEssentials-Sys")

  println(actorSystem.name)

}
