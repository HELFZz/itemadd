package net.invisiblesmp.itemadd;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemAddCommand implements CommandExecutor, TabCompleter {

    private final ItemDataUtil dataUtil;

    public ItemAddCommand(ItemAddPlugin plugin) {
        this.dataUtil = new ItemDataUtil(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команду можно использовать только в игре.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Возьми предмет в руку.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "effect" -> handleEffect(player, item, args);
            case "command" -> handleCommand(player, item, args);
            case "clear" -> {
                dataUtil.clear(item);
                player.sendMessage(ChatColor.GREEN + "Все эффекты и команды предмета очищены.");
            }
            case "info" -> handleInfo(player, item);
            default -> sendUsage(player);
        }
        return true;
    }

    /** /itemadd effect <эффект> <секунды> <амплификатор> [ambient true/false] */
    private void handleEffect(Player player, ItemStack item, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED
                    + "Использование: /itemadd effect <эффект> <секунды> <амплификатор> [ambient true/false]");
            player.sendMessage(ChatColor.GRAY + "Пример: /itemadd effect minecraft:haste 10 1 true");
            return;
        }

        String rawId = args[1].toLowerCase().replace("minecraft:", "");
        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(rawId));
        if (type == null) {
            player.sendMessage(ChatColor.RED + "Неизвестный эффект: " + args[1]);
            return;
        }

        int durationSec;
        int amplifier;
        try {
            durationSec = Integer.parseInt(args[2]);
            amplifier = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Секунды и амплификатор должны быть целыми числами.");
            return;
        }

        boolean ambient = args.length >= 5 && Boolean.parseBoolean(args[4]);

        String encoded = rawId + ":" + durationSec + ":" + amplifier + ":" + ambient;
        dataUtil.addEffect(item, encoded);

        player.sendMessage(ChatColor.GREEN + "Эффект " + rawId + " добавлен на удар ("
                + durationSec + "с, уровень " + (amplifier + 1) + ").");
    }

    /** /itemadd command <команда, можно с %target% и %player%> */
    private void handleCommand(Player player, ItemStack item, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /itemadd command <команда с %target% и %player%>");
            player.sendMessage(ChatColor.GRAY + "Пример: /itemadd command ban %target% Читы");
            return;
        }

        String cmd = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (cmd.startsWith("/")) cmd = cmd.substring(1);

        dataUtil.addCommand(item, cmd);
        player.sendMessage(ChatColor.GREEN + "Команда добавлена на удар: /" + cmd);
        player.sendMessage(ChatColor.GRAY + "Плейсхолдеры: %target% — кого ударили, %player% — тот, кто ударил.");
    }

    private void handleInfo(Player player, ItemStack item) {
        player.sendMessage(ChatColor.GOLD + "=== Эффекты предмета на удар ===");
        List<String> effects = dataUtil.getEffects(item);
        if (effects.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + " (пусто)");
        } else {
            for (String e : effects) player.sendMessage(ChatColor.GRAY + " - " + e);
        }

        player.sendMessage(ChatColor.GOLD + "=== Команды предмета на удар ===");
        List<String> commands = dataUtil.getCommands(item);
        if (commands.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + " (пусто)");
        } else {
            for (String c : commands) player.sendMessage(ChatColor.GRAY + " - /" + c);
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== ItemAdd ===");
        player.sendMessage(ChatColor.YELLOW + "/itemadd effect <эффект> <секунды> <амплификатор> [ambient]");
        player.sendMessage(ChatColor.YELLOW + "/itemadd command <команда>");
        player.sendMessage(ChatColor.YELLOW + "/itemadd clear");
        player.sendMessage(ChatColor.YELLOW + "/itemadd info");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(Arrays.asList("effect", "command", "clear", "info"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("effect")) {
            for (var key : Registry.EFFECT.stream().toList()) {
                // подсказки эффектов необязательны, оставлено пустым для скорости
            }
        }
        return options;
    }
}
