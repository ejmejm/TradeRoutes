package io.github.ejmejm.tradeRoutes.dataclasses;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import org.bukkit.Location;

public class Trader {
    public static String TRADER_NAME_PREFIX = "Trader-";

    private final Npc npc;
    private String affiliation; // town name

    public Trader(String uuid, String affiliation) {
        // Throw an exception if the NPC does not exist
        this.npc = FancyNpcsPlugin.get().getNpcManager().getNpcById(uuid);
        if (this.npc == null) {
            throw new IllegalArgumentException("NPC with UUID " + uuid + " does not exist.");
        }
        this.affiliation = affiliation;
    }

    public Trader(Npc npc, String affiliation) {
        this.npc = npc;
        this.affiliation = affiliation;
    }

    public Npc getNpc() {
        return npc;
    }
    public String getAffiliation() {
        return affiliation;
    }
    public String getUUID() {
        return npc.getData().getId();
    }
    public String getName() {
        return npc.getData().getName();
    }
    public Location getLocation() {
        return npc.getData().getLocation();
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }
}
