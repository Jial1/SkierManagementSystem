import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class ProcessLiftRide implements Runnable {
  private ConcurrentHashMap<Integer, List<LiftRideEvent>> liftRide;
  private String json;
  private final String QUEUE_NAME;
  private final Connection connection;
  private final JedisPool jedisPool;


  public ProcessLiftRide(ConcurrentHashMap<Integer, List<LiftRideEvent>> liftRide, String QUEUE_NAME,
      Connection connection, JedisPool jedisPool) {
    this.liftRide = liftRide;
    this.QUEUE_NAME = QUEUE_NAME;
    this.connection = connection;
    this.jedisPool = jedisPool;
  }

  @Override
  public void run() {
    try(Jedis jedis = jedisPool.getResource()) {
      System.out.println(jedis.ping());
      Channel channel = connection.createChannel();
      channel.queueDeclare(QUEUE_NAME, false, false, false, null);

      channel.basicQos(10);
      System.out.println(" [*] Thread waiting for messages. To exit press CTRL+C");

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        LiftRideEvent liftRideEvent = parseLiftRideEvent(message);


        updateSkierLiftRides(liftRideEvent);
        jedis.rpush(getKey(liftRideEvent), getLiftRidesData(liftRideEvent));


        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        System.out.println( "Callback thread ID = " + Thread.currentThread().getId() + " Received '" + message + "'");
      };
      // process messages
      channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private String getLiftRidesData(LiftRideEvent liftRideEvent) {
    String seasonId = liftRideEvent.getSeasonID();
    String dayId = liftRideEvent.getDayID();
    String liftId = String.valueOf(liftRideEvent.getLiftID());
    String time = String.valueOf(liftRideEvent.getTime());
    String resortId = String.valueOf(liftRideEvent.getResortID());
    return seasonId + "-" + dayId + "-" + liftId + "-" + time + "-" + resortId;
  }

  private String getKey(LiftRideEvent liftRideEvent) {
    return String.valueOf(liftRideEvent.getSkierID());
  }


  private void updateSkierLiftRides(LiftRideEvent liftRideEvent) {
    int skierID = liftRideEvent.getSkierID();
    liftRide.computeIfAbsent(skierID, k -> Collections.synchronizedList(new ArrayList<>())).add(liftRideEvent);
  }

  private LiftRideEvent parseLiftRideEvent(String message) {
    return new Gson().fromJson(message, LiftRideEvent.class);
  }


}
