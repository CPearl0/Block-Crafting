package com.cpearl.blockcrafting.multiblock;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.tag.CompoundTag;
import org.antlr.v4.runtime.misc.MultiMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class MultiblockStructure {
    public static final MultiMap<Block, MultiblockStructure> STRUCTURES = new MultiMap<>();

    public static void addStructure(MultiblockStructure structure) {
        var block = structure.centerBlock;
        var list = STRUCTURES.get(block);
        if (list == null) {
            STRUCTURES.map(block, structure);
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).name.equals(structure.name)) {
                list.set(i, structure);
                return;
            }
        }
        list.add(structure);
    }

    private final Component name;
    private final List<Tuple<Vec3i, Predicate<Block>>> blocks;
    private final Block centerBlock;
    private final Predicate<Item> craftingItem;
    private final List<ItemStack> results;

    public MultiblockStructure(Component name, List<Tuple<Vec3i, Predicate<Block>>> blocks, Block centerBlock, Predicate<Item> craftingItem, List<ItemStack> results) {
        this.name = name;
        this.blocks = blocks;
        this.centerBlock = centerBlock;
        this.craftingItem = craftingItem;
        this.results = results;
    }

    public Component getName() {
        return name;
    }

    public Block getCenterBlock() {
        return centerBlock;
    }

    public Predicate<Item> getCraftingItem() {
        return craftingItem;
    }

    public List<ItemStack> getResults() {
        return results;
    }

    public void addBlock(Vec3i pos, Predicate<Block> block) {
        blocks.add(new Tuple<>(pos, block));
    }

    public static Vec3i rotateClockwise(Vec3i vec) {
        return new Vec3i(-vec.getZ(), vec.getY(), vec.getX());
    }
    public int finishedDirection(ServerLevel level, BlockPos pos) {
        var res = 0;
        for (var blockPosPredicate : blocks) {
            if (!blockPosPredicate.getB().test(
                    level.getBlockState(pos.offset(
                            blockPosPredicate.getA()
                    )).getBlock())) {
                res = -1;
                break;
            }
        }
        if (res >= 0)
            return res;

        res = 1;
        for (var blockPosPredicate : blocks) {
            if (!blockPosPredicate.getB().test(
                    level.getBlockState(pos.offset(
                            rotateClockwise(blockPosPredicate.getA())
                    )).getBlock())) {
                res = -1;
                break;
            }
        }
        if (res >= 0)
            return res;

        res = 2;
        for (var blockPosPredicate : blocks) {
            if (!blockPosPredicate.getB().test(
                    level.getBlockState(pos.offset(
                            rotateClockwise(rotateClockwise(blockPosPredicate.getA()))
                    )).getBlock())) {
                res = -1;
                break;
            }
        }
        if (res >= 0)
            return res;

        res = 3;
        for (var blockPosPredicate : blocks) {
            if (!blockPosPredicate.getB().test(
                    level.getBlockState(pos.offset(
                            rotateClockwise(rotateClockwise(rotateClockwise(blockPosPredicate.getA())))
                    )).getBlock())) {
                res = -1;
                break;
            }
        }

        return res;
    }

    public boolean finish(ServerLevel level, BlockPos pos, int direction) {
        if (direction < 0)
            return false;
        for (var blockPosPredicate : blocks) {
            var rpos = blockPosPredicate.getA();
            for (int i = 0; i < direction; i++)
                rpos = rotateClockwise(rpos);
            level.destroyBlock(pos.offset(rpos), false);
        }
        for (var result : results) {
            int i = 1;
            for (; i * result.getMaxStackSize() <= result.getCount(); i++)
                level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(result.getItem(), result.getMaxStackSize())));
            if ((i - 1) * result.getMaxStackSize() < result.getCount())
                level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(result.getItem(), result.getCount() - (i - 1) * result.getMaxStackSize())));
        }
        return true;
    }

    public static class StructureBuilder {
        private final Component name;
        private final List<List<String>> pattern = new ArrayList<>();
        private char center;
        private Block centerBlock;
        private final Map<Character, Predicate<Block>> dict = new HashMap<>();

        private Predicate<Item> craftingItem;
        private final List<ItemStack> results = new ArrayList<>();

        public StructureBuilder(Component name) {
            this.name = name;
        }

        public static StructureBuilder create(Component name) {
            return new StructureBuilder(name);
        }

        public StructureBuilder pattern(String ...line) {
            this.pattern.add(List.of(line));
            return this;
        }

        public StructureBuilder center(char ch, Block block) {
            this.center = ch;
            this.centerBlock = block;
            this.dict.put(ch, Predicate.isEqual(block));
            return this;
        }

        public StructureBuilder whereCond(char ch, Predicate<Block> block) {
            this.dict.put(ch, block);
            return this;
        }

        public StructureBuilder where(char ch, Block block) {
            return whereCond(ch, Predicate.isEqual(block));
        }

        public StructureBuilder whereTag(char ch, ResourceLocation tag) {
            return whereCond(ch,
                    ForgeRegistries.BLOCKS.tags().getTag(TagKey.create(Registries.BLOCK, tag))::contains);
        }

        public StructureBuilder craftingItemCond(Predicate<Item> item) {
            this.craftingItem = item;
            return this;
        }

        public StructureBuilder craftingItem(Item item) {
            return craftingItemCond(Predicate.isEqual(item));
        }

        public StructureBuilder craftingItemTag(ResourceLocation tag) {
            return craftingItemCond(
                    ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, tag))::contains);
        }

        public StructureBuilder result(ItemStack ...item) {
            this.results.addAll(List.of(item));
            return this;
        }

        public MultiblockStructure build() {
            List<Tuple<Vec3i, Predicate<Block>>> blocks = new ArrayList<>();
            Vec3i centerPos = null;
            for (int i = 0; i < pattern.size(); i++) {
                var layer = pattern.get(i);
                for (int j = 0; j < layer.size(); j++) {
                    var line = layer.get(j);
                    for (int k = 0; k < line.length(); k++) {
                        var pos = new Vec3i(i, j, -k);
                        var ch = line.charAt(k);
                        var block = dict.get(ch);
                        if (ch == center)
                            centerPos = new Vec3i(i, j, -k);
                        blocks.add(new Tuple<>(pos, block));
                    }
                }
            }
            if (centerPos == null) {
                throw new RuntimeException("No center in multiblock!");
            }
            var centerX = centerPos.getX();
            var centerY = centerPos.getY();
            var centerZ = centerPos.getZ();
            blocks.forEach(vec3iPredicateTuple -> {
                var pos = vec3iPredicateTuple.getA();
                vec3iPredicateTuple.setA(pos.offset(-centerX, -centerY, -centerZ));
            });
            return new MultiblockStructure(name, blocks, centerBlock, craftingItem, results);
        }
    }

    public static class StructureFileBuilder {
        private final Component name;
        private File file;
        private Block centerBlock;

        private Predicate<Item> craftingItem;
        private final List<ItemStack> results = new ArrayList<>();

        public StructureFileBuilder(Component name) {
            this.name = name;
        }

        public static StructureFileBuilder create(Component name) {
            return new StructureFileBuilder(name);
        }

        public StructureFileBuilder file(String filename) {
            this.file = new File(filename);
            return this;
        }

        public StructureFileBuilder center(Block block) {
            this.centerBlock = block;
            return this;
        }

        public StructureFileBuilder craftingItemCond(Predicate<Item> item) {
            this.craftingItem = item;
            return this;
        }

        public StructureFileBuilder craftingItem(Item item) {
            return craftingItemCond(Predicate.isEqual(item));
        }

        public StructureFileBuilder craftingItemTag(ResourceLocation tag) {
            return craftingItemCond(
                    ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, tag))::contains);
        }

        public StructureFileBuilder result(ItemStack ...item) {
            this.results.addAll(List.of(item));
            return this;
        }

        public MultiblockStructure build() throws IOException, CommandSyntaxException {
            List<Tuple<Vec3i, Predicate<Block>>> blocks = new ArrayList<>();
            Vec3i centerPos = null;

            // Parse the file
            if (!(NBTUtil.read(file).getTag() instanceof CompoundTag tag))
                return null;
            var blockPosList = tag.getListTag("blocks").asCompoundTagList();
            var palette = tag.getListTag("palette").asCompoundTagList();
            Block[] blockList = new Block[palette.size()];
            for (int i = 0; i < palette.size(); i++) {
                blockList[i] = ForgeRegistries.BLOCKS.getValue(
                        ResourceLocation.tryParse(palette.get(i).getString("Name")));
            }
            for (int i = 0; i < blockPosList.size(); i++) {
                var blockTag = blockPosList.get(i);
                var pos = blockTag.getListTag("pos").asIntTagList();
                var state = blockTag.getInt("state");
                var block = blockList[state];
                if (block == Blocks.AIR)
                    continue;
                var posVec = new Vec3i(pos.get(0).asInt(), pos.get(1).asInt(), pos.get(2).asInt());
                if (block == centerBlock)
                    centerPos = posVec;
                blocks.add(new Tuple<>(posVec, Predicate.isEqual(block)));
            }

            if (centerPos == null) {
                throw new RuntimeException("No center in multiblock!");
            }
            var centerX = centerPos.getX();
            var centerY = centerPos.getY();
            var centerZ = centerPos.getZ();
            blocks.forEach(vec3iPredicateTuple -> {
                var pos = vec3iPredicateTuple.getA();
                vec3iPredicateTuple.setA(pos.offset(-centerX, -centerY, -centerZ));
            });
            return new MultiblockStructure(name, blocks, centerBlock, craftingItem, results);
        }
    }
}
