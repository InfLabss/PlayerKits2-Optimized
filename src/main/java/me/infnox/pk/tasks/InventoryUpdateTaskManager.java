package me.infnox.pk.tasks;

import me.infnox.pk.managers.*;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import me.infnox.pk.PlayerKits2;
import me.infnox.pk.model.inventory.InventoryPlayer;
import me.infnox.pk.utils.InventoryUtils;
import me.infnox.pk.utils.ItemUtils;

import java.util.ArrayList;

public class InventoryUpdateTaskManager {

    private PlayerKits2 plugin;
    public InventoryUpdateTaskManager(PlayerKits2 plugin){
        this.plugin = plugin;
    }

    public void start(){
        new BukkitRunnable(){
            @Override
            public void run() {
                execute();
            }
        }.runTaskTimer(plugin,0L,20L);
    }

    public void execute(){
        InventoryManager inventoryManager = plugin.getInventoryManager();
        KitsManager kitsManager = plugin.getKitsManager();
        MessagesManager msgManager = plugin.getMessagesManager();
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        KitItemManager kitItemManager = plugin.getKitItemManager();

        ArrayList<InventoryPlayer> players = inventoryManager.getPlayers();
        for(InventoryPlayer player : players){
            Inventory inv = InventoryUtils.getTopInventory(player.getPlayer());
            if(inv == null){
                continue;
            }
            ItemStack[] contents = inv.getContents();
            for(int i=0;i<contents.length;i++){
                ItemStack item = contents[i];
                if(item == null || item.getType().equals(Material.AIR)){
                    continue;
                }

                String kitName = ItemUtils.getTagStringItem(plugin,item,"playerkits_kit");
                if(kitName != null){
                    inventoryManager.setKit(kitName,player.getPlayer(),inv,i,kitsManager,
                            playerDataManager,kitItemManager,msgManager);
                }
            }
        }
    }
}
