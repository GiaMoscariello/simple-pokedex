

FROM fabric8/java-alpine-openjdk11-jre

COPY ./rest/target/scala-2.13/rest-assembly-0.1.0.jar /srv/rest.jar

CMD java -cp /srv/materializer.jar