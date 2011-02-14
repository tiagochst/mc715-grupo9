int tamanho() throws KeeperException, InterruptedException{
    Stat stat = new Stat();
    zk.getData("/ELECTION", false, stat);
            
    return stat.getNumChildren();
}