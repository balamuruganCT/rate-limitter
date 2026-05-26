package pt.throttler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenBucketThrottleAPI
{
    private final double maxTokens;          // bucket capacity
    private final double refillRatePerMs;    // tokens added per millisecond
    private final Map<String, double[]> map = new ConcurrentHashMap<>();

    public TokenBucketThrottleAPI(int maxTokens, double refillRatePerMs)
    {
        this.maxTokens        = maxTokens;
        this.refillRatePerMs  = refillRatePerMs;
    }

    public boolean allowRequest(String clientId)
    {
        long now = System.currentTimeMillis();
		map.putIfAbsent(clientId, new double[]{ maxTokens, now });

        double[] bucket = map.get(clientId);

        synchronized (bucket)
        {
            long elapsed   = now - (long) bucket[1];
            double refilled = elapsed * refillRatePerMs;
            bucket[0] = Math.min(maxTokens, bucket[0] + refilled);
            bucket[1] = now;

            if (bucket[0] >= 1.0)
            {
                bucket[0]--;
                return true;
            }
            return false; // Throttle violated
        }
    }


    public static void main(String[] args) throws Exception
    {
        TokenBucketThrottleAPI tokenBucket = new TokenBucketThrottleAPI(3, 1.0 / 200);
        System.out.println("=== Burst: 5 requests back-to-back (bucket starts full at 3) ===");
        for (int i = 0; i < 5; i++)
        {
            System.out.println("Request " + i + " -> " + tokenBucket.allowRequest("Client 1"));
        }

        System.out.println("\n--- Sleeping 400 ms (refills ~2 tokens) ---");
        Thread.sleep(400);
        System.out.println("\n=== 5 more requests after partial refill ===");
        for (int i = 0; i < 5; i++)
        {
            System.out.println("Request " + i + " -> " + tokenBucket.allowRequest("Client 1"));
        }

        System.out.println("\n--- Sleeping 1000 ms (bucket fully refills) ---");
        Thread.sleep(1000);

        System.out.println("\n=== 5 more requests after full refill ===");
        for (int i = 0; i < 5; i++)
        {
            System.out.println("Request " + i + " -> " + tokenBucket.allowRequest("Client 1"));
        }
    }
}