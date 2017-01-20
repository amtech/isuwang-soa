FROM docker.oa.tuikor.com:5000/basic/openjdk:8-jre-alpine
MAINTAINER 439674061@qq.com 

# Setting Envirnoment
ENV CONTAINER_HOME /dapeng-container
ENV PATH $CONTAINER_HOME:$PATH

RUN mkdir -p "$CONTAINER_HOME"

COPY target/dapeng-container /dapeng-container

WORKDIR "$CONTAINER_HOME/bin"

RUN chmod +x *.sh
