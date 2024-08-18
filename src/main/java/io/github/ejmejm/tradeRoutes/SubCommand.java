package io.github.ejmejm.tradeRoutes;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

public abstract class SubCommand {

    protected String CMD_ERROR_COLOR = "<#ff0000>";

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
                    sender.sendMessage(CMD_ERROR_COLOR + "Only players can use this command!");
                    return;
                }
            }

            // Check if @ExpectNArgs is present
            if (performMethod.isAnnotationPresent(ExpectNArgs.class)) {
                ExpectNArgs annotation = performMethod.getAnnotation(ExpectNArgs.class);
                int expectedArgs = annotation.value();
                if (args.length != expectedArgs) {
                    sender.sendMessage(CMD_ERROR_COLOR + "Invalid number of arguments. Expected syntax: " + getSyntax());
                    return;
                }
            }

            // Check for @ExpectNArgsRange annotation
            if (performMethod.isAnnotationPresent(ExpectNArgsRange.class)) {
                ExpectNArgsRange annotation = performMethod.getAnnotation(ExpectNArgsRange.class);
                int minArgs = annotation.min();
                int maxArgs = annotation.max();
                if (args.length < minArgs || args.length > maxArgs) {
                    sender.sendMessage(CMD_ERROR_COLOR + "Invalid number of arguments. Expected syntax: " + getSyntax());
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
