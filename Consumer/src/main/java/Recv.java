import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Recv {
  private final static String QUEUE_NAME = "liftRideEvent_Queue";
  private final static int NUM_Thread = 400;

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("52.35.158.255");
    factory.setPort(5672);
    factory.setUsername("jiali");
    factory.setPassword("12345");
    Connection connection = factory.newConnection();

    JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
    jedisPoolConfig.setMaxTotal(600);
    JedisPool pool = new JedisPool(jedisPoolConfig, "44.224.154.230", 6379);

    ConcurrentHashMap<Integer, List<LiftRideEvent>> liftRide = new ConcurrentHashMap<>();
    ExecutorService executorService = Executors.newFixedThreadPool(NUM_Thread);

    for(int i=0; i<NUM_Thread; i++) {
      executorService.execute(new ProcessLiftRide(liftRide, QUEUE_NAME, connection, pool));
    }

  }
}
