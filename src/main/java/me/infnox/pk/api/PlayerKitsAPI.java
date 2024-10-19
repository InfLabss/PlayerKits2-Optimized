package me.infnox.pk.api;

import org.bukkit.entity.Player;
import me.infnox.pk.PlayerKits2;
import me.infnox.pk.managers.MessagesManager;
import me.infnox.pk.managers.PlayerDataManager;
import me.infnox.pk.model.Kit;
import me.infnox.pk.model.internal.GiveKitInstructions;
import me.infnox.pk.model.internal.PlayerKitsMessageResult;
import me.infnox.pk.utils.PlayerUtils;

public class PlayerKitsAPI {

    private static PlayerKits2 plugin;
    public PlayerKitsAPI(PlayerKits2 plugin){
        this.plugin = plugin;
    }

    public static String getKitCooldown(Player player, String kitName){
        Kit kit = plugin.getKitsManager().getKitByName(kitName);
        MessagesManager messagesManager = plugin.getMessagesManager();

        if(kit == null){
            return null;
        }

        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        long playerCooldown = playerDataManager.getKitCooldown(player,kit.getName());
        if(kit.getCooldown() != 0 && !PlayerUtils.isPlayerKitsAdmin(player)){
            String timeStringMillisDif = playerDataManager.getKitCooldownString(playerCooldown);
            if(!timeStringMillisDif.isEmpty()) {
                return timeStringMillisDif;
            }
        }

        return messagesManager.getCooldownPlaceholderReady();
    }

    public static String getOneTimeReady(Player player, String kitName){
        Kit kit = plugin.getKitsManager().getKitByName(kitName);
        if(kit == null){
            return null;
        }

        boolean oneTime = plugin.getPlayerDataManager().isKitOneTime(player,kitName);
        if(oneTime){
            return "yes";
        }else{
            return "no";
        }
    }

    /**
     * Get a kit by its name.
     * @param kitName The name of the kit.
     * @return The Kit object, or null if not found.
     */
    public static Kit getKit(String kitName) {
        return plugin.getKitsManager().getKitByName(kitName);
    }

    /**
     * Give a kit to a player with custom instructions.
     * @param player The player to give the kit to.
     * @param kitName The name of the kit.
     * @param instructions Custom instructions for giving the kit.
     * @return A PlayerKitsMessageResult indicating success or failure.
     */
    public static PlayerKitsMessageResult giveKit(Player player, String kitName, GiveKitInstructions instructions) {
        return plugin.getKitsManager().giveKit(player, kitName, instructions);
    }

}
