# Cards Game

<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge-tiny.png" alt="Cats friendly" /></a>


This application spins up a server that allows a client to play a single card game.

##### Is written in a pure functional way using [cats](https://typelevel.org/cats-effect/), [fs2](https://fs2.io/#/), [circe](https://circe.github.io/circe/) and [http4s](https://github.com/http4s/http4s/blob/main/examples/blaze/src/main/scala/com/example/http4s/blaze/BlazeWebSocketExample.scala)

### How to use?

- Run ```sbt run```, it'll create a server bound to all available network cards.
- Connect via websocket (you can use Postman) pointing to the following url ```ws://127.0.0.1:8080/ws/:arbitraryId```, it'll automatically create a user and add it to the queue to start a game.
- Once connected you'll receive a message like such: ```{{playerId: UUID, balance: 1000}}``` and if two users are available then you'll receive yet another notification that contains the roomId. This is the id of the room that will be used to play.
- There are two types of movements ```playCard``` and ```fold```, you can decide how to play by sending the following payload via websocket:

```json
{"roomId": "{{roomId}}", "playerId": "{{arbitraryId}}", "action": "fold"}
```

#### Optional:

- To test it across multiple devices install [ngrock](https://dashboard.ngrok.com) and type the command ```ngrok tcp 8080```, this will create an external link to your application that should look like this ```tcp://2.tcp.ngrok.io:14744```.
- Replace the ```tcp``` portion for ```ws``` in your favourite client, and you're ready to go.
- Finally, there's a companion app to make life easier to test this application. Please refer to [iOS Client](https://github.com/samgj18/cards-game-client).


### Key design decisions
To ensure consistency and concurrency this application heavily relies on two data types:
```scala 
Queue[F, Type] and Ref[F, Type]
```

### Limitations

- Currently, this implementation doesn't support Double Card Game. 

**You should be able to see the output in the console of your websocket client**
