package dev.castiel.lib.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CommandRegistry {
    private final JavaPlugin plugin;

    public CommandRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(Object commandObject) {
        Command command = commandObject.getClass().getAnnotation(Command.class);
        if (command == null) {
            throw new IllegalArgumentException(commandObject.getClass().getName() + " is missing @Command");
        }
        DynamicCommand dynamic = new DynamicCommand(command.value(), command.description(), Arrays.asList(command.aliases()), commandObject);
        commandMap().register(plugin.getName().toLowerCase(Locale.ROOT), dynamic);
    }

    private CommandMap commandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access Bukkit command map", e);
        }
    }

    private static final class DynamicCommand extends org.bukkit.command.Command {
        private final Object target;
        private final Map<String, Method> methods = new LinkedHashMap<>();

        DynamicCommand(String name, String description, List<String> aliases, Object target) {
            super(name, description, "", aliases);
            this.target = target;
            for (Method method : target.getClass().getDeclaredMethods()) {
                SubCommand sub = method.getAnnotation(SubCommand.class);
                if (sub != null) {
                    method.setAccessible(true);
                    methods.put(sub.value().toLowerCase(Locale.ROOT), method);
                }
            }
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            Method method = select(args);
            if (method == null) {
                return false;
            }
            Permission permission = method.getAnnotation(Permission.class);
            if (permission == null) {
                permission = target.getClass().getAnnotation(Permission.class);
            }
            if (permission != null && !sender.hasPermission(permission.value())) {
                sender.sendMessage("§cYou do not have permission.");
                return true;
            }
            try {
                method.invoke(target, bind(method, sender, args));
                return true;
            } catch (ReflectiveOperationException e) {
                sender.sendMessage("§cCommand failed: " + e.getCause().getMessage());
                return true;
            }
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            if (args.length == 1) {
                List<String> out = new ArrayList<>();
                for (String key : methods.keySet()) {
                    if (!key.isEmpty() && key.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                        out.add(key);
                    }
                }
                return out;
            }
            return Collections.emptyList();
        }

        private Method select(String[] args) {
            if (args.length > 0 && methods.containsKey(args[0].toLowerCase(Locale.ROOT))) {
                return methods.get(args[0].toLowerCase(Locale.ROOT));
            }
            return methods.get("");
        }

        private Object[] bind(Method method, CommandSender sender, String[] rawArgs) {
            Parameter[] parameters = method.getParameters();
            Object[] bound = new Object[parameters.length];
            int argIndex = rawArgs.length > 0 && methods.containsKey(rawArgs[0].toLowerCase(Locale.ROOT)) ? 1 : 0;
            for (int i = 0; i < parameters.length; i++) {
                Class<?> type = parameters[i].getType();
                if (CommandSender.class.isAssignableFrom(type) || parameters[i].isAnnotationPresent(Sender.class)) {
                    bound[i] = sender;
                } else if (Player.class.isAssignableFrom(type)) {
                    bound[i] = sender instanceof Player ? sender : null;
                } else if (type == String.class) {
                    bound[i] = argIndex < rawArgs.length ? rawArgs[argIndex++] : "";
                } else if (type == int.class || type == Integer.class) {
                    bound[i] = argIndex < rawArgs.length ? Integer.parseInt(rawArgs[argIndex++]) : 0;
                } else if (type == String[].class) {
                    bound[i] = Arrays.copyOfRange(rawArgs, argIndex, rawArgs.length);
                } else {
                    bound[i] = null;
                }
            }
            return bound;
        }
    }
}
