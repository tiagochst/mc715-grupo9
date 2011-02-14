String produce(int i) throws KeeperException, InterruptedException{
    ByteBuffer b = ByteBuffer.allocate(4);
    byte[] value;

    // Add child with value i
    b.putInt(i);
    value = b.array();
    return zk.create(root + "/n_", value, Ids.OPEN_ACL_UNSAFE,
		     CreateMode.EPHEMERAL_SEQUENTIAL);
}