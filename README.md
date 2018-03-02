# Dist-Password-Cracker
A distributed system which attempts to crack DES-encrypted password. (Given as an assignment in big data technologies)

# Overview
The goal of the project is to create a scalable, efficient and robust distributed system to crack DES-encrypted password jobs. The Project has 3 major components;request clients,dispatch server and worker clients. The request clients sends password jobs to the dispatch server, then dispatcher server dispatches smaller parts of the jobs to the workers. The distributed system was created in scala with the akka framework and the cracker in C. I the above (scala with akka) for scalabilty and C for efficiency. 

# Flow
- worker joins: it will notify its presence and get a sub job of a request client if it exists. And the server will ping it.
- if a request client joins: it will notify that there is an encrypted password job to pick up, all idle workers will pick up work. The request client pings server.
- if a worker dies: it's sub job will be put in a failedWork Queue and picked up by the next idle worker.
- if a request client dies: it's password job will be 	removed. And next request client gets it's turn to get password cracked.
- server failure is not handled.
- if server is shutdown on purpose, all workers will get a shutdown message and suicide.

# How to run:
  - start: MyMain
  - type in 'd' for dispatcher to start
  - type in 'r' for request client to start
  - type in 'w' for worker to start
  - type in '123' to shutdown server.
  - sbt assembly to create a jar.
  
  

#Compatibilty
 - the crypt function used in sshpc.c works on linux & mac OS.

Shout out to Pithikos for the thread pool implementation
    - https://github.com/Pithikos/C-Thread-Pool
 


 
















