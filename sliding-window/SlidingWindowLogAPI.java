package pt.throttler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bala-zpt0052 on 07/04/26
 *
 */
public class SlidingWindowLogAPI
{
	private int maxRequests;
	private long windowSizeMs;

	private SlidingWindowLogAPI()
	{
	}

	public SlidingWindowLogAPI(int maxRequests, long windowSizeMs)
	{
		this.maxRequests = maxRequests;
		this.windowSizeMs = windowSizeMs;
	}

	private Map<String, Deque<Long>> log = new ConcurrentHashMap<>();

	public synchronized boolean isAllowRequest(String clientId)
	{
		long now = System.currentTimeMillis();
		long sessionTime = now - windowSizeMs;
		Deque<Long> logInfo = log.computeIfAbsent(clientId, k -> new ArrayDeque<>());

		while(!logInfo.isEmpty() && logInfo.peekFirst() < sessionTime)
		{
			logInfo.pollFirst();
		}

		if(logInfo.size() < maxRequests)
		{
			logInfo.addLast(now);
			return true;
		}

		return false;
	}

	public static void main(String[] args) throws Exception
	{
		SlidingWindowLogAPI slidingWindowLogAPI = new SlidingWindowLogAPI(3, 1000);
		for(int i = 0; i < 5; i++)
		{
			System.out.println(slidingWindowLogAPI.isAllowRequest("client1"));
			System.out.println(slidingWindowLogAPI.isAllowRequest("client2"));
		}

		Thread.sleep(1100);

		for(int i = 0; i < 5; i++)
		{
			System.out.println(slidingWindowLogAPI.isAllowRequest("client1"));
			System.out.println(slidingWindowLogAPI.isAllowRequest("client2"));
		}
	}
}
