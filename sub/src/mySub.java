import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ.Context;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by wxk007 on 4/16/17.
 */
public class mySub {
    //this context is used to receive current message
    private Context curContext;
    private Socket curSocket;
    private String port;

    //this context is used to receive history message
  //  private Context hisContext;
   // private Socket hisSocket;

    //describe the topic we sbuscribed
    private String topic;

    //this queue is only used to store current message, the length should always be no more than 1
    //we should implement a showCur method later to print this queue
    private ReentrantLock curLock;
    private Condition curCond;
    private volatile Queue<message> curMessage;

    //this queue is used to store history message, we should implement a showHis method later to print this queue
    private ReentrantLock hisLock;
    private Condition hisCond;
    private volatile Queue<message> hisMessage;

    public mySub(int port, String topic){
        this.port = Integer.toString(port);
        this.topic = topic;
        curContext = ZMQ.context(1);
        curSocket = curContext.socket(ZMQ.SUB);

        //hisContext = ZMQ.context(1);
       // hisSocket = hisContext.socket(ZMQ.SUB);

        this.curLock = new ReentrantLock();
        this.curCond = curLock.newCondition();
        this.curMessage = new LinkedList<>();

        this.hisLock = new ReentrantLock();
        this.hisCond = hisLock.newCondition();
        this.hisMessage = new LinkedList<>();

        curSocket.connect("tcp://localhost:" + this.port);
       // hisSocket.connect("tcp://localhost:" + this.port);
    }

    //we could use threads to add or remove the current item inside the buffer, which is way more faster
    public void recvMessage(){
        curSocket.subscribe(this.topic.getBytes());
        while (!Thread.currentThread ().isInterrupted ()){
            String curTopic = curSocket.recvStr();
            String curContent = curSocket.recvStr();
            curContent = curContent.replaceAll(" ","");
            String[] isHistory = curContent.split(",");
            message tempHis, tempMessage;
            //if the receiving message is a history, add it into the history buffer
            if (isHistory.length >= 2){
                tempHis = new message(curTopic, isHistory[0]);
                addToHisBuffer(tempHis);
            }
            //else, add it to current buffer
            else{
                tempMessage = new message(curTopic, curContent);
                addToCurBuffer(tempMessage);
            }
        }
    }

    //I would test it latter
    private void addToHisBuffer(message tempHis){
            hisLock.lock();
            while(hisMessage.size() >= 5){
                try {
                    hisCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            hisMessage.add(tempHis);
            //we should signal the waiting thread if we've got enough history record
            if(hisMessage.size() >= 5)
                hisCond.signal();
            hisLock.unlock();

    }

    private void addToCurBuffer(message tempMessage){
            curLock.lock();
            while(curMessage.size() > 0){
                try {
                    curCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            curMessage.add(tempMessage);
            if(curMessage.size() != 0){
                curCond.signal();
            }
            curLock.unlock();

    }

    /*
    //this should be deleted latter
    public void recvHistory(){
        String hisTopic = "history";
        hisSocket.subscribe(hisTopic.getBytes());
        while (!Thread.currentThread().isInterrupted()){
            String tempTopic = hisSocket.recvStr();
            String tempContent = hisSocket.recvStr();
            message tempMessage = new message(this.topic, tempContent);
            hisLock.lock();
            while(hisMessage.size() >= 5){
                try {
                    hisCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            hisMessage.add(tempMessage);
            //we should signal the waiting thread if we've got enough history record
            if(hisMessage.size() >= 5)
                hisCond.signal();
            hisLock.unlock();

        }
    }*/

    //this method is used to display current message
    public void showCur(){
        while (!Thread.currentThread ().isInterrupted ()){
            curLock.lock();
            while(curMessage.size() == 0){
                try {
                    curCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            message tempMessage = curMessage.poll();
            System.out.print("We've received message: " + tempMessage.toString() + "\n");
            if(curMessage.size() == 0)
                curCond.signal();
            curLock.unlock();
        }
    }

    //this method is used to display History message
    public void showHis(){
        while (!Thread.currentThread ().isInterrupted ()){
            hisLock.lock();
            while(hisMessage.size() < 5){
                try {
                    hisCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while(hisMessage.size() != 0){
                message tempMessage = hisMessage.poll();
                System.out.print("Now showing history: " + this.topic + " : " + tempMessage.getContent() + "\n");
            }
            if(hisMessage.size() == 0)
                hisCond.signal();
            hisLock.unlock();
        }
    }
}

class message{
    private String topic;
    private String content;
    public message(String topic, String content){
        this.topic = topic;
        this.content = content;
    }
    public String getTopic(){
        return topic;
    }
    public String getContent(){
        return content;
    }
    public String toString(){
        return topic + " : " + content;
    }
}