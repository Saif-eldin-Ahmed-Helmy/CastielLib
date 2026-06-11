package dev.castiel.lib.commands;

import dev.castiel.lib.permissions.Permissions;
import dev.castiel.lib.permissions.PermissionResolver;
import dev.castiel.lib.text.Colors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CommandRegistry {
    private final JavaPlugin plugin;
    private String noPermissionMessage = "";
    private PermissionResolver permissionResolver = new PermissionResolver() {
        @Override
        public boolean hasPermission(CommandSender sender, String permission) {
            return Permissions.hasWildcard(sender, permission);
        }
    };

    public CommandRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public CommandRegistry noPermissionMessage(String message) {
        this.noPermissionMessage = message == null ? "" : message;
        return this;
    }

    public CommandRegistry permissionResolver(PermissionResolver permissionResolver) {
        if (permissionResolver != null) {
            this.permissionResolver = permissionResolver;
        }
        return this;
    }

    public void register(Object commandObject) {
        Command command = commandObject.getClass().getAnnotation(Command.class);
        if (command == null) {
            throw new IllegalArgumentException(commandObject.getClass().getName() + " is missing @Command");
        }
        DynamicCommand dynamic = new DynamicCommand(this, command.value(), command.description(), Arrays.asList(command.aliases()), commandObject);
        CommandMap map = commandMap();
        List<String> labels = new ArrayList<String>();
        labels.add(command.value());
        labels.addAll(Arrays.asList(command.aliases()));
        unregisterExisting(map, labels);
        map.register(plugin.getName().toLowerCase(Locale.ROOT), dynamic);
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

    @SuppressWarnings("unchecked")
    private void unregisterExisting(CommandMap map, Collection<String> labels) {
        try {
            Field field = findField(map.getClass(), "knownCommands");
            field.setAccessible(true);
            Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) field.get(map);
            Set<String> requested = normalize(labels);
            Set<String> keysToRemove = new HashSet<String>();
            String prefix = plugin.getName().toLowerCase(Locale.ROOT) + ":";
            for (Map.Entry<String, org.bukkit.command.Command> entry : knownCommands.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT);
                org.bukkit.command.Command command = entry.getValue();
                if (requested.contains(key) || (key.startsWith(prefix) && requested.contains(key.substring(prefix.length()))) || matches(command, requested)) {
                    keysToRemove.add(entry.getKey());
                    command.unregister(map);
                }
            }
            for (String key : keysToRemove) {
                knownCommands.remove(key);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Set<String> normalize(Collection<String> labels) {
        Set<String> normalized = new HashSet<String>();
        for (String label : labels) {
            if (label != null && !label.trim().isEmpty()) {
                normalized.add(label.toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private static boolean matches(org.bukkit.command.Command command, Set<String> labels) {
        if (command == null) {
            return false;
        }
        if (labels.contains(command.getName().toLowerCase(Locale.ROOT))) {
            return true;
        }
        for (String alias : command.getAliases()) {
            if (labels.contains(alias.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static final class DynamicCommand extends org.bukkit.command.Command {
        private final CommandRegistry registry;
        private final Object target;
        private final Permission commandPermission;
        private final Map<String, RegisteredSubCommand> methods = new LinkedHashMap<>();

        DynamicCommand(CommandRegistry registry, String name, String description, List<String> aliases, Object target) {
            super(name, description, "", aliases);
            this.registry = registry;
            this.target = target;
            this.commandPermission = target.getClass().getAnnotation(Permission.class);
            for (Method method : target.getClass().getDeclaredMethods()) {
                SubCommand sub = method.getAnnotation(SubCommand.class);
                if (sub != null) {
                    method.setAccessible(true);
                    RegisteredSubCommand registered = new RegisteredSubCommand(sub.value().toLowerCase(Locale.ROOT), method, permission(method));
                    methods.put(registered.name, registered);
                }
            }
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            RegisteredSubCommand command = select(args);
            if (command == null) {
                return false;
            }
            if (!hasPermission(sender, command.permission)) {
                sendNoPermission(sender, command.permission);
                return true;
            }
            try {
                command.method.invoke(target, bind(command.method, sender, args));
                return true;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                sender.sendMessage("§cCommand failed: " + (cause == null ? e.getMessage() : cause.getMessage()));
                return true;
            } catch (ReflectiveOperationException e) {
                sender.sendMessage("§cCommand failed: " + e.getMessage());
                return true;
            }
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            if (args.length == 1) {
                List<String> out = new ArrayList<>();
                String prefix = args[0].toLowerCase(Locale.ROOT);
                for (RegisteredSubCommand command : methods.values()) {
                    if (!command.name.isEmpty() && command.name.startsWith(prefix) && hasPermission(sender, command.permission)) {
                        out.add(command.name);
                    }
                }
                return out;
            }
            return Collections.emptyList();
        }

        private RegisteredSubCommand select(String[] args) {
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

        private Permission permission(Method method) {
            Permission permission = method.getAnnotation(Permission.class);
            return permission == null ? commandPermission : permission;
        }

        private Permission permission(RegisteredSubCommand command) {
            return command == null ? commandPermission : command.permission;
        }

        private boolean hasPermission(CommandSender sender, Permission permission) {
            return permission == null || permission.value().trim().isEmpty() || registry.permissionResolver.hasPermission(sender, permission.value());
        }

        private void sendNoPermission(CommandSender sender, Permission permission) {
            String message = permission == null ? "" : permission.message();
            if (message == null || message.trim().isEmpty()) {
                message = registry.noPermissionMessage;
            }
            if (message != null && !message.trim().isEmpty()) {
                sender.sendMessage(Colors.color(message));
            }
        }
    }

    private static final class RegisteredSubCommand {
        private final String name;
        private final Method method;
        private final Permission permission;

        private RegisteredSubCommand(String name, Method method, Permission permission) {
            this.name = name;
            this.method = method;
            this.permission = permission;
        }
    }
}
