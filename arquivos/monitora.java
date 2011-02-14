void monitora(String s){
    this.dm = new DataMonitor(zk, "/ELECTION/n_" + s, null, this);
}
