# Cards Game

This application creates a websocket to communicate with
a client in order to play a single card game.

##### Is written in a pure functional way using [cats](https://typelevel.org/cats-effect/), [fs2](https://fs2.io/#/), [circe](https://circe.github.io/circe/) and [http4s](https://github.com/http4s/http4s/blob/main/examples/blaze/src/main/scala/com/example/http4s/blaze/BlazeWebSocketExample.scala)

### How to use?

- Run ```sbt run```, it'll expose create a server bound to all available network cards.
- Connect via websocket (you can use Postman) pointing to the following url ```ws://127.0.0.1:8080/ws/:arbitraryId```, it'll automatically create a user and add it to the queue to start a game.
- Once connected and if two users are available then you'll receive a message like such: ```General({{roomId}})```. This is the id of the room that will be used to play.
- There are two types of movements ```playCard``` and ```fold```, you can decide how to play by sending the following payload via websocket:

```json
{"roomId": "{{roomId}}", "playerId": "{{arbitraryId}}", "action": "fold"}
```

**You should be able to see the output in the console of your websocket client**
