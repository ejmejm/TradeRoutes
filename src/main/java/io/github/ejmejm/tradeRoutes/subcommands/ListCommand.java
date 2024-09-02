package io.github.ejmejm.tradeRoutes.subcommands;

import io.github.ejmejm.tradeRoutes.SubCommand;
import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class ListCommand extends SubCommand {
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
        return "/tr list < traders | caravans>";
    }

    private void listTraders(CommandSender sender) {
        Map<String, Trader> traders = TraderDatabase.getInstance().getTraders();
        // Create a string builder, and create a line for each trader in the following format:
        // Trader of <Affiliation> - (<x>, <y>, <z>)

        if (traders.isEmpty()) {
            sender.sendMessage(CMD_INFO_COLOR + "There are currently no traders.");
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

            Component traderName;
            if (affiliation == null) {
                traderName = Component.text("Unaffiliated Trader", NamedTextColor.WHITE);
            } else {
                traderName = Component.text("Trader of ", NamedTextColor.WHITE)
                        .append(Component.text(affiliation, affiliationColor, TextDecoration.BOLD, TextDecoration.ITALIC));
            }

            Component traderComponent = traderName
                    .append(Component.text(" - ", NamedTextColor.WHITE))
                    .append(Component.text("(", NamedTextColor.GRAY))
                    .append(Component.text(location.x(), NamedTextColor.GRAY))
                    .append(Component.text(", ", NamedTextColor.GRAY))
                    .append(Component.text(location.y(), NamedTextColor.GRAY))
                    .append(Component.text(", ", NamedTextColor.GRAY))
                    .append(Component.text(location.z(), NamedTextColor.GRAY))
                    .append(Component.text(")", NamedTextColor.GRAY))
                    .append(Component.newline());
            traderList = traderList.append(traderComponent);
        }

        sender.sendMessage(traderList);
    }

    private void listCaravans(CommandSender sender) {
        sender.sendMessage(CMD_ERROR_COLOR + "This command is not implemented yet.");
    }

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
