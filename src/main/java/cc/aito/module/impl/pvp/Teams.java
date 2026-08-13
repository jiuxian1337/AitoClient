package cc.aito.module.impl.pvp;

import cc.aito.module.Module;
import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;

import java.util.UUID;

public class Teams extends Module {

    @Exclude
    private static Teams instance;

    public Teams() {
        super(new Mod("Teams", ModType.PVP), "teams.json");
        initialize();
        instance = this;
    }

    public static boolean isSameTeam(EntityPlayer player, EntityPlayer target) {
        if (instance == null || !instance.enabled || player == null || target == null) {
            return false;
        }
        Team localTeam = getTeam(player);
        if (localTeam == null) {
            return false;
        }
        Team targetTeam = getTeam(target);
        if (targetTeam == null) {
            return false;
        }
        if (localTeam instanceof ScorePlayerTeam && targetTeam instanceof ScorePlayerTeam) {
            String localPrefix = ((ScorePlayerTeam) localTeam).getColorPrefix();
            String targetPrefix = ((ScorePlayerTeam) targetTeam).getColorPrefix();
            if (localPrefix.equals(targetPrefix)) {
                return true;
            }
        }
        return localTeam.isSameTeam(targetTeam);
    }

    private static Team getTeam(EntityPlayer player) {
        Team team = player.getTeam();
        if (team != null) {
            return team;
        }
        if (mc.getNetHandler() == null) {
            return null;
        }
        UUID uuid = player.getUniqueID();
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info.getGameProfile().getId().equals(uuid)) {
                ScorePlayerTeam scoreTeam = info.getPlayerTeam();
                if (scoreTeam != null) {
                    return scoreTeam;
                }
            }
        }
        return null;
    }
}
