
package lifting;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.rand.seed.StructureSeed;
import com.seedfinding.mccore.rand.seed.WorldSeed;
import com.seedfinding.mccore.util.data.Pair;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.util.pos.RPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class StructureLiftingSalt {
    // set to null for random
    public static final Random REPRODUCIBLE_RANDOM = new Random(42);
    public static final MCVersion VERSION = MCVersion.v1_17;
    public static final BuriedTreasure BURIED_TREASURE= new BuriedTreasure(VERSION);
    public static final DesertPyramid DESERT_PYRAMID = new DesertPyramid(VERSION);
    public static final DesertPyramid DESERT_PYRAMID_MODIFIED = new DesertPyramid(VERSION) {
        @Override
        public int getSalt() {
            return super.getSalt() + 1;
        }
    };
    public static final SwampHut SWAMP_HUT = new SwampHut(VERSION);
    public static final SwampHut SWAMP_HUT_MODIFIED = new SwampHut(VERSION) {
        @Override
        public int getSalt() {
            return super.getSalt() + 2;
        }
    };
    public static final Village VILLAGE = new Village(VERSION);
    public static final Village VILLAGE_MODIFIED = new Village(VERSION) {
        @Override
        public int getSalt() {
            return super.getSalt() + 3;
        }
    };
    public static final Igloo IGLOO = new Igloo(VERSION);
    public static final Igloo IGLOO_MODIFIED = new Igloo(VERSION) {
        @Override
        public int getSalt() {
            return super.getSalt() + 4;
        }
    };
    public static final JunglePyramid JUNGLE_TEMPLE = new JunglePyramid(VERSION);
    public static final JunglePyramid JUNGLE_TEMPLE_MODIFIED = new JunglePyramid(VERSION) {
        @Override
        public int getSalt() {
            return super.getSalt() + 5;
        }
    };
    public static final PillagerOutpost PILLAGER_OUTPOST = new PillagerOutpost(VERSION);
    public static final PillagerOutpost PILLAGER_OUTPOST_MODIFIED = new PillagerOutpost(VERSION) {
        @Override
        public int getSalt() {
            return super.getSalt() + 6;
        }
    };
    public static final Shipwreck SHIPWRECK = new Shipwreck(VERSION);
    public static final Shipwreck SHIPWRECK_MODIFIED = new Shipwreck(VERSION) {
        @Override
        public int getSalt() {
            return super.getSalt() + 7;
        }
    };


    public static void main(String[] args) {
        singleStructureSaltCrackingUsingNonModifiable();
    }

    public static void multipleStructureSaltCracking() {
        // data known from mc source
        int desertOriginalSalt = DESERT_PYRAMID.getSalt();
        int swampOriginalSalt = SWAMP_HUT.getSalt();
        int jungleOriginalSalt = JUNGLE_TEMPLE.getSalt();

        // data that comes from the server
        long structureSeed = 90212;
        long upperBits = 56;
        long worldSeed = StructureSeed.toWorldSeed(structureSeed, upperBits);
        long hashedSeed = WorldSeed.toHash(worldSeed);
        List<Data> desertDataList = new ArrayList<>();
        desertDataList.addAll(generateSaltedData(worldSeed, desertOriginalSalt, DESERT_PYRAMID_MODIFIED, 7));

        List<Data> swampDataList = new ArrayList<>();
        swampDataList.addAll(generateSaltedData(worldSeed, swampOriginalSalt, SWAMP_HUT_MODIFIED, 7));

        List<Data> jungleDataList = new ArrayList<>();
        jungleDataList.addAll(generateSaltedData(worldSeed, jungleOriginalSalt, JUNGLE_TEMPLE_MODIFIED, 7));


        List<Long> desertSeeds = crack(desertDataList);
        System.out.println("Finished cracking desert pyramid " + desertSeeds.size());
        List<Long> swampSeeds = crack(swampDataList);
        System.out.println("Finished cracking swamp hut " + swampSeeds.size());
        List<Long> jungleSeeds = crack(jungleDataList);
        System.out.println("Finished cracking jungle temple " + jungleSeeds.size());

        System.out.println("Finished cracking separately");

        List<List<Pair<RegionStructure<?, ?>, Long>>> validSeeds = reduceSalts(new ArrayList<Pair<RegionStructure<?, ?>, List<Long>>>() {{
            add(new Pair<>(DESERT_PYRAMID, desertSeeds));
            add(new Pair<>(SWAMP_HUT, swampSeeds));
            add(new Pair<>(JUNGLE_TEMPLE, jungleSeeds));
        }});
        /// here you will get a list of all possible
        /// triple which would all be of the form (seed1, seed2, seed3)
        /// with the relation seed1=seed2+offset1=seed3+offset2
        /// so cracking seed1 as structure seed + salt with the hashed seed
        /// will automatically yield the offset1 and offset2 thus yielding all the salts for
        /// each structure, however the cracking of seed1 is the heavy process as shown in singleCracking()
        /// We thus recommend getting the structure seed from a buried treasure.
        System.out.println(validSeeds);
        System.out.println(validSeeds.size());
    }

    public static List<List<Pair<RegionStructure<?, ?>, Long>>> reduceSalts(List<Pair<RegionStructure<?, ?>, List<Long>>> seeds) {
        Pair<RegionStructure<?, ?>, List<Long>> first = seeds.get(0);
        if (first == null) {
            return null;
        }
        List<List<Pair<RegionStructure<?, ?>, Long>>> validSeeds = first.getSecond().stream().map(seed -> new ArrayList<Pair<RegionStructure<?, ?>, Long>>() {{
            add(new Pair<>(first.getFirst(), seed));
        }}).collect(Collectors.toList());
        for (int i = 1; i < seeds.size(); i++) {
            Pair<RegionStructure<?, ?>, List<Long>> next = seeds.get(i);
            // can not happen
            assert next != null;
            validSeeds = reduceSalt(validSeeds, next);
        }
        return validSeeds;
    }

    public static List<List<Pair<RegionStructure<?, ?>, Long>>> reduceSalt(List<List<Pair<RegionStructure<?, ?>, Long>>> validSeeds, Pair<RegionStructure<?, ?>, List<Long>> next) {
        // can not happen
        assert next != null;
        ArrayList<List<Pair<RegionStructure<?, ?>, Long>>> toRemove = new ArrayList<>();
        for (List<Pair<RegionStructure<?, ?>, Long>> validSeed : validSeeds) {
            boolean isValidForAny = false;
            for (long firstSeed : next.getSecond()) {
                boolean isValid = validSeed.stream()
                    .allMatch(secondSeed -> Math.abs(firstSeed - secondSeed.getSecond()) < ((long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE));
                if (isValid) {
                    validSeed.add(new Pair<>(next.getFirst(), firstSeed));
                    isValidForAny = true;
                }
            }
            if (!isValidForAny) {
                toRemove.add(validSeed);
            }
        }
        for (List<Pair<RegionStructure<?, ?>, Long>> validSeed : toRemove) {
            validSeeds.remove(validSeed);
        }
        return validSeeds;
    }

    /// We use single structure lifting and one more source of non modifiable data
    /// Those data can be buried treasure for instance
    /// But could also be trees or dungeon tbh
    @SuppressWarnings("unchecked")
    public static void singleStructureSaltCrackingUsingNonModifiable(){
        // data known from mc source
        int originalSalt = DESERT_PYRAMID.getSalt();

        // data that comes from the server
        long structureSeed = 90212;
        long upperBits = 56;
        long worldSeed = StructureSeed.toWorldSeed(structureSeed, upperBits);
        long hashedSeed = WorldSeed.toHash(worldSeed);
        List<Data> dataList = new ArrayList<>();
        dataList.addAll(generateSaltedData(worldSeed, originalSalt, DESERT_PYRAMID_MODIFIED, 7));
        List<Long> seeds = crack(dataList);

        List<GenericData> nonModifiableDataList = new ArrayList<>();
        nonModifiableDataList.addAll(generateGenericData(worldSeed, BURIED_TREASURE, 7));

        for (Long seed : seeds) {
            System.out.println("Found salted structure seed: " + seed);
            // this will be really slow (2^32*2^16=2^48*(shaOp|biomeOp))
            // You can technically reduce to a smaller range because people use salts close to the original ones, so you could do
            // IntStream.range(originalSalt*-100,originalSalt*100)
            IntStream.range(Integer.MIN_VALUE, Integer.MAX_VALUE).boxed().parallel().forEach(salt -> {
                long structureSeedNonSalted = seed - originalSalt;
                long structureSeedCustomSalt = structureSeedNonSalted + salt;
                boolean isValid=true;
                ChunkRand rand = new ChunkRand();
                for (GenericData data:nonModifiableDataList){
                    if (!BURIED_TREASURE.canStart((RegionStructure.Data<BuriedTreasure>) data.regionData,structureSeedCustomSalt,rand)){
                        isValid=false;
                        break;
                    }
                }
                if (isValid){
                    WorldSeed.fromHash(structureSeedCustomSalt, hashedSeed).forEach(x -> System.out.println("Found world seed by bruteforcing: " + x+ " with salt "+salt));
                }
            });
        }
    }

    /// We use only hashed seed/biome and single structure lifting
    public static void singleStructureSaltCracking() {
        // data known from mc source
        int originalSalt = DESERT_PYRAMID.getSalt();

        // data that comes from the server
        long structureSeed = 90212;
        long upperBits = 56;
        long worldSeed = StructureSeed.toWorldSeed(structureSeed, upperBits);
        long hashedSeed = WorldSeed.toHash(worldSeed);
        List<Data> dataList = new ArrayList<>();
        dataList.addAll(generateSaltedData(worldSeed, originalSalt, DESERT_PYRAMID_MODIFIED, 7));

        List<Long> seeds = crack(dataList);
        for (Long seed : seeds) {
            System.out.println("Found salted structure seed: " + seed);
            // this will be really slow (2^32*2^16=2^48*(shaOp|biomeOp))
            // You can technically reduce to a smaller range because people use salts close to the original ones, so you could do
            // IntStream.range(originalSalt*-100,originalSalt*100)
            IntStream.range(Integer.MIN_VALUE, Integer.MAX_VALUE).boxed().parallel().forEach(salt -> {
                long structureSeedNonSalted = seed - originalSalt;
                long structureSeedCustomSalt = structureSeedNonSalted + salt;
                // we use hashed seed which is fast but if you checked biome operation then it would be super slow
                WorldSeed.fromHash(structureSeedCustomSalt, hashedSeed).forEach(x -> System.out.println("Found world seed by bruteforcing: " + x+ " with salt "+salt));
            });
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
            if (cPos != null) {
                res.add(new Data(structure, cPos));
            }
        }
        return res;
    }

    public static List<GenericData> generateGenericData(long structureSeed, RegionStructure<?,?> structure, int count) {
        List<GenericData> res = new ArrayList<>(count);
        ChunkRand rand = new ChunkRand();
        for (int i = 0; i < count; i++) {
            RPos rpos = getRandomRPos(structure);
            CPos cPos = structure.getInRegion(structureSeed, rpos.getX(), rpos.getZ(), rand);
            // WARNING Pillager outpost can be exploited to check the nextInt(5)!=0 also but they will produce some nulls here
            if (cPos != null) {
                res.add(new GenericData(structure, cPos));
            }
        }
        return res;
    }

    public static List<Data> generateSaltedData(long structureSeed, int originalSalt, UniformStructure<?> structure, int count) {
        List<Data> res = new ArrayList<>(count);
        ChunkRand rand = new ChunkRand();
        for (int i = 0; i < count; i++) {
            RPos rpos = getRandomRPos(structure);
            CPos cPos = structure.getInRegion(structureSeed, rpos.getX(), rpos.getZ(), rand);
            // WARNING Pillager outpost can be exploited to check the nextInt(5)!=0 also but they will produce some nulls here
            if (cPos != null) {
                Data data = new Data(structure, cPos);
                // we set the salt as the correct one, but we generated the data with an unknown salt
                data.salt = originalSalt;
                res.add(data);
            }
        }
        return res;
    }

    public static RPos getRandomRPos(RegionStructure<?,?> structure) {
        Random random = REPRODUCIBLE_RANDOM == null ? new Random() : new Random(REPRODUCIBLE_RANDOM.nextLong());
        return new RPos(random.nextInt(10000) - 5000, random.nextInt(10000) - 5000, structure.getSpacing());
    }

    public static class GenericData{
        public RegionStructure.Data<?> regionData;
        public int salt;
        public RegionStructure<?,?> structure;

        public GenericData(RegionStructure<?,?> structure, int chunkX, int chunkZ) {
            this.regionData = structure.at(chunkX, chunkZ);
            this.salt = structure.getSalt();
            this.structure = structure;
        }
        public GenericData(RegionStructure<?,?> structure, CPos cPos) {
            this(structure, cPos.getX(), cPos.getZ());
        }
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
