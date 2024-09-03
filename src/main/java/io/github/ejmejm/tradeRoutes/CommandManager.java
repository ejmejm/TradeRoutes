package io.github.ejmejm.tradeRoutes;

import io.github.ejmejm.tradeRoutes.subcommands.CancelMissionCommand;
import io.github.ejmejm.tradeRoutes.subcommands.ConfirmCommand;
import io.github.ejmejm.tradeRoutes.subcommands.ListCommand;
import io.github.ejmejm.tradeRoutes.subcommands.SpawnTraderCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabExecutor {
    protected static final NamedTextColor CMD_INFO_COLOR = NamedTextColor.BLUE;
    protected static final NamedTextColor CMD_ERROR_COLOR = NamedTextColor.RED;

    public record PendingConfirmation(ConfirmCommand.ConfirmationType type, Instant timestamp) {}
    private static final Map<UUID, PendingConfirmation> pendingConfirmations = new HashMap<>();

    private final Map<String, SubCommand> subcommands;
    private final Map<String, List<String>> tabCompletions; // Maps permission to syntax

    private static String repeat(String with, int count) {
        return new String(new char[count]).replace("\0", with);
    }

    public CommandManager() {
        List<SubCommand> commandList = Arrays.asList(
                new ListCommand(),
                new SpawnTraderCommand(),
                new CancelMissionCommand(),
                new ConfirmCommand()
        );

        subcommands = commandList.stream()
                .collect(Collectors.toMap(
                        subCommand -> subCommand.getName().toLowerCase(),
                        subCommand -> subCommand));
        tabCompletions = commandList.stream()
                .flatMap(subCommand -> subCommand.getPermissions().stream()
                        .map(permission -> Map.entry(permission, Arrays.asList(subCommand.getSyntax().split(" ")))))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.flatMapping(entry -> entry.getValue().stream(), Collectors.toList())
                ));
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
    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        return tabCompletions.entrySet().stream()
            .filter(entry -> sender.hasPermission(entry.getKey()))
            .filter(entry -> matchesArgs(entry.getValue(), args))
            .map(entry -> filterRecommendations(entry.getValue().get(args.length), args[args.length - 1]))
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    private boolean matchesArgs(List<String> completions, String[] args) {
        if (completions.size() <= args.length) return false;
        for (int i = 0; i < args.length - 1; i++) {
            if (!matchArg(completions.get(i + 1), args[i])) return false;
        }
        return true;
    }

    private boolean matchArg(String completion, String arg) {
        if (completion.startsWith("<") && completion.endsWith(">")) {
            return matchOptions(completion.substring(1, completion.length() - 1), arg, false);
        } else if (completion.startsWith("(") && completion.endsWith(")")) {
            return matchOptions(completion.substring(1, completion.length() - 1), arg, true);
        } else {
            return completion.equalsIgnoreCase(arg);
        }
    }

    private boolean matchOptions(String options, String arg, boolean allowEmpty) {
        if (allowEmpty && arg.isEmpty()) return true;
        String[] optionList = options.split("\\|");
        for (String option : optionList) {
            if (option.equals("affiliation")) {
                if (getAffiliations().contains(arg)) return true;
            } else if (option.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    private List<String> filterRecommendations(String completion, String currentArg) {
        if (completion.startsWith("<") && completion.endsWith(">")) {
            return getOptions(completion.substring(1, completion.length() - 1), false);
        } else if (completion.startsWith("(") && completion.endsWith(")")) {
            return getOptions(completion.substring(1, completion.length() - 1), true);
        } else {
            return Collections.singletonList(completion);
        }
    }

    private List<String> getOptions(String options, boolean includeEmpty) {
        List<String> result = new ArrayList<>();
        String[] optionList = options.split("\\|");
        for (String option : optionList) {
            if (option.equals("affiliation")) {
                result.addAll(getAffiliations());
            } else {
                result.add(option);
            }
        }
        if (includeEmpty) {
            result.add(" ");
        }
        return result;
    }

    private List<String> getAffiliations() {
        return List.of("affiliation");
    }

    public Map<String, SubCommand> getSubcommands(){
        return subcommands;
    }

    public static void setPendingConfirmation(UUID playerUUID, ConfirmCommand.ConfirmationType type) {
        pendingConfirmations.put(playerUUID, new PendingConfirmation(type, Instant.now()));
    }

    public static PendingConfirmation getPendingConfirmation(UUID playerUUID) {
        return pendingConfirmations.get(playerUUID);
    }

    public static void clearPendingConfirmation(UUID playerUUID) {
        pendingConfirmations.remove(playerUUID);
    }

}
