package net.invisiblesmp.itemadd;

import org.bukkit.plugin.java.JavaPlugin;

public class ItemAddPlugin extends JavaPlugin {

    private static ItemAddPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        ItemAddCommand executor = new ItemAddCommand(this);
        getCommand("itemadd").setExecutor(executor);
        getCommand("itemadd").setTabCompleter(executor);

        getServer().getPluginManager().registerEvents(new HitListener(this), this);

        getLogger().info("ItemAdd включен.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ItemAdd выключен.");
    }

    public static ItemAddPlugin getInstance() {
        return instance;
    }
}
