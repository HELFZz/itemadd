package net.invisiblesmp.itemadd;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Хранит список эффектов и команд прямо в NBT предмета (PersistentDataContainer),
 * так что данные переживают перезапуск сервера, и умеет перерисовывать лор предмета,
 * дописывая туда человекочитаемый список эффектов/команд.
 */
public class ItemDataUtil {

    // Символ-разделитель, который почти невозможно встретить в обычном тексте
    private static final String DELIM = "\u241F";

    private final NamespacedKey effectsKey;
    private final NamespacedKey commandsKey;
    private final NamespacedKey originalLoreKey;
    private final NamespacedKey hasOriginalLoreKey;

    public ItemDataUtil(ItemAddPlugin plugin) {
        this.effectsKey = new NamespacedKey(plugin, "effects");
        this.commandsKey = new NamespacedKey(plugin, "commands");
        this.originalLoreKey = new NamespacedKey(plugin, "original_lore");
        this.hasOriginalLoreKey = new NamespacedKey(plugin, "has_original_lore");
    }

    public List<String> getEffects(ItemStack item) {
        return getList(item, effectsKey);
    }

    public List<String> getCommands(ItemStack item) {
        return getList(item, commandsKey);
    }

    /** Добавляет эффект и сразу перерисовывает лор предмета. */
    public void addEffect(ItemStack item, String encoded) {
        saveOriginalLoreIfAbsent(item);
        addToList(item, effectsKey, encoded);
        rebuildLore(item);
    }

    /** Добавляет команду и сразу перерисовывает лор предмета. */
    public void addCommand(ItemStack item, String command) {
        saveOriginalLoreIfAbsent(item);
        addToList(item, commandsKey, command);
        rebuildLore(item);
    }

    /** Полностью снимает эффекты/команды и возвращает лор к исходному виду. */
    public void clear(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(effectsKey);
        pdc.remove(commandsKey);

        List<String> originalLore = getOriginalLore(item);
        pdc.remove(originalLoreKey);
        pdc.remove(hasOriginalLoreKey);

        meta.setLore(originalLore.isEmpty() ? null : originalLore);
        item.setItemMeta(meta);
    }

    /** Перестраивает лор: исходный текст + блок ItemAdd со списком эффектов/команд. */
    public void rebuildLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = new ArrayList<>(getOriginalLore(item));
        List<String> effects = getEffects(item);
        List<String> commands = getCommands(item);

        if (!effects.isEmpty() || !commands.isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "――――――――――――");
            lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "На удар:");

            for (String encoded : effects) {
                lore.add(ChatColor.GRAY + " • " + formatEffect(encoded));
            }
            for (String cmd : commands) {
                lore.add(ChatColor.GRAY + " • " + ChatColor.YELLOW + "/" + cmd);
            }
        }

        meta.setLore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
    }

    private String formatEffect(String encoded) {
        String[] parts = encoded.split(":");
        if (parts.length < 3) return encoded;

        String id = parts[0];
        String durationSec = parts[1];
        int amplifier;
        try {
            amplifier = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            amplifier = 0;
        }

        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(id));
        String niceName = (type != null) ? prettify(id) : id;

        return ChatColor.AQUA + niceName + ChatColor.GRAY
                + " (" + durationSec + "с, ур. " + (amplifier + 1) + ")";
    }

    private String prettify(String id) {
        String[] words = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    // ---------- внутренняя работа со "старым" (исходным) лором ----------

    private void saveOriginalLoreIfAbsent(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(hasOriginalLoreKey, PersistentDataType.BYTE)) return; // уже сохранён однажды

        List<String> currentLore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        String joined = String.join(DELIM, currentLore == null ? List.of() : currentLore);

        pdc.set(originalLoreKey, PersistentDataType.STRING, joined);
        pdc.set(hasOriginalLoreKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    private List<String> getOriginalLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new ArrayList<>();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String raw = pdc.get(originalLoreKey, PersistentDataType.STRING);
        List<String> list = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            for (String part : raw.split(DELIM, -1)) {
                list.add(part);
            }
        }
        return list;
    }

    // ---------- общие хелперы списков в PDC ----------

    private List<String> getList(ItemStack item, NamespacedKey key) {
        List<String> list = new ArrayList<>();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return list;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String raw = pdc.get(key, PersistentDataType.STRING);
        if (raw != null && !raw.isEmpty()) {
            for (String part : raw.split(DELIM)) {
                if (!part.isEmpty()) list.add(part);
            }
        }
        return list;
    }

    private void addToList(ItemStack item, NamespacedKey key, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String raw = pdc.get(key, PersistentDataType.STRING);
        String newRaw = (raw == null || raw.isEmpty()) ? value : raw + DELIM + value;

        pdc.set(key, PersistentDataType.STRING, newRaw);
        item.setItemMeta(meta);
    }
}
