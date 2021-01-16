FROM openjdk:8

RUN apt-get update && \
    apt-get install -y --no-install-recommends git openssh-client ca-certificates
    
ADD build/libs/miraql.jar /miraql.jar

RUN mkdir /root/.ssh/
RUN ls -Ralp /root/
COPY files/id_rsa /root/.ssh/
COPY files/id_rsa.pub /root/.ssh/
COPY files/known_hosts /root/.ssh/
RUN ls -Ralp /root/.ssh/
RUN chmod 600 /root/.ssh/id_rsa
RUN chmod 600 /root/.ssh/id_rsa.pub
RUN chmod 644 /root/.ssh/known_hosts
RUN chmod 755 /root/.ssh/

RUN git clone -b kb --single-branch git@github.com:nvbach91/yugioh-ontology.git /yugioh-ontology

EXPOSE 8080

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/miraql.jar"]
