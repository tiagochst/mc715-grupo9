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
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

public class Election implements Watcher,Runnable , DataMonitor.DataMonitorListener{

    static ZooKeeper zk = null;
    static Integer mutex;
    DataMonitor dm;

    String lider = null;
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
    }
    /* Verifica processo de queda do no lider.
     * Caso o lider caia, acorda clientes que querem ser o lider.
     * Cada cliente verifica o novo lider.
     */
    synchronized public void process(WatchedEvent event) {
        synchronized (mutex) {
	    if(event.getType() != Event.EventType.None) {
		boolean isNodeDeleted = event.getType().equals(EventType.NodeDeleted);          // Verifica se o evento eh de queda.
		boolean LiderAtual = event.getPath().equals(lider); //Verifica se evento ocorreu no lider.
		
		if (isNodeDeleted &&  LiderAtual) {
		    System.out.println("O no lider caiu");
		    mutex.notify();
		    notifyAll(); //Acorda clientes
		}
	    }
	}
    }
    /* Insere na classe o lider atual calculado pela classe queue. */
    public void SetLider(String s) {
	this.lider = s;
    }

    public void run() {
        try {
            synchronized (this) {
		wait(); // Espera lider cair
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
	// Funcao nao usada. Executada quando ocorre mudancas no lider.
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

	/* Retorna numero de clientes esperando para ser lider. */
	int tamanho() throws KeeperException, InterruptedException{
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
		    return aux;
		}
		return "-1";
	    }
	}
	
	/* Inicia monitoramento do no lider. */
	void monitora(String s){
	    this.dm = new DataMonitor(zk, "/ELECTION/n_" + s, null, this);
	}
    }

    public static void main(String args[]) {
	election(args);           
    }
    
    /* Cada cliente cria seu znode e espera ser o lider. */
    public static void election(String args[]){
	Queue q = new Queue(args[0], "/ELECTION");
	try{
	    int selfId = Integer.parseInt(q.produce(0).substring(13)); //Criacao do znode
	    System.out.println("Meu ID: " + selfId);

	    List<String> list = q.zk.getChildren("/ELECTION", true);
	    int aux = -1;
	    while (q.tamanho() != 0) {
		Integer menor = new Integer(q.menor());
		if(selfId < menor){
		    System.out.println("Eu morri!!");
		    return;
		}
		if(menor != aux){
		    System.out.println("Menor filho:" + menor);
		    aux = menor;
		    q.SetLider( "/ELECTION/n_" + q.menor());
		    if(selfId == menor){ //Verifica se o menor id eh o meu.
			System.out.println("Eu sou o lider\n");
		    }else{
			naoLider(q);
		    }
		}
	    }
	} catch (KeeperException e){
	} catch (InterruptedException e){
	}
    }
    static public void naoLider(Queue q){
	try{
	    System.out.println("O lider nao sou eu");
	    System.out.println("Entao eu vou monitorar o lider...\n");
	    q.monitora(q.menor());
	    q.run();
	    System.out.println("Fique esperando algo acontecer...\n");
	} catch (KeeperException e){
	} catch (InterruptedException e){
	}
	return;
    }
}

