synchronized public void process(WatchedEvent event){
    synchronized (mutex) {
	if(event.getType() != Event.EventType.None) {
	    boolean isNodeDeleted = event.getType().equals(EventType.NodeDeleted);          // Verifica se o evento eh de queda.
	    boolean LiderAtual = event.getPath().equals(lider); //Verifica se evento ocorreu no líder.
            
	    if (isNodeDeleted &&  LiderAtual) {
		System.out.println("O no lider caiu");
		mutex.notify();
		notifyAll(); //Acorda clientes
	    }
	}
    }
}