package pt.throttler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bala-zpt0052 on 07/04/26
 *
 */
public class FixedWindowThrottleAPI
{
	private final int maxRequest;
	private final long windowSizeMs;
	private Map<String, long[]> map = new ConcurrentHashMap<>();

	public FixedWindowThrottleAPI(int maxRequest, long windowSizeMs){
		this.maxRequest = maxRequest;
		this.windowSizeMs = windowSizeMs;
	}

	public boolean allowRequest(String clientId)
	{
		long now = System.currentTimeMillis();
		long currentEpoch = now / windowSizeMs;
		long[] sessionInfo = map.get(clientId);

		if (sessionInfo == null) {
			map.put(clientId, new long[]{1, currentEpoch});
			return true;
		}

		if(sessionInfo[1] != currentEpoch)
		{
			map.put(clientId, new long[]{1, currentEpoch});
			return true;
		}

		if(sessionInfo[0] < maxRequest)
		{
			sessionInfo[0]++;
			return true;
		}
		return false; // Throttle violated
	}



	public static void main(String[] args) throws Exception
	{
		FixedWindowThrottleAPI fixedWindow = new FixedWindowThrottleAPI(3, 500);
		for(int i = 0; i < 5; i++)
		{
			System.out.println("Request: "+ i + "->" + fixedWindow.allowRequest("Client 1"));
		}

		Thread.sleep(5100);

		for(int i = 0; i < 5; i++)
		{
			System.out.println("Request: "+ i + "->" + fixedWindow.allowRequest("Client 1"));
		}
	}
}
