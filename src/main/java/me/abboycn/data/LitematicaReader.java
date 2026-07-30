// me/abboycn/data/LitematicaReader.java
package me.abboycn.data;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.abboycn.LiteItemListFabric;
import me.abboycn.data.nbtprocess.BlockNbtProcessable;
import me.abboycn.data.nbtprocess.EntityNbtProcessable;
import me.abboycn.data.nbtprocess.processors.*;
import me.abboycn.task.TaskItem;
import me.abboycn.task.TaskItemList;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.Math.abs;

public class LitematicaReader {
    public static final Path SYNCMATICA_PATH = Paths.get("syncmatics/");
    public static final BiMap<String, String> fileNameSuggestionName = HashBiMap.create();

    // 方块Nbt处理器
    private static final List<BlockNbtProcessable> BLOCK_PROCESSORS = new ArrayList<>();

    public static void registerBlockProcessor(BlockNbtProcessable processor) {
        BLOCK_PROCESSORS.add(processor);
        LiteItemListFabric.LOGGER.info("Registered block processor: {}", processor.getName());
    }

    public static void registerBlockProcessors(BlockNbtProcessable... processors) {
        for (BlockNbtProcessable processor : processors) {
            registerBlockProcessor(processor);
        }
    }

    public static List<BlockNbtProcessable> getBlockProcessors() {
        return Collections.unmodifiableList(BLOCK_PROCESSORS);
    }

    public static void clearBlockProcessors() {
        BLOCK_PROCESSORS.clear();
        LiteItemListFabric.LOGGER.info("Cleared all block processors");
    }

    // 实体Nbt处理器
    private static final List<EntityNbtProcessable> ENTITY_PROCESSORS = new ArrayList<>();

    public static void registerEntityProcessor(EntityNbtProcessable processor) {
        ENTITY_PROCESSORS.add(processor);
        LiteItemListFabric.LOGGER.info("Registered entity processor: {}", processor.getName());
    }

    public static void registerEntityProcessors(EntityNbtProcessable... processors) {
        for (EntityNbtProcessable processor : processors) {
            registerEntityProcessor(processor);
        }
    }

    public static List<EntityNbtProcessable> getEntityProcessors() {
        return Collections.unmodifiableList(ENTITY_PROCESSORS);
    }

    public static void clearEntityProcessors() {
        ENTITY_PROCESSORS.clear();
        LiteItemListFabric.LOGGER.info("Cleared all entity processors");
    }

    private static void processBlock(BlockStateInfo blockInfo, NbtCompound tileEntity, Map<Item, Integer> itemCountMap) {
        Block block = blockInfo.block;

        if("true".equals(blockInfo.properties.getOrDefault("waterlogged", "false"))) {
            addItem(itemCountMap, Items.WATER_BUCKET, 1);
        }

        for (BlockNbtProcessable processor : BLOCK_PROCESSORS) {
            if (processor.supports(block)) {
                if (processor.process(blockInfo, tileEntity, itemCountMap)) {
                    return;         // 处理成功，不再尝试其他处理器
                }
            }
        }

        Item item = block.asItem();
        if (item != null && item != Items.AIR) {
            addItem(itemCountMap, item, 1);
        }
    }

    private static void processEntity(NbtCompound entityNbt, Map<Item, Integer> itemCountMap) {
        String entityId = entityNbt.getString("id");
        if (entityId == null || entityId.isEmpty()) return;

        for (EntityNbtProcessable processor : ENTITY_PROCESSORS) {
            if (processor.supports(entityId)) {
                if (processor.process(entityNbt, itemCountMap)) {
                    return;         // 处理成功，不再尝试其他处理器
                }
            }
        }
    }

    public static boolean addItem(Map<Item, Integer> map, Item item, int amount) {
        if (item == null || item == Items.AIR || amount <= 0) return false;
        map.put(item, map.getOrDefault(item, 0) + amount);
        return true;
    }

    // .litematic文件解析
    public static void refreshFileList() {
        fileNameSuggestionName.clear();
        File syncDir = SYNCMATICA_PATH.toFile();
        if (!syncDir.exists()) {
            LiteItemListFabric.LOGGER.warn("无法找到syncmatics目录: {}", syncDir.getAbsolutePath());
            return;
        }
        if (!syncDir.isDirectory()) {
            LiteItemListFabric.LOGGER.warn("syncmatics路径不是目录: {}", syncDir.getAbsolutePath());
            return;
        }
        traverseDirectory(syncDir);
    }

    public static List<String> getLitematicaFileNames() {
        return fileNameSuggestionName.keySet().stream().toList();
    }

    public static String getFileName(String suggestionName) {
        return fileNameSuggestionName.inverse().get(suggestionName);
    }

    public static String getSuggestionName(String fileName) {
        return fileNameSuggestionName.get(fileName);
    }

    public static TaskItemList parseLitematicaFile(File litematicFile) throws IOException {
        TaskItemList taskItemList = new TaskItemList(litematicFile.getAbsolutePath());
        NbtCompound rootNbt;
        try (FileInputStream fis = new FileInputStream(litematicFile)) {
            rootNbt = NbtIo.readCompressed(litematicFile.toPath(), NbtSizeTracker.ofUnlimitedBytes());
            if (rootNbt == null) {
                throw new IOException("can't read .litematic file, rootNbt is null.");
            }
        }

        NbtCompound metadata = rootNbt.getCompound("Metadata");
        taskItemList.setName(metadata.getString("Name"));
        NbtCompound regions = rootNbt.getCompound("Regions");
        Map<Item, Integer> itemCountMap = new HashMap<>();

        for (String regionKey : regions.getKeys()) {
            NbtCompound region = regions.getCompound(regionKey);
            parseRegion(region, itemCountMap);
        }

        for (Map.Entry<Item, Integer> entry : itemCountMap.entrySet()) {
            if (entry.getValue() > 0) {
                taskItemList.addTaskItem(new TaskItem(entry.getKey(), entry.getValue()));
            }
        }

        taskItemList.setTaskItems(taskItemList.getTaskItems().stream()
                .sorted(Comparator.comparingInt(TaskItem::getAmount).reversed())
                .toList());
        return taskItemList;
    }

    private static void parseRegion(NbtCompound region, Map<Item, Integer> itemCountMap) {
        NbtCompound sizeNbt = region.getCompound("Size");
        int sizeX = abs(sizeNbt.getInt("x"));
        int sizeY = abs(sizeNbt.getInt("y"));
        int sizeZ = abs(sizeNbt.getInt("z"));

        if (sizeX == 0 || sizeY == 0 || sizeZ == 0) return;

        NbtList paletteList = region.getList("BlockStatePalette", NbtElement.COMPOUND_TYPE);
        List<BlockStateInfo> palette = new ArrayList<>();

        for (int i = 0; i < paletteList.size(); i++) {
            NbtCompound blockState = paletteList.getCompound(i);
            palette.add(parseBlockState(blockState));
        }

        long[] blockStates = region.getLongArray("BlockStates");
        int bitsPerEntry = calculateBitsPerEntry(palette.size());
        int totalBlocks = sizeX * sizeY * sizeZ;

        Map<BlockPos, NbtCompound> tileEntities = new HashMap<>();
        NbtList tileEntitiesList = region.getList("TileEntities", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < tileEntitiesList.size(); i++) {
            NbtCompound te = tileEntitiesList.getCompound(i);
            int x = te.getInt("x");
            int y = te.getInt("y");
            int z = te.getInt("z");
            tileEntities.put(new BlockPos(x, y, z), te);
        }

        for (int index = 0; index < totalBlocks; index++) {
            int paletteIndex = getPaletteIndex(blockStates, index, bitsPerEntry);
            if (paletteIndex < 0 || paletteIndex >= palette.size()) continue;

            BlockStateInfo blockInfo = palette.get(paletteIndex);
            if (blockInfo.block == Blocks.AIR) continue;

            int x = index % sizeX;
            int y = (index / sizeX) % sizeY;
            int z = index / (sizeX * sizeY);
            BlockPos pos = new BlockPos(x, y, z);

            NbtCompound tileEntity = tileEntities.get(pos);
            processBlock(blockInfo, tileEntity, itemCountMap);
        }

        NbtList entitiesList = region.getList("Entities", NbtElement.COMPOUND_TYPE);
        parseEntities(entitiesList, itemCountMap);
    }

    private static void parseEntities(NbtList entitiesList, Map<Item, Integer> itemCountMap) {
        if (entitiesList == null) return;

        for (int i = 0; i < entitiesList.size(); i++) {
            NbtCompound entity = entitiesList.getCompound(i);
            processEntity(entity, itemCountMap);
        }
    }

    private static BlockStateInfo parseBlockState(NbtCompound blockStateNbt) {
        String blockName = null;

        if (blockStateNbt.contains("Name", NbtElement.STRING_TYPE)) {
            blockName = blockStateNbt.getString("Name");
        } else if (blockStateNbt.contains("id", NbtElement.STRING_TYPE)) {
            blockName = blockStateNbt.getString("id");
        }

        if (blockName == null || blockName.isEmpty()) {
            return new BlockStateInfo(Blocks.AIR, new HashMap<>());
        }

        if (blockName.contains("[")) {
            blockName = blockName.substring(0, blockName.indexOf("["));
        }

        Block block = Registries.BLOCK.get(Identifier.of(blockName));

        Map<String, String> properties = new HashMap<>();
        NbtCompound propsNbt = blockStateNbt.getCompound("Properties");
        if (propsNbt != null) {
            for (String key : propsNbt.getKeys()) {
                properties.put(key, propsNbt.getString(key));
            }
        }

        return new BlockStateInfo(block, properties);
    }

    public record BlockStateInfo(Block block, Map<String, String> properties) {}

    private static int calculateBitsPerEntry(int paletteSize) {
        if (paletteSize <= 1) return 1;
        return 32 - Integer.numberOfLeadingZeros(paletteSize - 1);
    }

    private static int getPaletteIndex(long[] blockStates, int index, int bitsPerEntry) {
        if (bitsPerEntry <= 0 || bitsPerEntry > 32) return 0;

        long bitOffset = (long) index * bitsPerEntry;
        int longIndex = (int) (bitOffset / 64);
        int bitIndex = (int) (bitOffset % 64);

        if (longIndex >= blockStates.length) return 0;

        long value;
        if (bitIndex + bitsPerEntry <= 64) {
            long mask = (1L << bitsPerEntry) - 1;
            value = (blockStates[longIndex] >>> bitIndex) & mask;
        } else {
            int firstPartBits = 64 - bitIndex;
            int secondPartBits = bitsPerEntry - firstPartBits;
            long firstPart = blockStates[longIndex] >>> bitIndex;
            long mask = (1L << secondPartBits) - 1;
            long secondPart = (longIndex + 1 < blockStates.length) ?
                    (blockStates[longIndex + 1] & mask) : 0;
            value = (secondPart << firstPartBits) | firstPart;
        }
        return (int) value;
    }

    private static void traverseDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            LiteItemListFabric.LOGGER.warn("failed to read folder: {}", dir.getAbsolutePath());
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                traverseDirectory(file);
            } else if (file.getName().toLowerCase().endsWith(".litematic")) {
                try {
                    Path path = file.toPath();
                    String pathStr = path.toString();
                    NbtCompound nbtCompound = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
                    if (nbtCompound == null) {
                        throw new IOException("can not read litematic file: " + file.getAbsolutePath());
                    }
                    fileNameSuggestionName.put(pathStr, String.format("%s(%s)",
                            nbtCompound.getCompound("Metadata").getString("Name"), file.getName()));
                } catch (Exception e) {
                    LiteItemListFabric.LOGGER.error("can not read litematic file: {}", file.getAbsolutePath(), e);
                }
            }
        }
    }
}