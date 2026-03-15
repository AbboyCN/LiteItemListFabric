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

    // 解析 Litematica 文件（.litematic）生成 TaskItemList
    public static TaskItemList parseLitematicaFile(File litematicFile) throws IOException {
        // 1. 初始化返回结果
        TaskItemList taskItemList = new TaskItemList(litematicFile.getAbsolutePath());

        // 2. 读取 NBT 文件
        NbtCompound rootNbt;
        try (FileInputStream fis = new FileInputStream(litematicFile)) {
            rootNbt = NbtIo.readCompressed(litematicFile.toPath(),NbtSizeTracker.ofUnlimitedBytes());
            if(rootNbt == null) {
                throw new IOException("can't read litematica file, rootNbt is null.");
            }
        }

        // 4. 设置 TaskItemList 名称（取自元数据）
        NbtCompound metadata = rootNbt.getCompound("Metadata");
        taskItemList.setName(metadata.getString("Name"));

        // 5. 解析所有 Region 区块
        NbtCompound regions = rootNbt.getCompound("Regions");
        Map<Identifier, Integer> blockCountMap = new HashMap<>();

        // 遍历所有区块（支持多区块）
        for (String regionKey : regions.getKeys()) {
            NbtCompound region = regions.getCompound(regionKey);
            parseRegion(region, blockCountMap);
        }

        // 6. 将统计的方块转换为 TaskItem
        for (Map.Entry<Identifier, Integer> entry : blockCountMap.entrySet()) {
            Identifier blockId = entry.getKey();
            int count = entry.getValue();

            // 从方块 ID 映射到对应的物品 ID（多数方块与物品 ID 一致）
            Item item = Registries.ITEM.get(blockId);
            if (item != Items.AIR) { // 排除空气等无效物品
                taskItemList.addTaskItem(new TaskItem(item, count));
            }
        }

        return taskItemList;
    }

    // 解析单个 Region 区块
    private static void parseRegion(NbtCompound region, Map<Identifier, Integer> blockCountMap) {
        // 1. 获取区块基础信息
        int sizeX = region.getCompound("Size").getInt("x");
        int sizeY = region.getCompound("Size").getInt("y");
        int sizeZ = region.getCompound("Size").getInt("z");

        // 2. 获取方块状态调色板
        NbtList paletteList = region.getList("BlockStatePalette", NbtElement.COMPOUND_TYPE);
        List<Identifier> palette = new ArrayList<>();
        for (int i = 0; i < paletteList.size(); i++) {
            NbtCompound blockState = paletteList.getCompound(i);
            String blockName = blockState.getString("Name");
            palette.add(Identifier.of(blockName));
        }

        // 3. 获取压缩的 BlockStates 长数组
        long[] blockStates = region.getLongArray("BlockStates");

        // 4. 计算每个方块状态占用的位数
        int bitsPerEntry = calculateBitsPerEntry(palette.size());

        // 5. 遍历所有 3D 坐标，解析每个位置的方块
        int totalBlocks = sizeX * sizeY * sizeZ;
        for (int index = 0; index < totalBlocks; index++) {
            // 5.1 从压缩数据中提取调色板索引
            int paletteIndex = getPaletteIndex(blockStates, index, bitsPerEntry);
            if (paletteIndex == 0) continue; // 跳过空气（调色板索引 0 通常是 air）

            // 5.2 统计方块数量
            Identifier blockId = palette.get(paletteIndex);
            blockCountMap.put(blockId, blockCountMap.getOrDefault(blockId, 0) + 1);
        }
    }

    // 计算每个方块状态需要的位数（向上取整的 log2）
    private static int calculateBitsPerEntry(int paletteSize) {
        if (paletteSize <= 1) return 1;
        return 32 - Integer.numberOfLeadingZeros(paletteSize - 1);
    }

    // 从 BlockStates 中提取指定索引的调色板下标
    private static int getPaletteIndex(long[] blockStates, int index, int bitsPerEntry) {
        long bitOffset = (long) index * bitsPerEntry;
        int longIndex = (int) (bitOffset / 64);
        int bitIndex = (int) (bitOffset % 64);

        // 处理跨 long 的情况
        long value;
        if (bitIndex + bitsPerEntry <= 64) {
            // 单个 long 内即可容纳
            value = (blockStates[longIndex] >>> bitIndex) & ((1L << bitsPerEntry) - 1);
        } else {
            // 跨两个 long，需要拼接
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
                        throw new IOException("can't read litematica file: " + file.getAbsolutePath());
                    }
                    fileNameSuggestionName.put(pathStr, String.format("%s(%s)", nbtCompound.getCompound("Metadata").getString("Name"),file.getName()));
                } catch (Exception e) {
                    LiteItemListFabric.LOGGER.error("can't read litematica file: {}", file.getAbsolutePath(), e);
                }
            }
        }
    }
}