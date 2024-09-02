package io.github.ejmejm.tradeRoutes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

public abstract class SubCommand {

    protected NamedTextColor CMD_INFO_COLOR = NamedTextColor.BLUE;
    protected NamedTextColor CMD_ERROR_COLOR = NamedTextColor.RED;

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface ExpectPlayer { }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface ExpectNArgs {
        int value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface ExpectNArgsRange {
        int min() default 0;
        int max() default Integer.MAX_VALUE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface RequireOneOfPermissions {
        String[] value();
    }

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getSyntax();

    protected abstract void perform(CommandSender sender, String[] args);

    public void execute(CommandSender sender, String[] args) {
        Method performMethod;
        try {
            // Get the concrete implementation of the "perform" method in the subclass
            performMethod = this.getClass().getDeclaredMethod("perform", CommandSender.class, String[].class);

            // Check if @ExpectPlayer is present
            if (performMethod.isAnnotationPresent(ExpectPlayer.class)) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Component.text("Only players can use this command!", CMD_ERROR_COLOR));
                    return;
                }
            }

            // Check if @ExpectNArgs is present
            if (performMethod.isAnnotationPresent(ExpectNArgs.class)) {
                ExpectNArgs annotation = performMethod.getAnnotation(ExpectNArgs.class);
                int expectedArgs = annotation.value();
                if (args.length != expectedArgs) {
                    sender.sendMessage(Component.text(
                            "Invalid number of arguments. Expected syntax: " + getSyntax(), CMD_ERROR_COLOR));
                    return;
                }
            }

            // Check for @ExpectNArgsRange annotation
            if (performMethod.isAnnotationPresent(ExpectNArgsRange.class)) {
                ExpectNArgsRange annotation = performMethod.getAnnotation(ExpectNArgsRange.class);
                int minArgs = annotation.min();
                int maxArgs = annotation.max();
                if (args.length < minArgs || args.length > maxArgs) {
                    sender.sendMessage(Component.text(
                            "Invalid number of arguments. Expected syntax: " + getSyntax(), CMD_ERROR_COLOR));
                    return;
                }
            }

            // Check for @RequireOneOfPermissions annotation
            if (performMethod.isAnnotationPresent(RequireOneOfPermissions.class)) {
                RequireOneOfPermissions annotation = performMethod.getAnnotation(RequireOneOfPermissions.class);
                String[] permissions = annotation.value();
                boolean hasPermission = false;
                for (String p : permissions) {
                    if (sender.hasPermission(p)) {
                        hasPermission = true;
                        break;
                    }
                }
                if (!hasPermission) {
                    sender.sendMessage(Component.text(
                            "You do not have permission to use this command!", CMD_ERROR_COLOR));
                    return;
                }
            }

            // If all checks pass, execute the command
            perform(sender, args);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}
