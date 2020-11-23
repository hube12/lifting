
package lifting;

import kaptainwutax.featureutils.structure.PillagerOutpost;
import kaptainwutax.featureutils.structure.RegionStructure;
import kaptainwutax.featureutils.structure.Village;
import kaptainwutax.seedutils.mc.ChunkRand;
import kaptainwutax.seedutils.mc.MCVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Main {
    private static final MCVersion version = MCVersion.v1_14_4;
    private static final PillagerOutpost OUTPOST = new PillagerOutpost(version);
    private static final Village VILLAGE = new Village(version);

    public static void main(String[] args) {

        List<Data> dataList = new ArrayList<>();
        ChunkRand rand = new ChunkRand();
        dataList.add(new Data(VILLAGE, -32,11));
        dataList.add(new Data(VILLAGE, -22, 128));
        dataList.add(new Data(VILLAGE, -29, 1319));
        dataList.add(new Data(VILLAGE, -350, -26));
        dataList.add(new Data(VILLAGE, -333, -2611));
        for (Data data : dataList) {
            System.out.println(data);
        }
        for (long lowerBits = 0; lowerBits < 1L << 19; lowerBits++) {
            boolean good = true;

            for (Data data : dataList) {
                rand.setRegionSeed(lowerBits, data.regionData.regionX, data.regionData.regionZ, data.salt, version);

                if (rand.nextInt(24) % 4 != data.regionData.offsetX % 4 || rand.nextInt(24) % 4 != data.regionData.offsetZ % 4) {
                    good = false;
                    break;
                }
            }

            if (!good) continue;
            System.out.println("Found lower bits " + lowerBits);

            for (long upperBits = 0; upperBits < 1L << (48 - 19); upperBits++) {
                long seed = (upperBits << 19) | lowerBits;
                boolean good2 = true;

                for (Data data : dataList) {
                    rand.setRegionSeed(seed, data.regionData.regionX, data.regionData.regionZ, data.salt, version);

                    if (rand.nextInt(24) != data.regionData.offsetX || rand.nextInt(24) != data.regionData.offsetZ) {
                        good2 = false;
                        break;
                    }
                }

                if (good2) System.out.println("Found world seed " + seed);
            }
        }
    }


    public static class Data {
        public RegionStructure.Data<?> regionData;
        public int salt;

        public Data(RegionStructure<?, ?> structure, int chunkX, int chunkZ) {
            this.regionData = structure.at(chunkX, chunkZ);
            this.salt = structure.getSalt();
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
                    "regionData=" + regionData.chunkX+" "+regionData.chunkZ +
                    ", salt=" + salt +
                    '}';
        }

        @Override
        public int hashCode() {
            return Objects.hash(regionData, salt);
        }

        public RegionStructure.Data<?> getRegionData() {
            return regionData;
        }

        public void setRegionData(RegionStructure.Data<?> regionData) {
            this.regionData = regionData;
        }
    }
}
