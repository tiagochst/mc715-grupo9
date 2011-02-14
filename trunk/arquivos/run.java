public void run() {
    try {
	synchronized (this) {
	    wait(); // Espera lider cair
	}
    } catch (InterruptedException e) {
    }
}
