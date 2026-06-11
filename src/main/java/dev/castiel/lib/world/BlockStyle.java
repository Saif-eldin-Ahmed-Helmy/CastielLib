package dev.castiel.lib.world;

import dev.castiel.lib.items.ItemStacks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.configuration.ConfigurationSection;

public final class BlockStyle {
    private final String material;

    public BlockStyle(String material) {
        this.material = material == null || material.trim().isEmpty() ? "CHEST" : material;
    }

    public static BlockStyle from(ConfigurationSection section, String defaultMaterial) {
        if (section == null) {
            return new BlockStyle(defaultMaterial);
        }
        return new BlockStyle(section.getString("Material", section.getString("material", defaultMaterial)));
    }

    public void apply(Location location) {
        apply(location, null);
    }

    public void apply(Location location, BlockFace facing) {
        if (location == null) {
            return;
        }
        apply(location.getBlock(), facing);
    }

    public void apply(Block block) {
        apply(block, null);
    }

    public void apply(Block block, BlockFace facing) {
        if (block == null) {
            return;
        }
        if (ItemStacks.looksLikeTexture(material)) {
            applyHead(block, facing);
            return;
        }
        block.setType(ItemStacks.resolveMaterial(material, Material.CHEST), false);
        orient(block, facing);
    }

    public String material() {
        return material;
    }

    private void applyHead(Block block, BlockFace facing) {
        block.setType(ItemStacks.resolveMaterial("PLAYER_HEAD", Material.PLAYER_HEAD), false);
        orient(block, facing);
        BlockState state = block.getState();
        if (!(state instanceof Skull)) {
            return;
        }
        Skull skull = (Skull) state;
        if (ItemStacks.applySkullTexture(skull, material)) {
            skull.update(true, false);
            orient(block, facing);
        }
    }

    private void orient(Block block, BlockFace facing) {
        BlockFace cardinal = cardinal(facing);
        if (cardinal == null) {
            return;
        }
        BlockData data = block.getBlockData();
        if (data instanceof Directional) {
            Directional directional = (Directional) data;
            BlockFace directionalFace = cardinalFour(cardinal);
            if (directionalFace != null && directional.getFaces().contains(directionalFace)) {
                directional.setFacing(directionalFace);
                block.setBlockData(directional, false);
            }
            return;
        }
        if (data instanceof Rotatable) {
            Rotatable rotatable = (Rotatable) data;
            rotatable.setRotation(cardinal);
            block.setBlockData(rotatable, false);
        }
    }

    private BlockFace cardinal(BlockFace facing) {
        if (facing == null) {
            return null;
        }
        switch (facing) {
            case NORTH:
            case EAST:
            case SOUTH:
            case WEST:
                return facing;
            case NORTH_EAST:
            case EAST_NORTH_EAST:
            case NORTH_NORTH_EAST:
                return BlockFace.NORTH_EAST;
            case SOUTH_EAST:
            case EAST_SOUTH_EAST:
            case SOUTH_SOUTH_EAST:
                return BlockFace.SOUTH_EAST;
            case SOUTH_WEST:
            case WEST_SOUTH_WEST:
            case SOUTH_SOUTH_WEST:
                return BlockFace.SOUTH_WEST;
            case NORTH_WEST:
            case WEST_NORTH_WEST:
            case NORTH_NORTH_WEST:
                return BlockFace.NORTH_WEST;
            default:
                return null;
        }
    }

    private BlockFace cardinalFour(BlockFace facing) {
        if (facing == null) {
            return null;
        }
        int x = facing.getModX();
        int z = facing.getModZ();
        if (Math.abs(x) > Math.abs(z)) {
            return x > 0 ? BlockFace.EAST : BlockFace.WEST;
        }
        if (z != 0) {
            return z > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
        if (x != 0) {
            return x > 0 ? BlockFace.EAST : BlockFace.WEST;
        }
        return null;
    }
}
