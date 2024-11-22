import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class testConnection {

  public static void main(String[] args) {
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    JedisPool pool = new JedisPool(poolConfig, "44.224.154.230", 6379);
    try(Jedis jedis = pool.getResource()) {
      jedis.set("iris", "evan");
      System.out.println(jedis.get("iris")); // prints bar
    }

    Jedis jedis = new Jedis("44.224.154.230", 6379);
    System.out.println(jedis.ping());

    System.out.println(jedis.get("foo"));


  }

}
