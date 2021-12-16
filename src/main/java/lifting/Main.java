
package lifting;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.util.pos.RPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class Main {
    public static final MCVersion VERSION = MCVersion.v1_17;
    public static final DesertPyramid DESERT_PYRAMID = new DesertPyramid(VERSION);
    public static final SwampHut SWAMP_HUT = new SwampHut(VERSION);
    public static final Village VILLAGE = new Village(VERSION);
    public static final Igloo IGLOO = new Igloo(VERSION);
    public static final JunglePyramid JUNGLE_TEMPLE = new JunglePyramid(VERSION);
    public static final PillagerOutpost PILLAGER_OUTPOST = new PillagerOutpost(VERSION);
    public static final Shipwreck SHIPWRECK = new Shipwreck(VERSION);


    public static void main(String[] args) {
        long structureSeed = 90212;
        List<Data> dataList = new ArrayList<>();

        dataList.addAll(generateData(structureSeed, SWAMP_HUT, 3));
        dataList.addAll(generateData(structureSeed, VILLAGE, 3));
        dataList.addAll(generateData(structureSeed, IGLOO, 3));
        dataList.addAll(generateData(structureSeed, PILLAGER_OUTPOST, 3));
        dataList.addAll(generateData(structureSeed, DESERT_PYRAMID, 3));
        dataList.addAll(generateData(structureSeed, JUNGLE_TEMPLE, 3));
        dataList.addAll(generateData(structureSeed, SHIPWRECK, 3));
        List<Long> seeds = crack(dataList);
        for (Long seed : seeds) {
            System.out.println(seed);
        }
    }

    public static List<Long> crack(List<Data> dataList) {
        // You could first lift on 1L<<18 with %2 since that would be a smaller range
        // Then lift on 1<<19 with those 1<<18 fixed with % 4 and for nextInt(24)
        // You can even do %8 on 1<<20 (however we included shipwreck so only nextInt(20) so 1<<19 is the max here
        Stream<Long> lowerBitsStream = LongStream.range(0, 1L << 19).boxed().filter(lowerBits -> {
            ChunkRand rand = new ChunkRand();
            for (Data data : dataList) {
                rand.setRegionSeed(lowerBits, data.regionData.regionX, data.regionData.regionZ, data.salt, VERSION);
                if (rand.nextInt(data.structure.getOffset()) % 4 != data.regionData.offsetX % 4 || rand.nextInt(data.structure.getOffset()) % 4 != data.regionData.offsetZ % 4) {
                    return false;
                }
            }
            return true;
        });

        Stream<Long> seedStream = lowerBitsStream.flatMap(lowerBits ->
            LongStream.range(0, 1L << (48 - 19))
                .boxed()
                .map(upperBits -> (upperBits << 19) | lowerBits)
        );

        Stream<Long> strutureSeedStream = seedStream.filter(seed -> {
            ChunkRand rand = new ChunkRand();
            for (Data data : dataList) {
                rand.setRegionSeed(seed, data.regionData.regionX, data.regionData.regionZ, data.salt, VERSION);
                if (rand.nextInt(data.structure.getOffset()) != data.regionData.offsetX || rand.nextInt(data.structure.getOffset()) != data.regionData.offsetZ) {
                    return false;
                }
            }
            return true;
        });
        return strutureSeedStream.parallel().collect(Collectors.toList());
    }

    public static List<Data> generateData(long structureSeed, UniformStructure<?> structure, int count) {
        List<Data> res = new ArrayList<>(count);
        ChunkRand rand = new ChunkRand();
        for (int i = 0; i < count; i++) {
            RPos rpos = getRandomRPos(structure);
            CPos cPos = structure.getInRegion(structureSeed, rpos.getX(), rpos.getZ(), rand);
            // WARNING Pillager outpost can be exploited to check the nextInt(5)!=0 also but they will produce some nulls here
            if (cPos!=null){
                res.add(new Data(structure, cPos));
            }
        }
        return res;
    }

    public static RPos getRandomRPos(UniformStructure<?> structure) {
        Random random = new Random();
        return new RPos(random.nextInt(10000) - 5000, random.nextInt(10000) - 5000, structure.getSpacing());
    }

    public static class Data {
        public UniformStructure.Data<?> regionData;
        public int salt;
        public UniformStructure<?> structure;

        public Data(UniformStructure<?> structure, int chunkX, int chunkZ) {
            this.regionData = structure.at(chunkX, chunkZ);
            this.salt = structure.getSalt();
            this.structure = structure;
        }

        public Data(UniformStructure<?> structure, CPos cPos) {
            this(structure, cPos.getX(), cPos.getZ());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Data data = (Data) o;
            return salt == data.salt &&
                Objects.equals(regionData, data.regionData);
        }

        @Override
        public String toString() {
            return "Data{" +
                "regionData=" + regionData.chunkX + " " + regionData.chunkZ +
                ", salt=" + salt +
                '}';
        }

        @Override
        public int hashCode() {
            return Objects.hash(regionData, salt);
        }

        public UniformStructure.Data<?> getRegionData() {
            return regionData;
        }

        public void setRegionData(UniformStructure.Data<?> regionData) {
            this.regionData = regionData;
        }
    }
}
