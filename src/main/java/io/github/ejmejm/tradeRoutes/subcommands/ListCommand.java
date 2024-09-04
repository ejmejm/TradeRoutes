package io.github.ejmejm.tradeRoutes.subcommands;

import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.dataclasses.ActiveTradeMission;
import io.github.ejmejm.tradeRoutes.dataclasses.TradeMissionSpec;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class ListCommand extends SubCommand {
    private static final String TRADER_PERMISSION = "traderoutes.command.list.traders";
    private static final String CARAVAN_PERMISSION = "traderoutes.command.list.missions";

    private static final TextColor[] affiliationColors = {
            NamedTextColor.GOLD,
            NamedTextColor.LIGHT_PURPLE,
            NamedTextColor.DARK_PURPLE,
            NamedTextColor.GREEN,
            NamedTextColor.DARK_GREEN,
            NamedTextColor.BLUE,
            NamedTextColor.DARK_BLUE,
            NamedTextColor.AQUA,
            NamedTextColor.DARK_AQUA,
            TextColor.color(0x938BFF),
            TextColor.color(0xFFB55E),
            TextColor.color(0xA65DFF),
            TextColor.color(0xFF4CA0)
    };

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "List all traders.";
    }

    @Override
    public String getSyntax() {
        return "/tr list <traders|caravans>";
    }

    @Override
    public List<String> getPermissions() {
        return List.of(TRADER_PERMISSION, CARAVAN_PERMISSION);
    }

    private void listTraders(CommandSender sender) {
        if (!sender.hasPermission(TRADER_PERMISSION)) {
            sender.sendMessage(Component.text("You don't have permission to list traders.", CMD_ERROR_COLOR));
            return;
        }

        Map<String, Trader> traders = TraderDatabase.getInstance().getTraders();

        if (traders.isEmpty()) {
            sender.sendMessage(Component.text("There are currently no traders.", CMD_INFO_COLOR));
            return;
        }

        Component traderList = Component.text("------------- [ Traders List ] -------------", NamedTextColor.GOLD)
                .append(Component.newline());

        int currentColorIndex = 0;
        for (Trader trader : traders.values()) {
            Location location = trader.getLocation();
            TextColor affiliationColor = affiliationColors[currentColorIndex];
            currentColorIndex = (currentColorIndex + 1) % affiliationColors.length;
            String affiliation = trader.getAffiliation();

            Component traderName = createTraderNameComponent(affiliation, affiliationColor);

            Component traderComponent = traderName
                    .append(Component.text(" - ", NamedTextColor.WHITE))
                    .append(locationToComponent(location))
                    .append(Component.newline());
            traderList = traderList.append(traderComponent);
        }

        sender.sendMessage(traderList);
    }

    private void listCaravans(CommandSender sender) {
        if (!sender.hasPermission(CARAVAN_PERMISSION)) {
            sender.sendMessage(Component.text("You don't have permission to list caravans.", CMD_ERROR_COLOR));
            return;
        }

        List<ActiveTradeMission> activeMissions = TraderDatabase.getInstance().getAllActiveTradeMissions();

        if (activeMissions.isEmpty()) {
            sender.sendMessage(Component.text("There are currently no active caravans.", CMD_INFO_COLOR));
            return;
        }
    
        Component caravanList = Component.text(
                "------------- [ Active Trade Missions ] -------------", NamedTextColor.GOLD)
                .append(Component.newline());
    
        if (sender instanceof Player player) {
            activeMissions.sort((m1, m2) -> {
                Location endLoc1 = m1.getMissionSpec().getEndTrader().getLocation();
                Location endLoc2 = m2.getMissionSpec().getEndTrader().getLocation();
                return Double.compare(player.getLocation().distance(endLoc1), player.getLocation().distance(endLoc2));
            });
        }
        for (ActiveTradeMission mission : activeMissions) {
            TradeMissionSpec spec = mission.getMissionSpec();
            String playerName = mission.getPlayerName();
            Location startLoc = spec.getStartTrader().getLocation();
            Location endLoc = spec.getEndTrader().getLocation();

            Component missionComponent = Component.text(playerName, NamedTextColor.AQUA)
                    .append(Component.text(" : ", NamedTextColor.WHITE))
                    .append(Component.text(
                            spec.getStartTrader().getAffiliation(), NamedTextColor.GOLD,
                            TextDecoration.BOLD, TextDecoration.ITALIC))
                    .append(Component.text(" ", NamedTextColor.WHITE))
                    .append(locationToComponent(startLoc))
                    .append(Component.text(" -> ", NamedTextColor.WHITE))
                    .append(Component.text(
                            spec.getEndTrader().getAffiliation(), NamedTextColor.GOLD,
                            TextDecoration.BOLD, TextDecoration.ITALIC))
                    .append(Component.text(" ", NamedTextColor.WHITE))
                    .append(locationToComponent(endLoc));

            // Append distance to caravan if the sender is a player and the caravan exists
            Entity caravan = Bukkit.getEntity(mission.getCaravanUUID());
            if (sender instanceof Player player && caravan != null) {
                int distanceToCaravan = (int) player.getLocation().distance(caravan.getLocation());
                missionComponent = missionComponent.append(Component.text(" <", NamedTextColor.BLUE)
                        .append(Component.text(distanceToCaravan, NamedTextColor.BLUE))
                        .append(Component.text(" blocks away>", NamedTextColor.BLUE)));
            }

            missionComponent = missionComponent.append(Component.newline());
            caravanList = caravanList.append(missionComponent);
        }
        sender.sendMessage(caravanList);
    }

    private Component createTraderNameComponent(String affiliation, TextColor affiliationColor) {
        if (affiliation == null) {
            return Component.text("Unaffiliated Trader", NamedTextColor.WHITE);
        } else {
            return Component.text("Trader of ", NamedTextColor.WHITE)
                    .append(Component.text(affiliation, affiliationColor, TextDecoration.BOLD, TextDecoration.ITALIC));
        }
    }

    private Component locationToComponent(Location location) {
        return Component.text("(", NamedTextColor.GRAY)
                .append(Component.text((int)location.getX(), NamedTextColor.GRAY))
                .append(Component.text(", ", NamedTextColor.GRAY))
                .append(Component.text((int)location.getY(), NamedTextColor.GRAY))
                .append(Component.text(", ", NamedTextColor.GRAY))
                .append(Component.text((int)location.getZ(), NamedTextColor.GRAY))
                .append(Component.text(")", NamedTextColor.GRAY));
    }

    @RequireOneOfPermissions({TRADER_PERMISSION, CARAVAN_PERMISSION})
    @ExpectNArgs(2)
    @Override
    protected void perform(CommandSender sender, String[] args) {
        String listType = args[1];
        switch (listType) {
            case "traders":
                listTraders(sender);
                break;
            case "caravans":
                listCaravans(sender);
                break;
            default:
                sender.sendMessage(CMD_ERROR_COLOR + "Invalid subcommand. Expected syntax: " + getSyntax());
        }
    }
}
