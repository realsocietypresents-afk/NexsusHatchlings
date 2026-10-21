package com.nexuscraft.nexushatchlings;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class NexusHatchlingsPlugin extends JavaPlugin {

    private HatchlingConfig config;
    private HatchlingKeys keys;
    private TraitRegistry traitRegistry;
    private TraitManager traitManager;
    private HatchlingManager manager;
    private CrystalItem crystalItem;
    private HatchlingFirstJoinListener firstJoinListener;
    private BukkitTask reapplyTask;
    private BukkitTask scanTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.keys = new HatchlingKeys(this);
        this.config = new HatchlingConfig(this);
        config.load();

        this.traitRegistry = new TraitRegistry(this);
        traitRegistry.load();
        this.traitManager = new TraitManager(traitRegistry, keys, config);

        this.manager = new HatchlingManager(this, config, keys, traitManager);
        this.crystalItem = new CrystalItem(keys);

        getServer().getPluginManager().registerEvents(new CrystalThrowListener(keys, crystalItem), this);
        getServer().getPluginManager().registerEvents(new CrystalImpactListener(keys), this);
        getServer().getPluginManager().registerEvents(new HatchlingListener(config, manager, keys), this);
        getServer().getPluginManager().registerEvents(new TraitAbilityListener(traitManager), this);

        boolean firstJoinMessage = getConfig().getBoolean("settings.first-join-message", true);
        this.firstJoinListener = new HatchlingFirstJoinListener(getDataFolder(), getLogger(), config,
                traitRegistry, firstJoinMessage);
        firstJoinListener.load();
        getServer().getPluginManager().registerEvents(firstJoinListener, this);

        getCommand("nexushatchlings").setExecutor(
                new NexusHatchlingsCommand(this, config, manager, crystalItem, traitRegistry, traitManager));

        if (config.recipeEnabled()) {
            registerRecipe();
        }

        // Anything already tamed and loaded (a /reload, or this plugin being installed onto a
        // running server) is picked up immediately rather than waiting for the next scheduled tick.
        manager.reapplyAllLoaded();

        startReapplyTask();
        startScanTask();

        getLogger().info("NexusHatchlings enabled -- crystal tames any mob at scale " + config.startScale()
                + "x, growing to " + config.maxScale() + "x over " + config.daysToGrow() + " real day(s), "
                + traitRegistry.all().size() + " possible traits.");
    }

    private void registerRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(this, "hatchling-crystal");
        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, crystalItem.create());
        recipe.addIngredient(Material.AMETHYST_SHARD);
        recipe.addIngredient(Material.GOLD_INGOT);
        recipe.addIngredient(Material.SLIME_BALL);
        recipe.addIngredient(Material.ENDER_PEARL);
        Bukkit.addRecipe(recipe);
    }

    private void startReapplyTask() {
        if (reapplyTask != null) {
            reapplyTask.cancel();
        }
        long intervalTicks = 20L * 60 * Math.max(1, config.reapplyIntervalMinutes());
        this.reapplyTask = getServer().getScheduler().runTaskTimer(this,
                manager::reapplyAllLoaded, intervalTicks, intervalTicks);
    }

    private void startScanTask() {
        if (scanTask != null) {
            scanTask.cancel();
        }
        long intervalTicks = 20L * Math.max(1, config.crystalScanIntervalSeconds());
        CrystalScanTask task = new CrystalScanTask(this, config, keys, manager);
        this.scanTask = getServer().getScheduler().runTaskTimer(this, task, intervalTicks, intervalTicks);
    }

    @Override
    public void onDisable() {
        if (reapplyTask != null) {
            reapplyTask.cancel();
        }
        if (scanTask != null) {
            scanTask.cancel();
        }
        if (firstJoinListener != null) {
            firstJoinListener.close();
        }
        getLogger().info("NexusHatchlings disabled.");
    }
}
