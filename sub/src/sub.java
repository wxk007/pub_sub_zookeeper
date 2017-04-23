import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import java.util.Scanner;

/**
 * Pubsub envelope subscriber
 */

public class sub {

    public static void main (String[] args) {
        Scanner mScanner = new Scanner(System.in);

        System.out.println("Please input the port you want to connect");
        int port;
        while(true){
            try{
                port = mScanner.nextInt();
                break;
            }catch (Exception e){
                System.out.println("Please input an integer");
            }
        }

        System.out.println("Please input the topic you want to subscribe");
        String topic = mScanner.next();

        mySub mSub = new mySub(port, topic);

        Thread recMessage = new Thread(() -> {
            mSub.recvMessage();
        });


        Thread showMessage = new Thread(() -> {
            mSub.showCur();
        });

        Thread showHistory = new Thread(() -> {
            mSub.showHis();
        });

        recMessage.start();
        showMessage.start();
        showHistory.start();
    }
}