package me.infnox.pk.managers;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import me.infnox.pk.PlayerKits2;
import me.infnox.pk.database.MySQLConnection;
import me.infnox.pk.model.PlayerData;
import me.infnox.pk.model.internal.PlayerKitsMessageResult;
import me.infnox.pk.utils.OtherUtils;

import java.util.ArrayList;

public class PlayerDataManager {

    private PlayerKits2 plugin;
    private ArrayList<PlayerData> players;

    public PlayerDataManager(PlayerKits2 plugin){
        this.plugin = plugin;
    }

    public ArrayList<PlayerData> getPlayers() {
        return players;
    }

    public void setPlayers(ArrayList<PlayerData> players) {
        this.players = players;
    }

    private PlayerData getPlayer(Player player,boolean create){
        for(PlayerData playerData : players){
            if(playerData.getUuid().equals(player.getUniqueId().toString())){
                return playerData;
            }
        }

        if(create){
            PlayerData playerData = new PlayerData(player.getName(),player.getUniqueId().toString());
            playerData.setModified(true);
            players.add(playerData);
            return playerData;
        }
        return null;
    }

    public PlayerData getPlayerByUUID(String uuid){
        for(PlayerData player : players){
            if(player.getUuid().equals(uuid)){
                return player;
            }
        }
        return null;
    }

    public void removePlayerByUUID(String uuid){
        for(int i=0;i<players.size();i++){
            if(players.get(i).getUuid().equals(uuid)){
                players.remove(i);
                return;
            }
        }
    }

    public PlayerData getPlayerByName(String name){
        for(PlayerData player : players){
            if(player.getName() != null && player.getName().equals(name)){
                return player;
            }
        }
        return null;
    }

    public void setKitCooldown(Player player,String kitName,long cooldown){
        PlayerData playerData = getPlayer(player,true);
        boolean creating = playerData.setKitCooldown(kitName,cooldown);
        playerData.setModified(true);
        if(plugin.getMySQLConnection() != null){
            plugin.getMySQLConnection().updateKit(playerData,playerData.getKit(kitName),creating);
        }
    }

    public long getKitCooldown(Player player,String kitName){
        PlayerData playerData = getPlayerByUUID(player.getUniqueId().toString());
        if(playerData == null){
            return 0;
        }else{
            return playerData.getKitCooldown(kitName);
        }
    }

    public String getKitCooldownString(long playerCooldown){
        long currentMillis = System.currentTimeMillis();
        long millisDif = playerCooldown-currentMillis;
        String timeStringMillisDif = OtherUtils.getTime(millisDif/1000, plugin.getMessagesManager());
        return timeStringMillisDif;
    }

    public void setKitOneTime(Player player,String kitName){
        PlayerData playerData = getPlayer(player,true);
        boolean creating = playerData.setKitOneTime(kitName);
        playerData.setModified(true);
        if(plugin.getMySQLConnection() != null){
            plugin.getMySQLConnection().updateKit(playerData,playerData.getKit(kitName),creating);
        }
    }

    public boolean isKitOneTime(Player player,String kitName){
        PlayerData playerData = getPlayerByUUID(player.getUniqueId().toString());
        if(playerData == null){
            return false;
        }else{
            return playerData.getKitOneTime(kitName);
        }
    }

    public void setKitBought(Player player,String kitName){
        PlayerData playerData = getPlayer(player,true);
        boolean creating = playerData.setKitBought(kitName);
        playerData.setModified(true);
        if(plugin.getMySQLConnection() != null){
            plugin.getMySQLConnection().updateKit(playerData,playerData.getKit(kitName),creating);
        }
    }

    public boolean isKitBought(Player player,String kitName){
        PlayerData playerData = getPlayerByUUID(player.getUniqueId().toString());
        if(playerData == null){
            return false;
        }else{
            return playerData.getKitHasBought(kitName);
        }
    }

    public PlayerKitsMessageResult resetKitForPlayer(String name, String kitName, boolean all){
        PlayerData playerData = getPlayerByName(name);
        FileConfiguration messagesConfig = plugin.getConfigsManager().getMessagesConfigManager().getConfig();
        if(playerData == null && !all){
            return PlayerKitsMessageResult.error(messagesConfig.getString("playerDataNotFound")
                    .replace("%player%",name));
        }

        if(all){
            for(PlayerData p : players){
                p.resetKit(kitName);
            }
        }else{
            playerData.resetKit(kitName);
        }

        if(plugin.getMySQLConnection() != null){
            if(all){
                plugin.getMySQLConnection().resetKit(null,kitName,true);
            }else{
                plugin.getMySQLConnection().resetKit(playerData.getUuid(),kitName,false);
            }

        }

        return PlayerKitsMessageResult.success();
    }

    public void manageJoin(Player player){
        // Create or update data
        if(plugin.getMySQLConnection() != null){
            MySQLConnection mySQLConnection = plugin.getMySQLConnection();
            String uuid = player.getUniqueId().toString();
            mySQLConnection.getPlayer(uuid, playerData -> {
                removePlayerByUUID(uuid); //Remove data if already exists
                if(playerData != null) {
                    players.add(playerData);
                    //Update name if different
                    if(!playerData.getName().equals(player.getName())){
                        playerData.setName(player.getName());
                        mySQLConnection.updatePlayerName(playerData);
                    }
                }else {
                    playerData = new PlayerData(player.getName(),uuid);
                    players.add(playerData);

                    //Create if it doesn't exist
                    mySQLConnection.createPlayer(playerData, () -> plugin.getKitsManager().giveFirstJoinKit(player));
                }
            });
        }else{
            PlayerData playerData = getPlayerByUUID(player.getUniqueId().toString());
            if(playerData == null){
                playerData = new PlayerData(player.getName(),player.getUniqueId().toString());
                playerData.setModified(true);
                players.add(playerData);
                plugin.getKitsManager().giveFirstJoinKit(player);
            }else{
                if(playerData.getName() == null || !playerData.getName().equals(player.getName())){
                    playerData.setName(player.getName());
                    playerData.setModified(true);
                }
            }
        }
    }
}
