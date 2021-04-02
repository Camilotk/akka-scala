//#full-example
package com.example

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import MovementProcessor.Movement
import GameMapActor.GameMap
import org.scalatest.wordspec.AnyWordSpecLike

//#definition
class AkkaQuickstartSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
//#definition

  "A Movement" must {
    //#test
    "move the character to a place" in {
      val replyProbe = createTestProbe[GameMap]()
      val underTest = spawn(MovementProcessor())
      underTest ! Movement("eduardo schenato", (9, -40), 'y', -4)
      val actionsRef = spawn(CharacterAction(), "acoes")
      replyProbe.expectMessage(GameMap("eduardo schenato", (9, -44), actionsRef))
    }
    //#test
  }

}
//#full-example
