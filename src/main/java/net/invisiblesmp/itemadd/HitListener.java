package net.invisiblesmp.itemadd;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class HitListener implements Listener {

    private final ItemAddPlugin plugin;
    private final ItemDataUtil dataUtil;

    public HitListener(ItemAddPlugin plugin) {
        this.plugin = plugin;
        this.dataUtil = new ItemDataUtil(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        Entity damagerEntity = event.getDamager();
        Entity victimEntity = event.getEntity();

        // Срабатывает только на удар игрока по игроку.
        // Если нужно, чтобы работало и по мобам — замени "Player victim" на "LivingEntity victim".
        if (!(damagerEntity instanceof Player attacker)) return;
        if (!(victimEntity instanceof Player victim)) return;

        ItemStack item = attacker.getInventory().getItemInMainHand();
        if (item.getType().isAir()) return;

        List<String> effects = dataUtil.getEffects(item);
        List<String> commands = dataUtil.getCommands(item);
        if (effects.isEmpty() && commands.isEmpty()) return;

        for (String encoded : effects) {
            applyEffect(victim, encoded);
        }

        for (String cmdTemplate : commands) {
            runCommand(cmdTemplate, attacker, victim);
        }
    }

    private void applyEffect(Player victim, String encoded) {
        String[] parts = encoded.split(":");
        if (parts.length < 3) return;

        String id = parts[0];
        int durationSec;
        int amplifier;
        try {
            durationSec = Integer.parseInt(parts[1]);
            amplifier = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return;
        }
        boolean ambient = parts.length >= 4 && Boolean.parseBoolean(parts[3]);

        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(id));
        if (type == null) return;

        PotionEffect effect = new PotionEffect(type, durationSec * 20, amplifier, ambient, true, true);
        victim.addPotionEffect(effect);
    }

    private void runCommand(String cmdTemplate, Player attacker, Player victim) {
        String finalCmd = cmdTemplate
                .replace("%target%", victim.getName())
                .replace("%player%", attacker.getName());

        // Выполняется от лица консоли (полные права) — команда сработает даже если
        // у игрока нет прав на неё. Раздавай /itemadd только доверенным людям (permission itemadd.use).
        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd)
        );
    }
}
