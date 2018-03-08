#Download base image ubuntu 16.04
FROM ubuntu:16.04

# Update Software repository
RUN apt-get update

WORKDIR /proj
COPY encrypt.c encrypt.c
COPY Makefile .
COPY sshpc.c .
COPY thpool.c .
COPY thpool.h .

RUN apt update
RUN apt -y install gcc
RUN apt -y install build-essential
RUN apt -y install libssl-dev


RUN make














