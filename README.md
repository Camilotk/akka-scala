# Scala Actors
Akka é um Framework Reativo que adere ao manifesto reativo.

### Actors
Atores são pequenos spaces que se comunicam através de mensagens com outros atores de forma assíncrona. 

**Ex 1: Exemplo simples de ator**
```scala
// começamos definindo uma nova class
object MovementProcessor {
    // que contêm uma case class com os attr do movimento
    // que representa tbm os atributos da mensagem que é enviada a outros atores
    final case class Movement(character: String, location: (Int, Int))

    // Então definimos o método apply que vai executar as ações desse actor
    // Ele é do tipo Behavior[T] e recebe um dos métodos da class Behaviors sobrecarregado
    def apply(): Behavior[Movement] = Behaviors.receiveMessage {
        message =>
            // Executamos uma ação com a mensagem
            println(f"O personagem ${message.character} moveu-se de ${message.location}!")

            // E retornamos .same de Bahaviors para
            // completar um retorno correto dizendo
            // para que seja retornado uma instância
            // desse Behavior
            Behaviors.same
    }

}
```
E então podemos chamar isso na nossa Main, passando uma mensagem para o nosso Ator.
```scala
object AkkaQuickstart extends App {
  // actor-system
  // criamos através do factory method
  // de mesmo nome uma instância de MovementProcessor com
  // o atributo name como "movements" -> esse atributo é 
  // usado para identificar a instancia, e guardá-la na
  // val movementProcessor
  val movementProcessor: ActorSystem[Movement] = ActorSystem(MovementProcessor(), "movements")

  // main-send-messages
  // Aqui mandamos as mensagens usando bang operator !
  movementProcessor ! Movement("João", (5, 18))
  movementProcessor ! Movement("Maria", (9, -2))

}
```
```
Obs: Em Scala mensagens são enviadas por atores usando um dos seguintes métodos:

    ! significa “fire-and-forget”, e.g. envia a mensagem assíncrona e retorna imediatamente. 
    Também conhecido como tell.

    ? envia a mensagem de forma assíncrona e recebe um Future para uma computação futura. 
    Também conhecido como ask.
```

**Ex 2: Opções**

É importante conhecer algumas diferenças como por exemplo entre Behavior.recieveMesage e Behavior.recieve, a primeira recebe apenas a mensagem, enquanto a segunda recebe o objeto context que contêm informações adicionais e a mensagem.

```scala
object GameMapActor {
    final case class GameMap(character: String, location: (Int, Int))

    // aqui temos .recieve ao invés de .recieveMessage
    def apply(): Behavior[GameMap] = Behaviors.receive {
        // que nos traz o objeto context 
        // juntamente com a mensagem
        (context, message) =>
            // o context além de mais informações contem
            // métodos como log que podem ser usados para
            // logar no console mensagens
            context.log.info(message)

        Behaviors.same
    }
}
```

Quando queremos que o Behavior tenha mais informações e ações do receber ou enviar uma mensagem usamos o método .setup de Bahaviors que nos permite declarar os listeners de enviar e receber mensagem dentro.

```scala
object MovementProcessor {
  final case class Movement(character: String, location: (Int, Int))

    // aqui usamos o método setup para adicionar 
    // informações e comportamentos no ator
    def apply(): Behavior[Movement] = Behaviors.setup {
        // setup recebe context
        context =>
            // add greeting
            val greeting: String = "Olá"

            // aqui declaramos recieveMessage normalmente
            Behaviors.receiveMessage {
                message =>
                    println(f"$greeting ${message.character}")

                    Behaviors.same
            }
    }
}
```

**Ex 3: Chamadas**

Um ator pode enviar mensagem para outro usando um dos operadores de mensagem.

```scala
object GameMapActor {
  final case class GameMap(character: String, location: (Int, Int))

  def apply(): Behavior[GameMap] = Behaviors.receive {

    val locations = Map(
        (31, 12) -> "Sala de Aula",
        (9, -44) -> "Sala dos Professores"
    )

    (context, message) =>
        // caso o lugar passado na mensagem exista
        if (locations.contains(message.location)) {
            // capture o nome do local do objeto locations
            val characterLocation = locations collect { case (message.location, place) => place }
            // imprima que o personagem está lá
            println(f"O personagem ${message.character} está em ${characterLocation.head}")
        }

    Behaviors.same
  }
}

object MovementProcessor {
  final case class Movement(character: String, location: (Int, Int), axis: Char, distance: Int)

  def apply(): Behavior[Movement] = Behaviors.setup {
    context =>
        // Declara um GameMapActor
        val gameMapRef = context.spawn(GameMapActor(), "mapa")

        // Recebe uma mensagem
        Behaviors.receiveMessage {
            message =>
                var position: (Int, Int) = (0, 0)
                // pattern match a posição do personagem
                message.axis match {
                    case 'x' => {
                        // aplica o movimento
                        position = (message.location._1 + message.distance, message.location._2)
                        // mostra no console
                        println(f"O personagem ${message.character} moveu ${message.distance} no eixo x e está em $position")
                    }
                    case 'y' => {
                        position = (message.location._1, message.location._2 + message.distance)
                        println(f"O personagem ${message.character} moveu ${message.distance} no eixo y e está em $position")
                    }
                    case _ => println("Erro")
                }

                // Envia a mensagem para o outro ator
                gameMapRef ! GameMap(message.character, position)

                Behaviors.same
            }
        }
}
```

**Ex 4: replyTo**

É aconselhavel passar o ator que tem que ser chamado pelo parametro replyTo do ator.

```scala
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
    // adicionamos o param replyTo
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

        // Envia a mensagem para o ator de replyTo
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
      // declaramos um Actor de actions como Ref
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

            // passamos o param reply com actionsRef
          gameMapRef ! GameMap(message.character, position, actionsRef)

          Behaviors.same
      }
  }
}
```