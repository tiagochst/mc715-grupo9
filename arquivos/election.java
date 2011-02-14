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
	    
