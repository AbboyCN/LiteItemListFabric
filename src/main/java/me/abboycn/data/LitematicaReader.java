package me.abboycn.data;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.abboycn.LiteItemListFabric;
import me.abboycn.task.TaskItem;
import me.abboycn.task.TaskItemList;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class LitematicaReader {
    public static final Path SYNCMATICA_PATH = Paths.get("syncmatics/");
    public static final BiMap<String,String> fileNameSuggestionName = HashBiMap.create();

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
            rootNbt = NbtIo.readCompressed(litematicFile.toPath(),NbtSizeTracker.ofUnlimitedBytes());
            if(rootNbt == null) {
                throw new IOException("can't read .litematic file, rootNbt is null.");
            }
        }

        NbtCompound metadata = rootNbt.getCompound("Metadata");
        taskItemList.setName(metadata.getString("Name"));
        NbtCompound regions = rootNbt.getCompound("Regions");
        Map<Identifier, Integer> blockCountMap = new HashMap<>();

        for (String regionKey : regions.getKeys()) {
            NbtCompound region = regions.getCompound(regionKey);
            parseRegion(region, blockCountMap);
        }
        for (Map.Entry<Identifier, Integer> entry : blockCountMap.entrySet()) {
            Identifier blockId = entry.getKey();
            int count = entry.getValue();
            Item item = Registries.BLOCK.get(blockId).asItem();
            if (item != Items.AIR) {
                taskItemList.addTaskItem(new TaskItem(item, count));
            }
        }
        return taskItemList;
    }

    private static void parseRegion(NbtCompound region, Map<Identifier, Integer> blockCountMap) {
        int sizeX = region.getCompound("Size").getInt("x");
        int sizeY = region.getCompound("Size").getInt("y");
        int sizeZ = region.getCompound("Size").getInt("z");
        NbtList paletteList = region.getList("BlockStatePalette", NbtElement.COMPOUND_TYPE);
        List<Identifier> palette = new ArrayList<>();
        for (int i = 0; i < paletteList.size(); i++) {
            NbtCompound blockState = paletteList.getCompound(i);
            String blockName = blockState.getString("Name");
            palette.add(Identifier.of(blockName));
        }
        long[] blockStates = region.getLongArray("BlockStates");
        int bitsPerEntry = calculateBitsPerEntry(palette.size());
        int totalBlocks = sizeX * sizeY * sizeZ;
        for (int index = 0; index < totalBlocks; index++) {
            int paletteIndex = getPaletteIndex(blockStates, index, bitsPerEntry);
            if (paletteIndex == 0) continue;
            Identifier blockId = palette.get(paletteIndex);
            blockCountMap.put(blockId, blockCountMap.getOrDefault(blockId, 0) + 1);
        }
    }

    private static int calculateBitsPerEntry(int paletteSize) {
        if (paletteSize <= 1) return 1;
        return 32 - Integer.numberOfLeadingZeros(paletteSize - 1);
    }

    private static int getPaletteIndex(long[] blockStates, int index, int bitsPerEntry) {
        long bitOffset = (long) index * bitsPerEntry;
        int longIndex = (int) (bitOffset / 64);
        int bitIndex = (int) (bitOffset % 64);
        long value;
        if (bitIndex + bitsPerEntry <= 64) {
            value = (blockStates[longIndex] >>> bitIndex) & ((1L << bitsPerEntry) - 1);
        } else {
            int firstPartBits = 64 - bitIndex;
            long firstPart = blockStates[longIndex] >>> bitIndex;
            long secondPart = blockStates[longIndex + 1] & ((1L << (bitsPerEntry - firstPartBits)) - 1);
            value = (secondPart << firstPartBits) | firstPart;
        }
        return (int) value;
    }

    private static void traverseDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            LiteItemListFabric.LOGGER.warn("无法读取目录内容: {}", dir.getAbsolutePath());
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                traverseDirectory(file);
            }
            else if (file.getName().toLowerCase().endsWith(".litematic")) {
                try {
                    Path path = file.toPath();
                    String pathStr = path.toString();
                    NbtCompound nbtCompound = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
                    if(nbtCompound == null) {
                        throw new IOException("can't read .litematic file: " + file.getAbsolutePath());
                    }
                    fileNameSuggestionName.put(pathStr, String.format("%s(%s)", nbtCompound.getCompound("Metadata").getString("Name"),file.getName()));
                } catch (Exception e) {
                    LiteItemListFabric.LOGGER.error("can't read .litematic file: {}", file.getAbsolutePath(), e);
                }
            }
        }
    }
}