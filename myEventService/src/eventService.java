/**
 * Created by wxk007 on 4/16/17.
 */
import com.sun.jmx.remote.internal.ArrayQueue;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.zookeeper.*;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import org.apache.zookeeper.ZooDefs.Ids;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;



//the logic of my event service: it would create 3 threads, they are used for: receive message from pub, send current
// message to subscriber and send history message to subscriber

//new: the message should accept all input content and split the topic by "/"

//if you signal up the wrong thread, you have to signal again until you get the correct one, use signalAll


//it would be changed after I figure out how to use zookeeper here
public class eventService {
    //this lock is used to protect the private field mHisList, which is used to store the history information
    private ReentrantLock mHisLock;
    private Condition mHisCond;

    private ReentrantLock mCurLock;
    private Condition mCurCond;

    //this zk object is used to coordinate among eventservices

    //history information would be send when it reached five
    private volatile Queue<message> mHisList;

    private volatile Queue<message> mCurMessage;

    //this port is the one that can get the message from pub
    private String getPort;
    private Context getContext;
    private Socket getSocket;

    //implemented zookeeper here, this pattern would ensure that zookeeper could connect to the server and return it
    private CountDownLatch connectedSignal = new CountDownLatch(1);
    private ZooKeeper zk;

    //this indicate the number of this es object, decide its zookeeper node
    private int number;

    private ZooKeeper connect() throws IOException {
        ZooKeeper zk = new ZooKeeper("127.0.0.1:2181", 300000000, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if(event.getState()== Event.KeeperState.SyncConnected){
                    connectedSignal.countDown();
                }
            }
        });
        try {
            connectedSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zk;
    }
    //this port is used to send current and history message to the sub
    private String sendPort;
    private Context sendContext;
    private Socket sendSocket;

    //getPort is used to set the port number that we used to get messages, sendPort is used to set the port number that
    //we used to send messages and isLeader is a flag that we could use it decided whether this object is going to be the
    //Leader(create main node) you should create leader firstly
    public eventService(int getPort, int sendPort) throws IOException, KeeperException, InterruptedException {

        //this part is for zookeeper settingup
        this.zk = connect();
        //we're going to create a main node here, if there's not a main node already
        if(zk.exists("/main", false) == null){
            zk.create("/main", "".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        this.getPort = Integer.toString(getPort);
        this.sendPort = Integer.toString(sendPort);
        //this.topic = topic;
        mHisLock = new ReentrantLock();
        mHisCond = mHisLock.newCondition();
        mCurLock = new ReentrantLock();
        mCurCond = mCurLock.newCondition();
        mHisList = new LinkedList<>();
        mCurMessage = new LinkedList<>();
        getContext = ZMQ.context(1);
        getSocket = getContext.socket(ZMQ.SUB);
        sendContext = ZMQ.context(1);
        sendSocket = sendContext.socket(ZMQ.PUB);

        getSocket.bind("tcp://*:" + this.getPort);

        sendSocket.bind("tcp://*:" + this.sendPort);
    }

    public void receive() throws KeeperException, InterruptedException {
        getSocket.subscribe("".getBytes());


        int flag = 0;
        while(!Thread.currentThread().isInterrupted()){
            //String topic = getSocket.recvStr();
            byte[] curContent = getSocket.recv();

            //System.out.print("received");
            String content = new String(curContent);

            //get current data from zookeeper and add the new one
            String curBuffer = new String(zk.getData("/main", false, null));
            //if there's no message in the buffer right now
            if(curBuffer.length() == 0){
                String temp = content + ";1;";
                zk.setData("/main", temp.getBytes(), -1);
            }
            //else, append current message to the buffer and delete first half of it if the number of messages reach 100
            else{
                String[] curNumber = curBuffer.split(";");
                try {
                    int a = Integer.parseInt(curNumber[curNumber.length - 1]);
                   /* if(a % 1000 == 0) {
                        //a = 0;
                        int size = curBuffer.length();
                        String newBuffer = curBuffer.substring(size/2, size);
                        // System.out.print(newBuffer);
                        String curNum = Integer.toString(a + 1);
                        String curMes = newBuffer +content+ ";" + curNum + ";";
                        //append the new message into the message set
                        zk.setData("/main", curMes.getBytes(), -1);

                    }
                    else{
                        String curNum = Integer.toString(a + 1);
                        String curMes = curBuffer +content+ ";" + curNum + ";";
                        //append the new message into the message set
                        zk.setData("/main", curMes.getBytes(), -1);
                    }*/
                    String curNum = Integer.toString(a + 1);
                    String curMes = curBuffer +content+ ";" + curNum + ";";
                    //append the new message into the message set
                    zk.setData("/main", curMes.getBytes(), -1);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            content = content.replaceAll(" ","");
            String[] Message = content.split("/");
            System.out.println("received: " + Message[0] + " : " + Message[1]);
            //try to write the information into the CurList
            message curMessage = new message(Message[0],Message[1]);
            mCurLock.lock();
            //curMessage can only have one single message in it, guarentee it
            while(mCurMessage.size() >= 10){
                try {
                    mCurCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mCurMessage.add(curMessage);
            //after you add it into the list, you have to signal up the waitting thread who is trying to get message from it
            mCurCond.signalAll();
            mCurLock.unlock();

            mCurLock.lock();

            //we should put the
            mHisLock.lock();
            //the history list can have no more than 5 messages
            while(mHisList.size() >= 20){
                try {
                    mHisCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mHisList.add(curMessage);
            //signal the sending method if we've got enough history messages
            if(mHisList.size() > 4)
                mHisCond.signalAll();
            mHisLock.unlock();

            flag++;
            String tempBuffer = new String(zk.getData("/main", false, null));
            String[] zkQueue = tempBuffer.split(";");
            //String content = "";
            int positon = Arrays.binarySearch(zkQueue, Integer.toString(flag));
            //that means the node has been deleted
            if(positon < 0) continue;
            String tempMessage = zkQueue[positon - 1];
            String[] contentArray = tempMessage.split("/");
            message mMessage = new message(contentArray[0], contentArray[1]);

            mCurLock.lock();
            //curMessage can only have one single message in it, guarentee it
            while(mCurMessage.size() >= 10){
                try {
                    mCurCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //Boolean flag = true;
            Boolean isExist = true;
            for(message item : mCurMessage){
                if(item.getTopic().equals(mMessage.getTopic()) && item.getContent().equals(mMessage.getContent())){
                    isExist = false;
                }
            }
            //we can not add the element that already in the list into it
            if(isExist){
                mCurMessage.add(mMessage);
            }
            //after you add it into the list, you have to signal up the waitting thread who is trying to get message from it
            mCurCond.signalAll();
            mCurLock.unlock();

            mHisLock.lock();
            //the history list can have no more than 5 messages
            while(mHisList.size() >= 20){
                try {
                    mHisCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mHisList.add(mMessage);
            //signal the sending method if we've got enough history messages
            if(mHisList.size() > 4)
                mHisCond.signalAll();
            mHisLock.unlock();

        }

    }
    


    //send method is merely used to send current message, has nothing to do with history list
    public void send(){
        while(!Thread.currentThread().isInterrupted()){
            mCurLock.lock();
            while (mCurMessage.size() < 10){
                try {
                    mCurCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.printf("We've got %d messages in current list, sending them \n", mCurMessage.size());
            while (mCurMessage.size() != 0){
                message tempMessage = mCurMessage.poll();
                System.out.println("sending: " + tempMessage.toString());
                sendSocket.sendMore(tempMessage.getTopic());
                sendSocket.send(tempMessage.getContent());
            }

            if(mCurMessage.size() == 0)
                mCurCond.signalAll();
            mCurLock.unlock();
        }

    }

    //this method is used to send history list towards subscribers
    public void sendHistory(){
        while(!Thread.currentThread().isInterrupted()){
            mHisLock.lock();
            while(mHisList.size() < 20){
                try {
                    mHisCond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.printf("Sending history messages to subscriber, we've got %d messages in historylist \n", mHisList.size());
            while(mHisList.size() != 0){
                message tempMessage = mHisList.poll();
                String historyTopic = tempMessage.getTopic();
                String content = tempMessage.getContent() + ",history";
                sendSocket.sendMore(historyTopic);
                sendSocket.send(content);
            }
            //after sending history message, signal the waiting thread
            if(mHisList.size() == 0)
                mHisCond.signalAll();
            mHisLock.unlock();
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