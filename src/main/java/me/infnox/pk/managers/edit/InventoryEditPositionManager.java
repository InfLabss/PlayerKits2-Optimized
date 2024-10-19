package me.infnox.pk.managers.edit;

import me.infnox.pk.managers.InventoryManager;
import me.infnox.pk.managers.KitItemManager;
import me.infnox.pk.managers.KitsManager;
import me.infnox.pk.managers.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import me.infnox.pk.PlayerKits2;
import me.infnox.pk.model.Kit;
import me.infnox.pk.model.internal.KitVariable;
import me.infnox.pk.model.inventory.InventoryPlayer;
import me.infnox.pk.model.inventory.ItemKitInventory;
import me.infnox.pk.model.inventory.KitInventory;
import me.infnox.pk.utils.InventoryItem;
import me.infnox.pk.utils.InventoryUtils;
import me.infnox.pk.utils.ItemUtils;

import java.util.ArrayList;
import java.util.List;

public class InventoryEditPositionManager {

    private PlayerKits2 plugin;
    private InventoryEditManager inventoryEditManager;
    public InventoryEditPositionManager(PlayerKits2 plugin, InventoryEditManager inventoryEditManager){
        this.plugin = plugin;
        this.inventoryEditManager = inventoryEditManager;
    }

    public void openInventory(InventoryPlayer inventoryPlayer,String positionInventory) {
        KitInventory kitInventory = plugin.getInventoryManager().getInventory(positionInventory);

        Inventory inv = Bukkit.createInventory(null,kitInventory.getSlots(),
                MessagesManager.getColoredMessage(kitInventory.getTitle()));

        List<ItemKitInventory> items = kitInventory.getItems();
        KitItemManager kitItemManager = plugin.getKitItemManager();
        KitsManager kitsManager = plugin.getKitsManager();

        for(ItemKitInventory itemInventory : items){
            for(int slot : itemInventory.getSlots()){
                String type = itemInventory.getType();
                if(type != null){
                    if(type.startsWith("kit: ")){
                        String kitName = type.replace("kit: ","");
                        Kit kit = kitsManager.getKitByName(kitName);
                        if(kit == null){
                            continue;
                        }
                        ItemStack item = kitItemManager.createItemFromKitItem(kit.getDisplayItemDefault(),null);
                        //Check if it is the same kit
                        if(kitName.equals(inventoryPlayer.getKitName())){
                            ItemMeta meta = item.getItemMeta();
                            List<String> extraLore = new ArrayList<>();
                            extraLore.add(" ");
                            extraLore.add(MessagesManager.getColoredMessage("&a&lTHIS IS THE CURRENT POSITION OF"));
                            extraLore.add(MessagesManager.getColoredMessage("&a&lTHE KIT."));
                            List<String> itemLore = new ArrayList<>();
                            if(meta.hasLore()){
                                itemLore = new ArrayList<>(meta.getLore());
                            }
                            itemLore.addAll(extraLore);
                            meta.setLore(itemLore);
                            item.setItemMeta(meta);
                        }

                        ArrayList<KitVariable> variablesToReplace = new ArrayList<>();
                        variablesToReplace.add(new KitVariable("%kit_name%",kit.getName()));
                        kitItemManager.replaceVariables(item,variablesToReplace);

                        inv.setItem(slot,item);
                    }
                    continue;
                }

                ItemStack item = kitItemManager.createItemFromKitItem(itemInventory.getItem(),inventoryPlayer.getPlayer());
                String openInventory = itemInventory.getOpenInventory();
                if(openInventory != null) {
                    item = ItemUtils.setTagStringItem(plugin,item, "playerkits_open_inventory", openInventory);
                }
                inv.setItem(slot,item);
            }
        }

        inventoryPlayer.setInventoryName("edit_position;"+positionInventory+";false");
        inventoryPlayer.getPlayer().openInventory(inv);
        inventoryPlayer.setInventoryName("edit_position;"+positionInventory+";true");
        inventoryPlayer.saveInventoryContents();
        inventoryPlayer.getPlayer().getInventory().clear();
        //Set Info Item
        List<String> lore = new ArrayList<String>();
        lore.add("&7Just click on the position of the");
        lore.add("&7inventory, where you want the new kit");
        lore.add("&7item to be displayed.");
        lore.add("");
        lore.add("&7You can go back by closing this inventory.");
        new InventoryItem(inventoryPlayer.getPlayer().getInventory(), 22, Material.COMPASS).name("&6&lInfo").lore(lore).ready();
        inventoryPlayer.getPlayer().updateInventory();
        inventoryEditManager.getPlayers().add(inventoryPlayer);


    }

    public void setPosition(InventoryPlayer inventoryPlayer,int slot){
        String positionInventory = inventoryPlayer.getInventoryName().split(";")[1];
        InventoryManager inventoryManager = plugin.getInventoryManager();
        String kitName = inventoryPlayer.getKitName();

        // Remove kit position if already exists (check on all inventories)
        inventoryManager.removeKitFromInventory(kitName);

        KitInventory kitInventory = inventoryManager.getInventory(positionInventory);
        kitInventory.addKitItemOnSlot(kitName,slot);
        plugin.getConfigsManager().getInventoryConfigManager().save();

        closeInventory(inventoryPlayer);
    }

    public void clickInventory(InventoryPlayer inventoryPlayer, ItemStack item, int slot, ClickType clickType, InventoryClickEvent event){
        event.setCancelled(true);
        if(event.getSlotType() == null || event.getClickedInventory() == null){
            return;
        }
        if(event.getClickedInventory().equals(InventoryUtils.getTopInventory(inventoryPlayer.getPlayer()))){
            //Should not be an item unless has open inventory tag
            if(item == null || item.getType().equals(Material.AIR)){
                //Update position
                setPosition(inventoryPlayer,slot);
                inventoryPlayer.getPlayer().sendMessage(MessagesManager.getColoredMessage(PlayerKits2.prefix+"&aKit position updated."));
                return;
            }

            String openInventory = ItemUtils.getTagStringItem(plugin,item,"playerkits_open_inventory");
            if(openInventory != null){
                openInventory(inventoryPlayer,openInventory);
                return;
            }
            inventoryPlayer.getPlayer().sendMessage(MessagesManager.getColoredMessage(PlayerKits2.prefix+"&cThis position is occupied."));
        }
    }

    public void closeInventory(InventoryPlayer inventoryPlayer){
        boolean mustReturn = Boolean.parseBoolean(inventoryPlayer.getInventoryName().split(";")[2]);
        if(mustReturn){
            new BukkitRunnable(){
                @Override
                public void run() {
                    inventoryPlayer.restoreSavedInventoryContents();
                    inventoryEditManager.openInventory(inventoryPlayer);
                }
            }.runTaskLater(plugin,1L);
        }
    }
}
