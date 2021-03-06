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
package de.erethon.dungeonsxl.sign;

import de.erethon.dungeonsxl.world.DGameWorld;
import org.bukkit.Location;
import org.bukkit.block.Sign;

/**
 * @author Daniel Saukel
 */
public abstract class LocationSign extends DSign {

    protected Location location;

    public LocationSign(Sign sign, String[] lines, DGameWorld gameWorld) {
        super(sign, lines, gameWorld);
        double x = sign.getX() + 0.5;
        double y = sign.getY();
        double z = sign.getZ() + 0.5;
        float yaw = letterToYaw(((org.bukkit.material.Sign) getSign().getData()).getFacing().getOppositeFace().name().charAt(0));
        float pitch = 0;
        location = new Location(gameWorld.getWorld(), x, y, z, yaw, pitch);
    }

    /**
     * @return
     * the location marked by this sign
     */
    public Location getLocation() {
        return location;
    }

    public static int letterToYaw(char c) {
        switch (c) {
            case 'S':
            case 's':
                return 0;
            case 'W':
            case 'w':
                return 90;
            case 'N':
            case 'n':
                return 180;
            case 'E':
            case 'e':
                return -90;
            default:
                return -1;
        }
    }

}
