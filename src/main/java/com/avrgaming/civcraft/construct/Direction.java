package com.avrgaming.civcraft.construct;

import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;
import org.bukkit.Location;

public enum Direction {
    none, east, south, west, north;

    public static Direction rotateDirection(Direction dir) {
        switch (dir) {
            case north:
                return east;
            case east:
                return south;
            case south:
                return west;
            case west:
                return north;
        }
        return none;
    }

    public static Direction invertDirection(Direction dir) {
        switch (dir) {
            case north:
                return south;
            case east:
                return west;
            case south:
                return north;
            case west:
                return east;
        }
        return none;
    }

    public static Direction derotateDirection(Direction dir) {
        switch (dir) {
            case north:
                return west;
            case east:
                return north;
            case south:
                return east;
            case west:
                return south;
        }
        return none;
    }

    public static Direction concatate(Direction dir1, Direction dir2) {
        switch (dir1) {
            case north:
                return dir2;
            case east:
                return rotateDirection(dir2);
            case south:
                return invertDirection(dir2);
            case west:
                return derotateDirection(dir2);
        }
        return none;
    }

    public static Direction newDirection(Location loc) {
        double yaw = loc.getYaw();
        if (yaw < 0) yaw += 360.0;
        yaw = yaw % 360;

        if (0 <= yaw && yaw < 45)
            return south;
        else if (45 <= yaw && yaw < 135)
            return west;
        else if (135 <= yaw && yaw < 225)
            return north;
        else if (225 <= yaw && yaw < 315)
            return east;
        else if (315 <= yaw && yaw < 360)
            return south;
        else
            return north;
    }

    public static Direction newDirection(String filepath) {
        if (filepath.contains("_east")) return east;
        if (filepath.contains("_south")) return south;
        if (filepath.contains("_west")) return west;
        if (filepath.contains("_north")) return north;
        return none;
    }

    public static Direction newDirection(SimpleBlock sb) {
        return withTypeAndData(sb.getType(), sb.getData());
    }

    public static Direction withTypeAndData(int type, byte data) {
        if (type == 68 || type == 54) return withChestData(data);
        if (type == 63) return withSignData(data);
        if (type == 64 || type == 71 || type == 193 || type == 194 || type == 195 || type == 196 || type == 197) return withDoorData(data);
        return none;
    }

    private static Direction withSignData(byte data) {
        switch (data) {
            case 0:
            case 1:
            case 2:
            case 14:
            case 15:
                return north;
            case 3:
            case 4:
            case 5:
                return east;
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
                return south;
            case 11:
            case 12:
            case 13:
                return west;
        }
        return none;
    }

    private static Direction withChestData(byte data) {
        switch (data) {
            case 0:
            case 1:
            case 2:
            case 6:
            case 7:
            case 8:
            case 13:
            case 14:
                return south;
            case 3:
            case 10:
            case 15:
                return north;
            case 4:
            case 11:
                return east;
            case 5:
            case 12:
                return west;
        }
        return none;
    }

    private static Direction withDoorData(byte data) {
        switch (data) {
            case 0:
                return east;
            case 1:
                return south;
            case 2:
                return west;
            case 3:
                return north;
        }
        return none;
    }

    public static byte convertSignDataToChestData(byte data) {
        return Direction.withSignData(data).getChestData();
    }

    public byte getSignData() {
        switch (this) {
            case north:
                return 0;
            case east:
                return 4;
            case south:
                return 8;
            case west:
                return 12;
        }
        return 0x0;
    }

    public byte getWallSignData() {
        switch (this) {
            case north:
                return 3;
            case east:
                return 4;
            case south:
                return 2;
            case west:
                return 5;
        }
        return 0x0;
    }

    public byte getChestData() {
        switch (this) {
            case north:
                return 3;
            case east:
                return 4;
            case south:
                return 2;
            case west:
                return 5;
        }
        return 0x0;
    }

    public byte getDoorData() {
        switch (this) {
            case north:
                return 3;
            case east:
                return 0;
            case south:
                return 1;
            case west:
                return 2;
        }
        return 0x0;
    }

    public BlockCoord convertBlockCoord(BlockCoord bc, int size_x, int size_z) {
        switch (this) {
            case north:
                return bc.getRelative(1 - size_x, 0, 1 - size_z);
            case east:
                return bc.getRelative(0, 0, 1 - size_z);
            case south:
                return bc;
            case west:
                return bc.getRelative(1 - size_x, 0, 0);
        }
        return bc;
    }

}
