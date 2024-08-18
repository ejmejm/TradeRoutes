package io.github.ejmejm.tradeRoutes;

import io.github.ejmejm.tradeRoutes.subcommands.ListCommand;
import io.github.ejmejm.tradeRoutes.subcommands.SpawnTraderCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor {

    private final Map<String, SubCommand> subcommands;

    public CommandManager() {
        List<SubCommand> commandList = Arrays.asList(
                new ListCommand(),
                new SpawnTraderCommand()
        );
        subcommands = commandList.stream()
                .collect(Collectors.toMap(
                        subCommand -> subCommand.getName().toLowerCase(),
                        subCommand -> subCommand));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if(args.length == 0) {
            sender.sendMessage("--------------------------------");
            for (SubCommand subcommand : subcommands.values()) {
                sender.sendMessage(subcommand.getSyntax() + " - " + subcommand.getDescription());
            }
            sender.sendMessage("--------------------------------");
        } else {
            SubCommand subcommand = subcommands.get(args[0].toLowerCase());
            if (subcommand == null) {
                sender.sendMessage("Invalid command. Subcommand '" + args[0] + "' does not exist.");
            } else {
                subcommand.execute(sender, args);
            }
        }

        return true;
    }

    public Map<String, SubCommand> getSubcommands(){
        return subcommands;
    }

}
