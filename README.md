# pub_sub_zookeeper
This is a pub sub model enhanced by zookeeper library.

Pub's duty is to send messages to eventService(MiddleWare).

EventService(MiddleWare) could store the message and zookeeper is implemented to coordinate the messages between different eventServices
Then, the message would be send to subscriber. Because messages in each eventService have been coordinated, so the subscriber can
connected to each of them, it would be same.

Subscriber would subscribe a specific topic and receive messages belong to that topic. Each subscriber can connect to any one of
eventServices.

You have to binding your jdk to zmq and zookeeper library before you run this sample.

second commit:

Simplify the code and fix some bugs. This time the eventService only have 2 threads and the local buffer has been removed. I let eventServices simply send messages from zookeeper node data, which has better performance

In this pattern, you should open several pub, sub, and eventservice programs in the network enviornment you want. In pub program, you should input the port number and topic name for each publisher; for each eventservice, you should input two port number, one for receive , one for receive, one for send. for each subscriber, you should input the port number and the topic you want to subscribe. Notice that each eventService could connect to any numbers of sub and pub you want, but each pub and sub can only connect to one eventservice. Then, the sub would print messages to the console so you can see what did it received.

In this project, because I implemented zookeeper pattern, so the subscriber can connect to any eventservice you want because they share the same message pool and send them at the same time. Messages would be stored in the pool.

After created numbers of mininet enviornment, you can type javac pub.java, javac sub.java and javac eventService.java to run the subscriber, publisher and eventservice. Then type java pub, java sub and java eventService to run them. You could use the topology network we used in assignment1 and 2, or just run them on command line. If you ran multiple eventservice and multiple pub, you would notice that zookeeper is working.


eventService
TO run the code, you should put all the .jar pack in the zkLib dir, and do:
javac -cp /home/wxk007/Documents/zkLib/zookeeper-3.4.6.jar:/home/wxk007/Documents/zkLib/jline-0.9.94.jar:/home/wxk007/Documents/zkLib/log4j-1.2.16.jar:/home/wxk007/Documents/zkLib/netty-3.7.0.Final.jar:/home/wxk007/Documents/zkLib/slf4j-api-1.6.1.jar:/home/wxk007/Documents/zkLib/slf4j-log4j12-1.6.1.jar:/home/wxk007/Documents/zkLib/zmq.jar *.java
Then do:
java -cp /home/wxk007/Documents/zkLib/zookeeper-3.4.6.jar:/home/wxk007/Documents/zkLib/jline-0.9.94.jar:/home/wxk007/Documents/zkLib/log4j-1.2.16.jar:/home/wxk007/Documents/zkLib/netty-3.7.0.Final.jar:/home/wxk007/Documents/zkLib/slf4j-api-1.6.1.jar:/home/wxk007/Documents/zkLib/slf4j-log4j12-1.6.1.jar:/home/wxk007/Documents/zkLib/zmq.jar: main

the eventService could run then. You should replace the path with your own path
