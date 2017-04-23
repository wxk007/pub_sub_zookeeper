/**
 * Created by wxk007 on 4/14/17.
 */
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import java.util.Scanner;

/**
 * Pubsub envelope publisher
 */

// in my pattern, each publisher should send message to a specific event service . They could coordinated by using zookeeper, and send the
//    result to subscriber via the one which has been named as the "main event service"
public class pub {

    /*
    public static void main (String[] args) throws Exception {
        // Prepare our context and publisher
        Context context = ZMQ.context(1);
        Socket publisher = context.socket(ZMQ.PUB);

        publisher.bind("tcp://*:5563");
        while (!Thread.currentThread ().isInterrupted ()) {
            // Write two messages, each with an envelope and content
            publisher.sendMore ("A");
            publisher.send ("We don't want to see this");
            publisher.sendMore ("B");
            publisher.send("We would like to see this");
        }
        publisher.close ();
        context.term ();
    }*/
    public static void main(String args[]){
        Scanner mScanner = new Scanner(System.in);
        System.out.println("Please input the port number you want");
        int port;
        while (true){
            try{
                port = mScanner.nextInt();
                break;
            }catch (Exception e){
                System.out.println("Please input an integer");
            }
        }
        System.out.println("Please input the topic");
        String topic = mScanner.next();
        my_pub publisher = new my_pub(port);
        while(!Thread.currentThread ().isInterrupted ()){
            try {
                publisher.send(topic, "hello");
            } catch (Exception e) {
                publisher.shutDown();
                break;
            }
        }
    }
}