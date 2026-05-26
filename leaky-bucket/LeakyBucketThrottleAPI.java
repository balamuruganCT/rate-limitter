package pt.throttler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LeakyBucketThrottleAPI
{
	private final double bucketCapacity;
	private final double leakRatePerMs;
	private final Map<String, double[]> map = new ConcurrentHashMap<>();
	public LeakyBucketThrottleAPI(int bucketCapacity, double leakRatePerMs)
	{
		this.bucketCapacity = bucketCapacity;
		this.leakRatePerMs  = leakRatePerMs;
	}

	public boolean allowRequest(String clientId)
	{
		long now = System.currentTimeMillis();

		map.putIfAbsent(clientId, new double[]{ 0.0, now });

		double[] bucket = map.get(clientId);

		synchronized (bucket)
		{
			long elapsed   = now - (long) bucket[1];
			double leaked  = elapsed * leakRatePerMs;
			bucket[0] = Math.max(0.0, bucket[0] - leaked);
			bucket[1] = now;

			if (bucket[0] < bucketCapacity)
			{
				bucket[0]++;
				return true;
			}
			return false;
		}
	}

	public static void main(String[] args) throws Exception
	{
		LeakyBucketThrottleAPI leakyBucket = new LeakyBucketThrottleAPI(3, 1.0 / 300);

		System.out.println("=== Burst: 5 requests back-to-back (bucket capacity = 3) ===");
		for (int i = 0; i < 5; i++)
		{
			System.out.println("Request " + i + " -> " + leakyBucket.allowRequest("Client 1"));
		}

		System.out.println("\n--- Sleeping 600 ms (leaks ~2 units from bucket) ---");
		Thread.sleep(600);

		System.out.println("\n=== 5 more requests after partial drain ===");
		for (int i = 0; i < 5; i++)
		{
			System.out.println("Request " + i + " -> " + leakyBucket.allowRequest("Client 1"));
		}

		System.out.println("\n--- Sleeping 1500 ms (bucket fully drains) ---");
		Thread.sleep(1500);

		System.out.println("\n=== 5 more requests after full drain ===");
		for (int i = 0; i < 5; i++)
		{
			System.out.println("Request " + i + " -> " + leakyBucket.allowRequest("Client 1"));
		}
	}
}