package me.infnox.pk.database;

import me.infnox.pk.model.PlayerData;

public interface PlayerCallback {

    void onDone(PlayerData player);
}
