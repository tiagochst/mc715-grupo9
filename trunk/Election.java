import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

public class Election implements Watcher, Runnable, DataMonitor.DataMonitorListener{

    static ZooKeeper zk = null;
    static Integer mutex;
    DataMonitor dm;

    Process child; //@@@@@@@@@@@@@@@@@@@@@

    String root;

    Election(String address) {
        if(zk == null){
            try {
                System.out.println("Starting ZK:");
                zk = new ZooKeeper(address, 3000, this);
                mutex = new Integer(-1);
		System.out.println("Finished starting ZK: " + zk);
            } catch (IOException e) {
                System.out.println(e.toString());
                zk = null;
            }
        }
        //else mutex = new Integer(-1);
    }

    synchronized public void process(WatchedEvent event) {
        synchronized (mutex) {
            //System.out.println("Process: " + event.getType());
            mutex.notify();
        }
    }

    /**
     * Producer-Consumer queue
     */
    static public class Queue extends Election {

        /**
         * Constructor of producer-consumer queue
         *
         * @param address
         * @param name
         */
        Queue(String address, String name) {
            super(address);
            this.root = name;
            // Create ZK node name
            if (zk != null) {
                try {
                    Stat s = zk.exists(root, false);
                    if (s == null) {
                        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
				  CreateMode.PERSISTENT);
                    }
                } catch (KeeperException e) {
                    System.out
			.println("Keeper exception when instantiating queue: "
				 + e.toString());
                } catch (InterruptedException e) {
                    System.out.println("Interrupted exception");
                }
            }
        }

        /**
         * Add element to the queue.
         *
         * @param i
         * @return Znode
         */

	String produce(int i) throws KeeperException, InterruptedException{
            ByteBuffer b = ByteBuffer.allocate(4);
            byte[] value;

            // Add child with value i
            b.putInt(i);
            value = b.array();
            return zk.create(root + "/n_", value, Ids.OPEN_ACL_UNSAFE,
			     CreateMode.EPHEMERAL_SEQUENTIAL);
        }


        /**
         * Remove first element from the queue.
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */
        int consume() throws KeeperException, InterruptedException{
            int retvalue = -1;
            Stat stat = null;

            // Get the first element available
            while (true) {
                synchronized (mutex) {
                    List<String> list = zk.getChildren(root, true);
                    if (list.size() == 0) {
                        System.out.println("Going to wait");
                        mutex.wait();
                    } else {
                        Integer min = new Integer(list.get(0).substring(7));
                        for(String s : list){
                            Integer tempValue = new Integer(s.substring(7));
                            //System.out.println("Temporary value: " + tempValue);
                            if(tempValue < min) min = tempValue;
                        }
                        System.out.println("Temporary value: " + root + "/element" + min);
                        byte[] b = zk.getData(root + "/element" + min,
					      false, stat);
		        zk.delete(root + "/element" + min, 0);
                        ByteBuffer buffer = ByteBuffer.wrap(b);
                        retvalue = buffer.getInt();

                        return retvalue;
                    }
                }
            }
        }

        int tamanho() throws KeeperException, InterruptedException{
	    //List<String> list = zk.getChildren(root, true);
	    //return list.size();
	    Stat stat = new Stat();
	    zk.getData("/ELECTION", false, stat);
	    
	    return stat.getNumChildren();
	}

        /**
         * Acha o menor elemento da fila.
         *
         * @return menor ID
         * @throws KeeperException
         * @throws InterruptedException
         */
        int menor() throws KeeperException, InterruptedException{
            int retvalue = -1;
            Stat stat = null;
	    //String aux = new String();

            // Get the first element available
            while (true) {
		List<String> list = zk.getChildren(root, true);
		if(list.size() != 0){
		    Integer min = new Integer(list.get(0).substring(7));
		    for(String s : list){
			Integer tempValue = new Integer(s.substring(7));
			if(tempValue < min){
			    min = tempValue;
			    String aux = new String(s.substring(7));
			    System.out.println("Variavel aux: " + aux);
		    }
		    dm = new DataMonitor(zk, "/ELECTION/n_" + aux, null, this);
		    return min;
		}
		return -1;
	    }
	}
    }

    public static void main(String args[]) {
	election(args);          
    }
    
    public static void election(String args[]){
	Queue q = new Queue(args[0], "/ELECTION");
    
	try{
	    int selfId = Integer.parseInt(q.produce(0).substring(13));
	    System.out.println("Id do filho" + selfId);

	    List<String> list = q.zk.getChildren("/ELECTION", true);
	    int aux = -1;
	    while (q.tamanho() != 0) {
		if(q.menor() != aux){
		    System.out.println("Menor filho:" + q.menor());
		    aux = q.menor();
		    if(selfId == q.menor()) 
			System.out.println("Eu sou o lider\n");
		    else
			System.out.println("O lider nao sou eu\n");
		}
	    } 
	} catch (KeeperException e){

	} catch (InterruptedException e){

	}
    }
}

