package redis.embedded;

import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static org.junit.Assert.*;

public class RedisServerTest {

	private RedisServer redisServer;
	
	@Test(timeout = 1500L)
	public void testSimpleRun() throws Exception {
		redisServer = new RedisServer(6379);
		redisServer.start();
		Thread.sleep(1000L);
		redisServer.stop();
	}
	
	@Test(expected = RuntimeException.class)
	public void shouldNotAllowMultipleRunsWithoutStop() throws Exception {
		try {
			redisServer = new RedisServer(6379);
			redisServer.start();
			redisServer.start();
		} finally {
			redisServer.stop();
		}
	}
	
	@Test
	public void shouldAllowSubsequentRuns() throws Exception {
		redisServer = new RedisServer(6379);
		redisServer.start();
		redisServer.stop();
		
		redisServer.start();
		redisServer.stop();
		
		redisServer.start();
		redisServer.stop();
	}
	
	@Test
	public void testSimpleOperationsAfterRun() throws Exception {
		redisServer = new RedisServer(6379);
		redisServer.start();
		
		JedisPool pool = null;
		Jedis jedis = null;
		try {
			pool = new JedisPool("localhost", 6379);
			jedis = pool.getResource();
			jedis.mset("abc", "1", "def", "2");
			
			assertEquals("1", jedis.mget("abc").get(0));
			assertEquals("2", jedis.mget("def").get(0));
			assertEquals(null, jedis.mget("xyz").get(0));
		} finally {
			if (jedis != null)
				pool.returnResource(jedis);
			redisServer.stop();
		}
	}

    @Test
    public void shouldIndicateInactiveBeforeStart() throws Exception {
        redisServer = new RedisServer(6379);
        assertFalse(redisServer.isActive());
    }

    @Test
    public void shouldIndicateActiveAfterStart() throws Exception {
        redisServer = new RedisServer(6379);
        redisServer.start();
        assertTrue(redisServer.isActive());
        redisServer.stop();
    }

    @Test
    public void shouldIndicateInactiveAfterStop() throws Exception {
        redisServer = new RedisServer(6379);
        redisServer.start();
        redisServer.stop();
        assertFalse(redisServer.isActive());
    }
}
