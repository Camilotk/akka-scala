//#full-example
package com.example

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import MovementProcessor.Movement
import GameMapActor.GameMap
import CharacterAction.Action

object CharacterAction {
  final case class Action(character: String, place: String, doing: Boolean)

  def apply(): Behavior[Action] = Behaviors.receive {
    (context, message) =>
        message.place match {
          case "Sala de Aula" => {
            if(message.doing) {
              println(f"O personagem ${message.character} está ministrando aula!")
            } else {
              println(f"O personagem ${message.character} está esperando a aula começar!")
            }
          }
          case _ => println(f"O personagem ${message.character} está comtemplando a paisagem")
      }
      Behaviors.same
  }
}

object GameMapActor {
  final case class GameMap(character: String, location: (Int, Int), replyTo: ActorRef[Action])

  def apply(): Behavior[GameMap] = Behaviors.receive {
    val locations = Map(
      (31, 12) -> "Sala de Aula",
      (9, -44) -> "Sala dos Professores"
    )
    (context, message) =>
      if (locations.contains(message.location)) {
        val characterLocation = locations collect { case (message.location, place) => place }
        println(f"O personagem ${message.character} está em ${characterLocation.head}")
        message.replyTo ! Action(message.character, characterLocation.head, true)
      }

      Behaviors.same
  }
}

object MovementProcessor {
  final case class Movement(character: String, location: (Int, Int), axis: Char, distance: Int)

  def apply(): Behavior[Movement] = Behaviors.setup {
    context => 
      val gameMapRef = context.spawn(GameMapActor(), "mapa")
      val actionsRef = context.spawn(CharacterAction(), "acoes")

      Behaviors.receiveMessage {
        message =>
          var position: (Int, Int) = (0, 0)
          message.axis match {
            case 'x' => {
              position = (message.location._1 + message.distance, message.location._2)
              println(f"O personagem ${message.character} moveu ${message.distance} no eixo x e está em $position")
            }
            case 'y' => {
              position = (message.location._1, message.location._2 + message.distance)
              println(f"O personagem ${message.character} moveu ${message.distance} no eixo y e está em $position")
            }
            case _ => println("Erro")
          }
          // context.log.info(message.toString())
          // println(message.toString())

          gameMapRef ! GameMap(message.character, position, actionsRef)

          Behaviors.same
      }
  }
}

//#main-class
object AkkaQuickstart extends App {
  //#actor-system
  val movementProcessor: ActorSystem[MovementProcessor.Movement] = ActorSystem(MovementProcessor(), "movements") 
  //#actor-system

  //#main-send-messages
  movementProcessor ! Movement("thyago salva", (5, 18), 'x', 34)
  movementProcessor ! Movement("mauricio rosito", (9, -2), 'y', -42)
  movementProcessor ! Movement("lissandra", (8, 12), 'x', 23)
  movementProcessor ! Movement("sandro", (-8, 2), 'y', 23)
  //#main-send-messages
}
//#main-class
//#full-example
