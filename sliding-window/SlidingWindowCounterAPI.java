package pt.throttler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bala-zpt0052 on 08/04/26
 *
 */
public class SlidingWindowCounterAPI
{
	private int maxRequest;
	private long windowSizeMs;

	private SlidingWindowCounterAPI(){}

	public SlidingWindowCounterAPI(int maxRequest, long windowSizeMs)
	{
		this.maxRequest = maxRequest;
		this.windowSizeMs = windowSizeMs;
	}

	// [epoch, count]
	Map<String, long[]> prev = new ConcurrentHashMap<>();
	Map<String, long[]> curr = new ConcurrentHashMap<>();

	public synchronized boolean isAllowRequest(String clientId)
	{
		long now = System.currentTimeMillis();
		long epochTime = now / windowSizeMs;

		long[] currWindow  = curr.get(clientId);
		long[] prevWindow = prev.get(clientId);

		if(currWindow == null || currWindow[0] != epochTime)
		{
			prev.put(clientId, currWindow != null ? currWindow : new long[]{epochTime - 1, 0});
			currWindow = new long[]{epochTime, 0};
			curr.put(clientId, currWindow);
		}

		long prevCount = (prevWindow != null && prevWindow[0] == epochTime - 1) ? prevWindow[1] : 0;
		long elapsed = now % windowSizeMs;
		double weight = 1.0 - (((double) elapsed) / windowSizeMs);

		double approxCount = prevCount * weight + currWindow[1];

		if(approxCount < maxRequest)
		{
			currWindow[1] ++;
			return true;
		}
		return false;
	}

	public static void main(String[] args) throws Exception
	{
		SlidingWindowCounterAPI slidingWindowCounter = new SlidingWindowCounterAPI(3, 1000);
		for(int i = 0; i < 5; i++)
		{
			System.out.println(slidingWindowCounter.isAllowRequest("client1"));
			System.out.println(slidingWindowCounter.isAllowRequest("client2"));
		}

		Thread.sleep(1100);

		for(int i = 0; i < 5; i++)
		{
			System.out.println(slidingWindowCounter.isAllowRequest("client1"));
			System.out.println(slidingWindowCounter.isAllowRequest("client2"));
		}
	}
}
