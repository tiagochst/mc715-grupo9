import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

public class Election implements Watcher,Runnable , DataMonitor.DataMonitorListener{

    static ZooKeeper zk = null;
    static Integer mutex;
    DataMonitor dm;

    Process child; //@@@@@@@@@@@@@@@@@@@@@
    String filename;
    String exec[];


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


    public void run() {
        try {
            synchronized (this) {
                while (!dm.dead) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
        }
    }

    public void closing(int rc) {
        synchronized (this) {
            notifyAll();
        }
    }


    static class StreamWriter extends Thread {
        OutputStream os;

        InputStream is;

        StreamWriter(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
            start();
        }

        public void run() {
            byte b[] = new byte[80];
            int rc;
            try {
                while ((rc = is.read(b)) > 0) {
                    os.write(b, 0, rc);
                }
            } catch (IOException e) {
            }

        }
    }

    public void exists(byte[] data) {
        if (data == null) {
            if (child != null) {
                System.out.println("Killing process");
                child.destroy();
                try {
                    child.waitFor();
                } catch (InterruptedException e) {
                }
            }
            child = null;
        } else {
            if (child != null) {
                System.out.println("Stopping child");
                child.destroy();
                try {
                    child.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
	    /*     try {
                FileOutputStream fos = new FileOutputStream(filename);
                fos.write(data);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
		}*/
	    //    try {
                System.out.println("Starting child");
                //child = Runtime.getRuntime().exec(exec);
                //new StreamWriter(child.getInputStream(), System.out);
                //new StreamWriter(child.getErrorStream(), System.err);
		//         } catch (IOException e) {
		//            e.printStackTrace();
		//       }
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
        String menor() throws KeeperException, InterruptedException{
            int retvalue = -1;
            Stat stat = null;
	    String aux = new String();

            // Get the first element available
            while (true) {
		List<String> list = zk.getChildren(root, true);
		if(list.size() != 0){
		    Integer min = new Integer(list.get(0).substring(7));
		    aux = list.get(0).substring(2);
		    for(String s : list){
			Integer tempValue = new Integer(s.substring(7));
			if(tempValue < min){
			    min = tempValue;
			    aux = s.substring(2);
			   
			}
		    }
		    System.out.println("Variavel aux: " + aux);
		    return aux;
		}
		return "-1";
	    }
	}
	
	void monitora(String s){
	    this.dm = new DataMonitor(zk, "/ELECTION/n_" + s, null, this);
	}
	
    }

    public static void main(String args[]) {
	election(args);           
    }
    
    public static void election(String args[]){
	Queue q = new Queue(args[0], "/ELECTION");
    
	try{
	    int selfId = Integer.parseInt(q.produce(0).substring(13));
	    q.produce(0);
	    System.out.println("Id do filho " + selfId);

	    List<String> list = q.zk.getChildren("/ELECTION", true);
	    int aux = -1;
	    while (q.tamanho() != 0) {
		Integer menor = new Integer(q.menor());
		if(menor != aux){
		    System.out.println("Menor filho:" + menor);
		    aux = menor;
		    if(selfId == menor) 
			System.out.println("Eu sou o lider\n");
		    else{
			System.out.println("O lider nao sou eu");
			System.out.println("Entao eu vou monitorar o lider...\n");
			//	q.monitora(q.menor());
		    }
		}
	    } 
	} catch (KeeperException e){

	} catch (InterruptedException e){

	}
    }
}

