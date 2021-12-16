package lifting;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcmath.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DecoratorLifting {
    public static final int BOUND = 6;

    public static void main(String[] args) {
        ChunkRand rand = new ChunkRand();
        long worldSeed = 1234L & Mth.MASK_48;

        Map<BPos, Integer> calls = new HashMap<>();

        for(int i = 0; i < 35; i++) {
            rand.setPopulationSeed(worldSeed, i * 16, (i + 1) * 16, MCVersion.v1_16_2);
            calls.put(new BPos(i * 16, 0,(i + 1) * 16), rand.nextInt(BOUND));
        }

        int decoBits = Long.numberOfTrailingZeros(BOUND);
        int bits = 16 + 17 + decoBits - 4;
        System.out.println("Starting search for world seed [" + worldSeed + "]" + " with " + calls.size() + " calls.");
        System.out.println("Looking for valid " + bits + " lower bits...");
        long start = System.nanoTime();

        List<Map.Entry<BPos, Integer>> entries = new ArrayList<>(calls.entrySet());

        for(long lowerBits = 0; lowerBits < 1L << bits; lowerBits++) {
            boolean good = true;

            for(Map.Entry<BPos, Integer> entry: entries) {
                rand.setPopulationSeed(lowerBits, entry.getKey().getX(), entry.getKey().getZ(), MCVersion.v1_16_2);

                if(rand.nextInt(BOUND) % (1L << decoBits) != entry.getValue() % (1L << decoBits)) {
                    good = false;
                    break;
                }
            }

            if(good) {
                System.out.println("Found [" + lowerBits + "].");
            }
        }

        System.out.println("Took " + (System.nanoTime() - start) / 1_000_000_000.0D + " seconds.");
    }
}
