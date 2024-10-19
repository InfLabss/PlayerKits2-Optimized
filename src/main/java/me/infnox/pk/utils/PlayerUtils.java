package me.infnox.pk.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerUtils {

    public static ItemStack[] getAllInventoryContents(Player player) {
        if (isLegacyVersion()) {
            ItemStack[] contents = new ItemStack[40];
            System.arraycopy(player.getInventory().getContents(), 0, contents, 0, player.getInventory().getContents().length);
            System.arraycopy(player.getInventory().getArmorContents(), 0, contents, player.getInventory().getContents().length, player.getInventory().getArmorContents().length);
            return contents;
        } else {
            return player.getInventory().getContents();
        }
    }

    public static int getUsedSlots(Player player) {
        ItemStack[] contents = isLegacyVersion() ? player.getInventory().getContents() : player.getInventory().getStorageContents();

        int usedSlots = 0;
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                usedSlots++;
            }
        }

        return usedSlots;
    }

    public static boolean isPlayerKitsAdmin(CommandSender sender) {
        return sender.hasPermission("playerkits.admin");
    }

    public static boolean hasCooldownBypassPermission(CommandSender sender) {
        return sender.hasPermission("playerkits.bypass.cooldown");
    }

    public static boolean hasOneTimeBypassPermission(CommandSender sender) {
        return sender.hasPermission("playerkits.bypass.onetime");
    }

    public static boolean passCondition(Player player, String condition) {
        String[] sep = condition.split(" ");
        if (sep.length < 3) return false;

        String variable = PlaceholderAPI.setPlaceholders(player, sep[0]);
        String operator = sep[1];
        String value = sep[2];

        try {
            double varValue = Double.parseDouble(variable);
            double condValue = Double.parseDouble(value);

            switch (operator) {
                case ">=":
                    return varValue >= condValue;
                case "<=":
                    return varValue <= condValue;
                case ">":
                    return varValue > condValue;
                case "<":
                    return varValue < condValue;
                case "==":
                    return varValue == condValue;
                case "!=":
                    return varValue != condValue;
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            switch (operator) {
                case "==":
                    return variable.equals(value);
                case "!=":
                    return !variable.equals(value);
                default:
                    return false;
            }
        }
    }

    private static boolean isLegacyVersion() {
        return Bukkit.getVersion().contains("1.8");
    }
}
