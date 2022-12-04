# Simple Pokedex

Welcome to Simple Pokedex!

This service can provide some basic information about Pokemon, and if you want with some fun translation.

## Endpoint examples

##### Request

`/pokemon?name=<pokemon-name>` 

##### Response

```json 
{
    "name": "pikachu",
    "description": "When several of these POKéMON gather, their electricity could build and cause lightning storms.",
    "habitat": "forest",
    "is_legendary": false
}
```

##### Request

`/translated?name=<pokemon-name>`

##### Response

```json
{
    "name": "pikachu",
    "description": "At which hour several of these pokémon gather,  their electricity couldst buildeth and cause lightning storms.",
    "habitat": "forest",
    "is_legendary": false
}
```

### Design

This service is a simple http server which call a couple of idempotent APIs. Hence, we can easily cache the Http request and response.
In this first implementation I used in memory cache (Caffeine: https://github.com/ben-manes/caffeine), but for a production version 
a persistence solution with any high-performance Key-value DB may be used.

#### Authentication

No auth was implemented here, but if we needed for production, we can implement a user authentication with 
*Bearer token* or maybe if there is any API gateway, maybe this can do that job for us. 

#### Tech stack used

To implement this service, written in Scala, these frameworks and libs were used, for some reference:

* Http4s (https://github.com/http4s/http4s): Used to create an HTTP server and his routes
* Sttp (https://github.com/softwaremill/sttp): To create simple and fast http request
* Circe (https://github.com/circe/circe): Auto and semi-auto parsing json lib. Works perfectly with Http4s
* Wiremock(https://github.com/wiremock/wiremock): Needed to mock request and response in tests


### How to run

If you don't have a `JDK 11` and `SBT` installed you can use a docker image:

```shell
docker build -t simple_pokedex . 
```
and then 

```shell
docker run -p 9091:9090  simple_pokedex \
--env SERVER_PORT=9090 \
--env SERVER_HOST="0.0.0.0"
```

Otherwise, you can run locally with:
```shell
  set -a; source envs/local; set +a
```

```shell
  sbt "runMain  com.gia.moscariello.simple.pokedex.ServerApp"
```

If you want to install a `JDK` and `SBT` follow this:
https://www.scala-sbt.org/1.x/docs/Setup.html

### Tests

To run test:

```shell 
docker-compose build && docker-compose up -d && docker logs pokemon-api_pokemon_api_test_1 --follow
```






















