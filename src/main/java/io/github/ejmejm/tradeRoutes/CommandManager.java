package io.github.ejmejm.tradeRoutes;

import io.github.ejmejm.tradeRoutes.subcommands.ListCommand;
import io.github.ejmejm.tradeRoutes.subcommands.SpawnTraderCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor {
    protected NamedTextColor CMD_INFO_COLOR = NamedTextColor.BLUE;
    protected NamedTextColor CMD_ERROR_COLOR = NamedTextColor.RED;

    private static final TextColor ORANGE = TextColor.color(0xFFAE00);

    private final Map<String, SubCommand> subcommands;

    private static String repeat(String with, int count) {
        return new String(new char[count]).replace("\0", with);
    }

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
            String pluginHeader = repeat("-", 17)
                    + " [ /tr ] "
                    + repeat("-", 17);

            Component helpMessage = Component.text(pluginHeader, NamedTextColor.GOLD)
                    .append(Component.newline());

            for (SubCommand subcommand : subcommands.values()) {
                String[] syntax = subcommand.getSyntax().split(" ");
                // Add a component with the first word in the syntax with blue color
                // Add a component with the rest of the syntax with cyan color
                // Add a description with gray color
                Component syntaxComponent = Component.text(syntax[0], NamedTextColor.BLUE);
                if (syntax.length > 1) {
                    String commandArgs = String.join(" ", Arrays.copyOfRange(syntax, 1, syntax.length));
                    syntaxComponent = syntaxComponent.append(Component.text(" " + commandArgs, NamedTextColor.AQUA));
                }
                Component descriptionComponent = Component.text(
                        " : " + subcommand.getDescription(), NamedTextColor.GRAY);
                helpMessage = helpMessage
                        .append(syntaxComponent)
                        .append(descriptionComponent)
                        .append(Component.newline());
            }
            helpMessage = helpMessage.append(
                    Component.text(repeat("-", pluginHeader.length() - 2), NamedTextColor.GRAY));
            sender.sendMessage(helpMessage);
        } else {
            SubCommand subcommand = subcommands.get(args[0].toLowerCase());
            if (subcommand == null) {
                sender.sendMessage(Component.text(
                        "Invalid command. Subcommand '" + args[0] + "' does not exist.", CMD_ERROR_COLOR));
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
