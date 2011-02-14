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
