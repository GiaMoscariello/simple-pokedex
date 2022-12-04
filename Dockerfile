FROM hseeberger/scala-sbt:eclipse-temurin-11.0.14.1_1.6.2_3.1.1 as builder

WORKDIR /build
COPY . /build

RUN sbt 'set test in assembly := {}' -batch assembly

FROM openjdk:11-jre-slim as final

COPY --from=builder /build/target/scala-2.13/pokemon-api-assembly-0.1.0.jar /srv/boot.jar

ENV POKEMON_BASE_URL="https://pokeapi.co/api/v2/"
ENV TRANSLATION_BASE_URL="https://api.funtranslations.com/translate"
ENV CLIENT_ENDPOINT_POKEMON="pokemon-species"
ENV SERVER_ENDPOINT_POKEMON="pokemon"
ENV SERVER_ENDPOINT_POKEMON_TRANSLATED="translated"
ENV ENDPOINT_YODA_TRANSLATION="yoda.json"
ENV ENDPOINT_SHAKESPEARE_TRANSLATION="shakespeare.json"


ENTRYPOINT java -cp /srv/boot.jar com.gia.moscariello.simple.pokedex.ServerApp

