import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created by wxk007 on 4/16/17.
 */
public class main {
    public static void main(String args[]) throws InterruptedException, IOException, KeeperException {
        Scanner mScanner = new Scanner(System.in);
        int recPort, sendPort;
        System.out.println("Please input the port of publisher");
        while(true){
            try{
                recPort = mScanner.nextInt();
                break;
            }catch (Exception e){
                System.out.println("Please input an integer");
            }
        }

        System.out.println("Please input the port of subscriber");
        while(true){
            try{
                sendPort = mScanner.nextInt();
                break;
            }catch (Exception e){
                System.out.println("Please input an integer");
            }
        }
        //System.out.println("Please input the topic");
        //String topic = mScanner.next();

        eventService mES = new eventService(recPort, sendPort);

        Thread recCurThread = new Thread(()->{
            try {
                mES.receive();
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread sendCurThread = new Thread(() ->{
            mES.send();
        });

        Thread sendHisThread = new Thread(() -> {
            mES.sendHistory();
        });
        Thread receiveFromZk = new Thread(() ->{
            try {
                mES.receiveFromBuffer();
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });


        recCurThread.start();
        sendCurThread.start();
        sendHisThread.start();
        receiveFromZk.start();
    }
}
