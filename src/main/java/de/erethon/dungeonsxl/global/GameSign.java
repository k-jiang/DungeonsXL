/*
 * Copyright (C) 2012-2018 Frank Baumann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.erethon.dungeonsxl.global;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.misc.BlockUtil;
import de.erethon.dungeonsxl.DungeonsXL;
import de.erethon.dungeonsxl.config.DMessage;
import de.erethon.dungeonsxl.dungeon.Dungeon;
import de.erethon.dungeonsxl.game.Game;
import de.erethon.dungeonsxl.player.DGroup;
import de.erethon.dungeonsxl.util.LegacyUtil;
import de.erethon.dungeonsxl.world.DResourceWorld;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Basically a GroupSign, but to form a game of multiple groups.
 *
 * @author Frank Baumann, Milan Albrecht, Daniel Saukel
 */
public class GameSign extends GlobalProtection {

    private Game[] games;
    private Dungeon dungeon;
    private int maxGroupsPerGame;
    private Block startSign;
    private int directionX = 0, directionZ = 0;
    private int verticalSigns;
    private Set<Block> blocks;

    public GameSign(int id, Block startSign, String identifier, int maxGames, int maxGroupsPerGame) {
        super(startSign.getWorld(), id);

        this.startSign = startSign;
        games = new Game[maxGames];
        dungeon = DungeonsXL.getInstance().getDungeons().getByName(identifier);
        if (dungeon == null) {
            DResourceWorld resource = DungeonsXL.getInstance().getDWorlds().getResourceByName(identifier);
            if (resource != null) {
                dungeon = new Dungeon(resource);
            }
        }
        this.maxGroupsPerGame = maxGroupsPerGame;
        verticalSigns = (int) Math.ceil((float) (1 + maxGroupsPerGame) / 4);

        int[] direction = getDirection(this.startSign.getData());
        directionX = direction[0];
        directionZ = direction[1];

        update();
    }

    /**
     * @return the games
     */
    public Game[] getGames() {
        return games;
    }

    /**
     * @param games
     * the games to set
     */
    public void setGames(Game[] games) {
        this.games = games;
    }

    /**
     * @return the dungeon
     */
    public Dungeon getDungeon() {
        return dungeon;
    }

    /**
     * @param dungeon
     * the dungeon to set
     */
    public void setDungeon(Dungeon dungeon) {
        this.dungeon = dungeon;
    }

    /**
     * @return the maximum player count per group
     */
    public int getMaxGroupsPerGame() {
        return maxGroupsPerGame;
    }

    /**
     * @param maxGroupsPerGame
     * the maximum player count per group to set
     */
    public void setMaxGroupsPerGame(int maxGroupsPerGame) {
        this.maxGroupsPerGame = maxGroupsPerGame;
    }

    /**
     * Update this game sign to show the game(s) correctly.
     */
    public void update() {
        int i = 0;
        for (Game game : games) {
            if (!(startSign.getRelative(i * directionX, 0, i * directionZ).getState() instanceof Sign)) {
                i++;
                continue;
            }

            Sign sign = (Sign) startSign.getRelative(i * directionX, 0, i * directionZ).getState();

            // Reset Signs
            sign.setLine(0, "");
            sign.setLine(1, "");
            sign.setLine(2, "");
            sign.setLine(3, "");

            int yy = -1;
            while (sign.getBlock().getRelative(0, yy, 0).getState() instanceof Sign) {
                Sign subsign = (Sign) sign.getBlock().getRelative(0, yy, 0).getState();
                subsign.setLine(0, "");
                subsign.setLine(1, "");
                subsign.setLine(2, "");
                subsign.setLine(3, "");
                subsign.update();
                yy--;
            }

            // Set Signs
            if (game != null && game.getDGroups().size() > 0) {
                if (game.getDGroups().get(0).isPlaying()) {
                    sign.setLine(0, DMessage.SIGN_GLOBAL_IS_PLAYING.getMessage());

                } else if (game.getDGroups().size() >= maxGroupsPerGame) {
                    sign.setLine(0, DMessage.SIGN_GLOBAL_FULL.getMessage());

                } else {
                    sign.setLine(0, DMessage.SIGN_GLOBAL_JOIN_GAME.getMessage());
                }

                int j = 1;
                Sign rowSign = sign;

                for (DGroup dGroup : game.getDGroups()) {
                    if (j > 3) {
                        j = 0;
                        rowSign = (Sign) sign.getBlock().getRelative(0, -1, 0).getState();
                    }

                    if (rowSign != null) {
                        rowSign.setLine(j, dGroup.getName());
                    }

                    j++;
                    rowSign.update();
                }

            } else {
                sign.setLine(0, DMessage.SIGN_GLOBAL_NEW_GAME.getMessage());
            }

            sign.update();

            i++;
        }
    }

    @Override
    public Set<Block> getBlocks() {
        if (blocks == null) {
            blocks = new HashSet<>();

            int i = games.length;
            do {
                i--;

                blocks.add(startSign.getRelative(i * directionX, 0, i * directionZ));

            } while (i >= 0);

            HashSet<Block> toAdd = new HashSet<>();
            for (Block block : blocks) {
                i = verticalSigns;
                do {
                    i--;

                    Block beneath = block.getLocation().add(0, -1 * i, 0).getBlock();
                    toAdd.add(beneath);
                    toAdd.add(BlockUtil.getAttachedBlock(beneath));

                } while (i >= 0);
            }
            blocks.addAll(toAdd);
        }

        return blocks;
    }

    @Override
    public void save(FileConfiguration config) {
        String preString = "protections.gameSigns." + getWorld().getName() + "." + getId();

        config.set(preString + ".x", startSign.getX());
        config.set(preString + ".y", startSign.getY());
        config.set(preString + ".z", startSign.getZ());
        config.set(preString + ".dungeon", dungeon.getName());
        config.set(preString + ".maxGames", games.length);
        config.set(preString + ".maxGroupsPerGame", maxGroupsPerGame);
    }

    /* Statics */
    /**
     * @param block
     * a block which is protected by the returned GameSign
     */
    public static GameSign getByBlock(Block block) {
        if (!LegacyUtil.isSign(block)) {
            return null;
        }

        int x = block.getX(), y = block.getY(), z = block.getZ();
        for (GlobalProtection protection : DungeonsXL.getInstance().getGlobalProtections().getProtections(GameSign.class)) {
            GameSign gameSign = (GameSign) protection;

            int sx1 = gameSign.startSign.getX(), sy1 = gameSign.startSign.getY(), sz1 = gameSign.startSign.getZ();
            int sx2 = sx1 + (gameSign.games.length - 1) * gameSign.directionX;
            int sy2 = sy1 - gameSign.verticalSigns + 1;
            int sz2 = sz1 + (gameSign.games.length - 1) * gameSign.directionZ;

            if (sx1 > sx2) {
                if (x < sx2 || x > sx1) {
                    continue;
                }

            } else if (sx1 < sx2) {
                if (x > sx2 || x < sx1) {
                    continue;
                }

            } else if (x != sx1) {
                continue;
            }

            if (sy1 > sy2) {
                if (y < sy2 || y > sy1) {
                    continue;
                }

            } else if (y != sy1) {
                continue;
            }

            if (sz1 > sz2) {
                if (z < sz2 || z > sz1) {
                    continue;
                }

            } else if (sz1 < sz2) {
                if (z > sz2 || z < sz1) {
                    continue;
                }

            } else if (z != sz1) {
                continue;
            }

            return gameSign;
        }

        return null;
    }

    /**
     * @param game
     * the game to check
     */
    public static GameSign getByGame(Game game) {
        for (GlobalProtection protection : DungeonsXL.getInstance().getGlobalProtections().getProtections(GameSign.class)) {
            GameSign gameSign = (GameSign) protection;

            for (Game signGame : gameSign.games) {
                if (signGame == game) {
                    return gameSign;
                }
            }
        }

        return null;
    }

    /* SUBJECT TO CHANGE*/
    @Deprecated
    public static GameSign tryToCreate(Block startSign, String mapName, int maxGames, int maxGroupsPerGame) {
        World world = startSign.getWorld();
        int direction = startSign.getData();
        int x = startSign.getX(), y = startSign.getY(), z = startSign.getZ();

        int verticalSigns = (int) Math.ceil((float) (1 + maxGroupsPerGame) / 4);

        CopyOnWriteArrayList<Block> changeBlocks = new CopyOnWriteArrayList<>();

        int xx, yy, zz;
        switch (direction) {
            case 2:
                zz = z;

                for (yy = y; yy > y - verticalSigns; yy--) {
                    for (xx = x; xx > x - maxGames; xx--) {
                        Block block = world.getBlockAt(xx, yy, zz);

                        if (block.getType() != Material.AIR && block.getType() != Material.WALL_SIGN) {
                            return null;
                        }

                        if (block.getRelative(0, 0, 1).getType() == Material.AIR) {
                            return null;
                        }

                        changeBlocks.add(block);
                    }
                }

                break;

            case 3:
                zz = z;
                for (yy = y; yy > y - verticalSigns; yy--) {
                    for (xx = x; xx < x + maxGames; xx++) {

                        Block block = world.getBlockAt(xx, yy, zz);
                        if (block.getType() != Material.AIR && block.getType() != Material.WALL_SIGN) {
                            return null;
                        }

                        if (block.getRelative(0, 0, -1).getType() == Material.AIR) {
                            return null;
                        }

                        changeBlocks.add(block);
                    }
                }

                break;

            case 4:
                xx = x;
                for (yy = y; yy > y - verticalSigns; yy--) {
                    for (zz = z; zz < z + maxGames; zz++) {

                        Block block = world.getBlockAt(xx, yy, zz);
                        if (block.getType() != Material.AIR && block.getType() != Material.WALL_SIGN) {
                            return null;
                        }

                        if (block.getRelative(1, 0, 0).getType() == Material.AIR) {
                            return null;
                        }

                        changeBlocks.add(block);
                    }
                }
                break;

            case 5:
                xx = x;
                for (yy = y; yy > y - verticalSigns; yy--) {
                    for (zz = z; zz > z - maxGames; zz--) {

                        Block block = world.getBlockAt(xx, yy, zz);
                        if (block.getType() != Material.AIR && block.getType() != Material.WALL_SIGN) {
                            return null;
                        }

                        if (block.getRelative(-1, 0, 0).getType() == Material.AIR) {
                            return null;
                        }

                        changeBlocks.add(block);
                    }
                }

                break;
        }

        for (Block block : changeBlocks) {
            block.setTypeIdAndData(68, startSign.getData(), true);
        }

        GameSign sign = new GameSign(DungeonsXL.getInstance().getGlobalProtections().generateId(GameSign.class, world), startSign, mapName, maxGames, maxGroupsPerGame);

        return sign;
    }

    @Deprecated
    public static boolean playerInteract(Block block, Player player) {
        int x = block.getX(), y = block.getY(), z = block.getZ();
        GameSign gameSign = getByBlock(block);

        if (gameSign == null) {
            return false;
        }

        if (DungeonsXL.getInstance().getDWorlds().getGameWorlds().size() >= DungeonsXL.getInstance().getMainConfig().getMaxInstances()) {
            MessageUtil.sendMessage(player, DMessage.ERROR_TOO_MANY_INSTANCES.getMessage());
            return true;
        }

        DGroup dGroup = DGroup.getByPlayer(player);

        if (dGroup == null) {
            MessageUtil.sendMessage(player, DMessage.ERROR_JOIN_GROUP.getMessage());
            return true;
        }

        if (!dGroup.getCaptain().equals(player)) {
            MessageUtil.sendMessage(player, DMessage.ERROR_NOT_CAPTAIN.getMessage());
            return true;
        }

        if (Game.getByDGroup(dGroup) != null) {
            MessageUtil.sendMessage(player, DMessage.ERROR_LEAVE_GAME.getMessage());
            return true;
        }

        int sx1 = gameSign.startSign.getX(), sy1 = gameSign.startSign.getY(), sz1 = gameSign.startSign.getZ();

        Block topBlock = block.getRelative(0, sy1 - y, 0);

        int column;
        if (gameSign.directionX != 0) {
            column = Math.abs(x - sx1);

        } else {
            column = Math.abs(z - sz1);
        }

        if (!(topBlock.getState() instanceof Sign)) {
            return true;
        }

        Sign topSign = (Sign) topBlock.getState();

        if (topSign.getLine(0).equals(DMessage.SIGN_GLOBAL_NEW_GAME.getMessage())) {
            Game game = new Game(dGroup);
            dGroup.setDungeon(gameSign.dungeon);
            gameSign.games[column] = game;
            gameSign.update();

        } else if (topSign.getLine(0).equals(DMessage.SIGN_GLOBAL_JOIN_GAME.getMessage())) {
            gameSign.games[column].addDGroup(dGroup);
            gameSign.update();
        }

        return true;
    }

    @Deprecated
    public static int[] getDirection(byte data) {
        int[] direction = new int[2];

        switch (data) {
            case 2:
                direction[0] = -1;
                break;

            case 3:
                direction[0] = 1;
                break;

            case 4:
                direction[1] = 1;
                break;

            case 5:
                direction[1] = -1;
                break;
        }
        return direction;
    }

}
