package com.nexuscraft.nexushatchlings;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.nexuscraft.nexushatchlings.Kit.expect;

/** Start-up wiring: what onEnable registers and schedules for the dragons, what reload does, and what onDisable cleans up. */
public class PluginTest {

    static List<Long> dragonPeriods() {
        return new ArrayList<>(BukkitScheduler.PERIODS);
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("nxh-plugin").toFile();
        org.bukkit.plugin.java.JavaPlugin.DATA_FOLDER = dir;

        // 1. defaults
        Kit.reset();
        PluginManager.REGISTERED.clear();
        NexusHatchlingsPlugin plugin = new NexusHatchlingsPlugin();
        plugin.onEnable();

        PluginCommand dragon = plugin.getCommand("dragon");
        expect(dragon.getExecutor() instanceof DragonCommand, "the /dragon command has its executor");
        expect(dragon.getTabCompleter() instanceof DragonCommand, "the /dragon command has its tab completer");
        expect(dragon.getExecutor() == dragon.getTabCompleter(), "one object does both");
        expect(PluginManager.REGISTERED.stream().anyMatch(l -> l instanceof DragonListener), "the dragon listener is registered");
        expect(PluginManager.REGISTERED.stream().anyMatch(l -> l instanceof CrystalImpactListener), "the crystal impact listener is registered");
        expect(PluginManager.REGISTERED.stream().anyMatch(l -> l instanceof OwnerInteractListener), "the owner interaction listener is registered");

        List<Long> periods = dragonPeriods();
        expect(periods.contains(2L), "the movement timer runs every 2 ticks: " + periods);
        expect(periods.contains(10L), "the aura timer runs every 10 ticks: " + periods);
        expect(periods.contains(100L), "the scan timer runs every 100 ticks: " + periods);
        expect(periods.stream().filter(p -> p == 100L).count() == 2, "scan and egg timers both run every 100 ticks: " + periods);
        int withDragons = BukkitScheduler.HANDLES.size();

        // 2. reload cancels the old timers and starts new ones
        List<BukkitTask> before = new ArrayList<>(BukkitScheduler.HANDLES);
        NexusHatchlingsCommand cmd = (NexusHatchlingsCommand) plugin.getCommand("nexushatchlings").getExecutor();
        Kit.TestPlayer admin = Kit.online("Admin");
        Kit.TestPlayer nobody = Kit.online("Nobody");
        cmd.onCommand(nobody, plugin.getCommand("nexushatchlings"), "nexushatchlings", new String[]{"reload"});
        expect(BukkitScheduler.HANDLES.size() == withDragons, "a player without permission does not restart anything");
        admin.perms.add("nexushatchlings.admin");
        cmd.onCommand(admin, plugin.getCommand("nexushatchlings"), "nexushatchlings", new String[]{"reload"});
        int cancelled = 0;
        for (BukkitTask t : before) {
            if (t.isCancelled()) {
                cancelled++;
            }
        }
        expect(cancelled >= 4, "reload cancels the four dragon timers (" + cancelled + ")");
        expect(BukkitScheduler.HANDLES.size() >= withDragons + 4, "reload starts four new ones: " + BukkitScheduler.HANDLES.size() + " vs " + withDragons);

        // 3. disabling stops everything it started
        List<BukkitTask> live = new ArrayList<>();
        for (BukkitTask t : BukkitScheduler.HANDLES) {
            if (!t.isCancelled()) {
                live.add(t);
            }
        }
        plugin.onDisable();
        for (BukkitTask t : live) {
            expect(t.isCancelled(), "every timer is cancelled on disable");
        }

        // 4. the dragons switched off: no dragon timers at all
        Kit.reset();
        NexusHatchlingsPlugin quiet = new NexusHatchlingsPlugin();
        quiet.getConfig().set("dragons.enabled", false);
        quiet.onEnable();
        int off = BukkitScheduler.HANDLES.size();
        Kit.reset();
        NexusHatchlingsPlugin loud = new NexusHatchlingsPlugin();
        loud.onEnable();
        int on = BukkitScheduler.HANDLES.size();
        expect(on - off == 4, "dragons on add exactly four timers: " + on + " vs " + off);
        quiet.onDisable();
        loud.onDisable();

        // 5. egg timer period follows check-seconds
        Kit.reset();
        NexusHatchlingsPlugin slow = new NexusHatchlingsPlugin();
        slow.getConfig().set("dragons.egg.check-seconds", 9);
        slow.getConfig().set("dragons.movement.interval-ticks", 4);
        slow.onEnable();
        expect(BukkitScheduler.PERIODS.contains(180L), "egg timer 9 s -> 180 ticks: " + BukkitScheduler.PERIODS);
        expect(BukkitScheduler.PERIODS.contains(4L), "movement 4 ticks: " + BukkitScheduler.PERIODS);
        slow.onDisable();

        // 6. disabling without ever enabling must not throw (a failed enable still gets a disable)
        boolean threw = false;
        try {
            new NexusHatchlingsPlugin().onDisable();
        } catch (RuntimeException e) {
            threw = true;
        }
        expect(!threw, "onDisable on a never-enabled plugin is harmless");

        Kit.finish();
    }
}
