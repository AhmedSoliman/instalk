package im.instalk.store

trait CassandraStatements {
  this: CassandraConfig =>
  private def tableName = s"${keyspace}.${table}"

  def createKeyspace = s"""
      CREATE KEYSPACE IF NOT EXISTS ${keyspace}
      WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : ${replicationFactor} };
    """

  def createTable = s"""
      CREATE TABLE IF NOT EXISTS ${tableName} (
        room_id text,
        seq_nr bigint,
        operation ascii,
        topic text STATIC,
        data text,
        delivered_to list<text>,
        PRIMARY KEY (room_id, seq_nr))
        WITH CLUSTERING ORDER BY (seq_nr DESC);
    """

  def createEvent =
    s"""
      INSERT INTO ${tableName} (room_id, seq_nr, operation, topic, data, delivered_to)
      VALUES (?, ?, ?, ?, ?, ?);
    """

  def selectLatestEvents = s"""
      SELECT * FROM ${tableName} WHERE
        room_id = ? ORDER BY seq_nr DESC
        LIMIT $limit;
    """
  def selectOlderEvents = s"""
      SELECT * FROM ${tableName} WHERE
        room_id = ? AND seq_nr < ? ORDER BY seq_nr DESC
        LIMIT ${limit};
    """

  def selectLatestSeqNr =
    s"""
      SELECT seq_nr FROM ${tableName} WHERE
        room_id = ? ORDER BY seq_nr DESC
        LIMIT 1;
     """

  def selectLatestTopic =
    s"""
      SELECT topic FROM ${tableName} WHERE
        room_id = ? ORDER BY seq_nr DESC
        LIMIT 1;
     """

  def dropRoom =
    s""" DELETE FROM ${tableName} WHERE room_id = ?"""

  def dropTable =
    s""" DROP TABLE IF EXISTS ${tableName}"""

}