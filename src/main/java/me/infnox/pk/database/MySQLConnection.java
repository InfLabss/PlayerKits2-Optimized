package me.infnox.pk.database;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import me.infnox.pk.PlayerKits2;
import me.infnox.pk.managers.MessagesManager;
import me.infnox.pk.model.PlayerData;
import me.infnox.pk.model.PlayerDataKit;

import java.sql.*;
import java.util.*;

public class MySQLConnection {

    private final PlayerKits2 plugin;
    private HikariConnection connection;
    private String host, database, username, password;
    private int port;

    public MySQLConnection(PlayerKits2 plugin) {
        this.plugin = plugin;
    }

    public void setupMySql() {
        FileConfiguration config = plugin.getConfigsManager().getMainConfigManager().getConfig();
        try {
            this.host = config.getString("mysql_database.host");
            this.port = Integer.parseInt(config.getString("mysql_database.port"));
            this.database = config.getString("mysql_database.database");
            this.username = config.getString("mysql_database.username");
            this.password = config.getString("mysql_database.password");

            this.connection = new HikariConnection(host, port, database, username, password);
            createTables();
            loadData();

            Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(plugin.prefix + " &aSuccessfully connected to the Database."));
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(plugin.prefix + " &cError while connecting to the Database."));
        }
    }

    public String getDatabase() {
        return this.database;
    }

    private Connection getConnection() throws SQLException {
        return connection.getHikari().getConnection();
    }

    private void executeUpdate(String query, QueryParameterSetter setter) {
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            setter.setParameters(stmt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadData() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Initialize the ArrayList directly
                ArrayList<PlayerData> players = new ArrayList<>();

                try (Connection connection = getConnection()) {
                    PreparedStatement statement = connection.prepareStatement(
                            "SELECT playerkits_players.UUID, playerkits_players.PLAYER_NAME, " +
                                    "playerkits_players_kits.NAME, " +
                                    "playerkits_players_kits.COOLDOWN, " +
                                    "playerkits_players_kits.ONE_TIME, " +
                                    "playerkits_players_kits.BOUGHT " +
                                    "FROM playerkits_players LEFT JOIN playerkits_players_kits " +
                                    "ON playerkits_players.UUID = playerkits_players_kits.UUID"
                    );

                    ResultSet result = statement.executeQuery();
                    // Use a Map for efficient lookups while processing the data
                    Map<String, PlayerData> playerMap = new HashMap<>();

                    while (result.next()) {
                        String uuid = result.getString("UUID");
                        String playerName = result.getString("PLAYER_NAME");
                        String kitName = result.getString("NAME");
                        long cooldown = result.getLong("COOLDOWN");
                        boolean oneTime = result.getBoolean("ONE_TIME");
                        boolean bought = result.getBoolean("BOUGHT");

                        PlayerData player = playerMap.computeIfAbsent(uuid, id -> new PlayerData(playerName, uuid));

                        if (kitName != null) {
                            PlayerDataKit playerDataKit = new PlayerDataKit(kitName);
                            playerDataKit.setCooldown(cooldown);
                            playerDataKit.setOneTime(oneTime);
                            playerDataKit.setBought(bought);
                            player.addKit(playerDataKit);
                        }
                    }

                    players.addAll(playerMap.values());

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            plugin.getPlayerDataManager().setPlayers(players);
                        }
                    }.runTask(plugin);

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }


    private void createTables() {
        executeUpdate(
                "CREATE TABLE IF NOT EXISTS playerkits_players " +
                        "(UUID varchar(200) NOT NULL, PLAYER_NAME varchar(50), PRIMARY KEY (UUID))",
                stmt -> {}
        );
        executeUpdate(
                "CREATE TABLE IF NOT EXISTS playerkits_players_kits " +
                        "(ID int NOT NULL AUTO_INCREMENT, UUID varchar(200) NOT NULL, NAME varchar(100), " +
                        "COOLDOWN BIGINT, ONE_TIME BOOLEAN, BOUGHT BOOLEAN, PRIMARY KEY (ID), " +
                        "FOREIGN KEY (UUID) REFERENCES playerkits_players(UUID))",
                stmt -> {}
        );
    }

    public void getPlayer(String uuid, PlayerCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerData player = null;
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "SELECT p.UUID, p.PLAYER_NAME, k.NAME, k.COOLDOWN, k.ONE_TIME, k.BOUGHT " +
                                     "FROM playerkits_players p LEFT JOIN playerkits_players_kits k ON p.UUID = k.UUID WHERE p.UUID = ?")) {

                    stmt.setString(1, uuid);
                    try (ResultSet rs = stmt.executeQuery()) {
                        boolean firstFind = true;
                        while (rs.next()) {
                            if (firstFind) {
                                player = new PlayerData(rs.getString("PLAYER_NAME"), uuid);
                                firstFind = false;
                            }
                            String kitName = rs.getString("NAME");
                            if (kitName != null) {
                                PlayerDataKit kit = new PlayerDataKit(kitName);
                                kit.setCooldown(rs.getLong("COOLDOWN"));
                                kit.setOneTime(rs.getBoolean("ONE_TIME"));
                                kit.setBought(rs.getBoolean("BOUGHT"));
                                player.addKit(kit);
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                final PlayerData result = player;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.onDone(result);
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    public void createPlayer(PlayerData player, SimpleCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                executeUpdate(
                        "INSERT INTO playerkits_players (UUID, PLAYER_NAME) VALUES (?, ?)",
                        stmt -> {
                            stmt.setString(1, player.getUuid());
                            stmt.setString(2, player.getName());
                        }
                );
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.onDone();
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    public void updatePlayerName(PlayerData player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                executeUpdate(
                        "UPDATE playerkits_players SET PLAYER_NAME=? WHERE UUID=?",
                        stmt -> {
                            stmt.setString(1, player.getName());
                            stmt.setString(2, player.getUuid());
                        }
                );
            }
        }.runTaskAsynchronously(plugin);
    }

    public void updateKit(PlayerData player, PlayerDataKit kit, boolean mustCreate) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String query = mustCreate ?
                        "INSERT INTO playerkits_players_kits (UUID, NAME, COOLDOWN, ONE_TIME, BOUGHT) VALUES (?,?,?,?,?)" :
                        "UPDATE playerkits_players_kits SET COOLDOWN=?, ONE_TIME=?, BOUGHT=? WHERE UUID=? AND NAME=?";

                executeUpdate(query, stmt -> {
                    if (mustCreate) {
                        stmt.setString(1, player.getUuid());
                        stmt.setString(2, kit.getName());
                        stmt.setLong(3, kit.getCooldown());
                        stmt.setBoolean(4, kit.isOneTime());
                        stmt.setBoolean(5, kit.isBought());
                    } else {
                        stmt.setLong(1, kit.getCooldown());
                        stmt.setBoolean(2, kit.isOneTime());
                        stmt.setBoolean(3, kit.isBought());
                        stmt.setString(4, player.getUuid());
                        stmt.setString(5, kit.getName());
                    }
                });
            }
        }.runTaskAsynchronously(plugin);
    }

    public void resetKit(String uuid, String kitName, boolean all) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String query = all ?
                        "DELETE FROM playerkits_players_kits WHERE NAME=?" :
                        "DELETE FROM playerkits_players_kits WHERE UUID=? AND NAME=?";

                executeUpdate(query, stmt -> {
                    if (all) {
                        stmt.setString(1, kitName);
                    } else {
                        stmt.setString(1, uuid);
                        stmt.setString(2, kitName);
                    }
                });
            }
        }.runTaskAsynchronously(plugin);
    }

    @FunctionalInterface
    private interface QueryParameterSetter {
        void setParameters(PreparedStatement stmt) throws SQLException;
    }
}
