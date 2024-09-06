package io.github.ejmejm.tradeRoutes.events;


import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownDeletionListener implements Listener {

    @EventHandler
    public void onTownDeletion (DeleteTownEvent event) {
        String townName = event.getTownName();
    }

}
