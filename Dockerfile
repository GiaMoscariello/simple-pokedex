FROM hseeberger/scala-sbt:eclipse-temurin-11.0.14.1_1.6.2_3.1.1

WORKDIR /build
COPY . /pokemon-api

COPY build.sbt .

ADD . .

