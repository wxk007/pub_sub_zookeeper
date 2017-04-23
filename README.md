# pub_sub_zookeeper
This is a pub sub model enhanced by zookeeper library.

Pub's duty is to send messages to eventService(MiddleWare).

EventService(MiddleWare) could store the message and zookeeper is implemented to coordinate the messages between different eventServices
Then, the message would be send to subscriber. Because messages in each eventService have been coordinated, so the subscriber can
connected to each of them, it would be same.

Subscriber would subscribe a specific topic and receive messages belong to that topic. Each subscriber can connect to any one of
eventServices.

You have to binding your jdk to zmq and zookeeper library before you run this sample.
