package io.github.ejmejm.tradeRoutes.subcommands;

import io.github.ejmejm.tradeRoutes.SubCommand;
import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.HashMap;

public class ListCommand extends SubCommand {
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
        return "/tr list [traders|caravans]";
    }

    private void listTraders(CommandSender sender) {
        HashMap<String, Trader> traders = TraderDatabase.getInstance().getTraders();
        // Create a string builder, and create a line for each trader in the following format:
        // Trader of <Affiliation> - (<x>, <y>, <z>)

        if (traders.isEmpty()) {
            sender.sendMessage("There are currently no traders.");
            return;
        }

        StringBuilder traderList = new StringBuilder();
        traderList.append("Traders:\n");
        for (Trader trader : traders.values()) {
            Location location = trader.getLocation();
            traderList.append("Trader of <<b><i>")
                    .append(trader.getAffiliation())
                    .append("</i></b>> - <gray>(")
                    .append(location.x()).append(", ")
                    .append(location.y()).append(", ")
                    .append(location.z()).append(")\n");
        }
        String traderListString = traderList.substring(0, traderList.length() - 1);
        sender.sendMessage(traderListString);
    }

    private void listCaravans(CommandSender sender) {
        sender.sendMessage("This command is not implemented yet.");
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
