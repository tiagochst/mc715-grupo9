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
	return INFINITO;
    }
}
