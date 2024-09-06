package io.github.ejmejm.tradeRoutes.events;


import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.sql.SQLException;
import java.util.Optional;

import static io.github.ejmejm.tradeRoutes.TraderManager.removeTraderTransaction;

public class TownDeletionListener implements Listener {

    @EventHandler
    public void onTownDeletion (DeleteTownEvent event) {
        String townName = event.getTownName();
        TraderDatabase db = TraderDatabase.getInstance();
        try {
            Optional<Trader> trader = db.getTraderByAffiliation(townName);
            if (trader.isPresent())
                removeTraderTransaction(trader.get());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
