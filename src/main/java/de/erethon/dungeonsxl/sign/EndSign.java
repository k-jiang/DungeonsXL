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

import de.erethon.dungeonsxl.config.DMessage;
import de.erethon.dungeonsxl.player.DGamePlayer;
import de.erethon.dungeonsxl.trigger.InteractTrigger;
import de.erethon.dungeonsxl.world.DGameWorld;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

/**
 * @author Frank Baumann, Milan Albrecht, Daniel Saukel
 */
public class EndSign extends DSign {

    private DSignType type = DSignTypeDefault.END;

    public EndSign(Sign sign, String[] lines, DGameWorld gameWorld) {
        super(sign, lines, gameWorld);
    }

    @Override
    public boolean check() {
        return true;
    }

    @Override
    public void onInit() {
        if (!getTriggers().isEmpty()) {
            getSign().getBlock().setType(Material.AIR);
            return;
        }

        InteractTrigger trigger = InteractTrigger.getOrCreate(0, getSign().getBlock(), getGameWorld());
        if (trigger != null) {
            trigger.addListener(this);
            addTrigger(trigger);
        }

        getSign().setLine(0, ChatColor.DARK_BLUE + "############");
        getSign().setLine(1, DMessage.SIGN_END.getMessage());
        getSign().setLine(2, "");
        getSign().setLine(3, ChatColor.DARK_BLUE + "############");
        getSign().update();
    }

    @Override
    public boolean onPlayerTrigger(Player player) {
        DGamePlayer dPlayer = DGamePlayer.getByPlayer(player);
        if (dPlayer == null) {
            return true;
        }

        if (dPlayer.isFinished()) {
            return true;
        }

        dPlayer.finish();
        return true;
    }

    @Override
    public void onTrigger() {
        for (DGamePlayer dPlayer : DGamePlayer.getByWorld(getGameWorld().getWorld())) {
            dPlayer.finish();
        }
    }

    @Override
    public DSignType getType() {
        return type;
    }

}
