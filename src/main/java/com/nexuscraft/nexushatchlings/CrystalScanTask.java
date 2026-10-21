package com.nexuscraft.nexushatchlings;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The background half of the crystal mechanic. A thrown crystal doesn't tame anything by itself --
 * it just lands and waits (see {@link CrystalImpactListener}). This task is what periodically looks
 * around every waiting crystal for the nearest eligible mob and actually tames it, the "the mob
 * picks it up" step of the whole "throw it, the mob picks it up" mechanic.
 */
public final class CrystalScanTask implements Runnable {

    private final JavaPlugin plugin;
    private final HatchlingConfig config;
    private final HatchlingKeys keys;
    private final HatchlingManager manager;

    public CrystalScanTask(JavaPlugin plugin, HatchlingConfig config, HatchlingKeys keys, HatchlingManager manager) {
        this.plugin = plugin;
        this.config = config;
        this.keys = keys;
        this.manager = manager;
    }

    @Override
    public void run() {
        scanOnce();
    }

    /** Package-private testing seam: exercised directly by a standalone test since the stub scheduler never actually fires runTaskTimer. */
    void scanOnce() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : new ArrayList<>(world.getEntities())) {
                if (!(entity instanceof Item)) {
                    continue;
                }
                Item item = (Item) entity;
                PersistentDataContainer pdc = item.getPersistentDataContainer();
                if (!pdc.has(keys.crystalMarker, HatchlingKeys.STRING)) {
                    continue;
                }

                try {
                    processWaitingCrystal(world, item, pdc);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                            "NexusHatchlings: couldn''t process a waiting crystal, leaving it for next scan.", e);
                }
            }
        }
    }

    private void processWaitingCrystal(World world, Item item, PersistentDataContainer pdc) {
        Long droppedAt = pdc.get(keys.droppedAt, HatchlingKeys.LONG);
        long ageSeconds = droppedAt == null ? 0 : (System.currentTimeMillis() - droppedAt) / 1000L;
        if (ageSeconds >= config.crystalExpirySeconds()) {
            item.remove();
            return;
        }

        Location location = item.getLocation();
        if (location == null) {
            return;
        }

        double radius = config.crystalPickupRadiusBlocks();
        List<Entity> nearby = new ArrayList<>(world.getNearbyEntities(location, radius, radius, radius));
        Mob target = null;
        for (Entity candidate : nearby) {
            if (manager.isEligible(candidate)) {
                target = (Mob) candidate;
                break;
            }
        }

        if (target == null) {
            return;
        }

        String thrownBy = pdc.get(keys.thrownBy, HatchlingKeys.STRING);
        UUID ownerId = parseUuid(thrownBy);
        if (ownerId == null) {
            // A crystal with no recoverable owner (shouldn't normally happen) can't tame anything --
            // let it sit until it expires rather than taming something to nobody.
            return;
        }

        List<TraitDefinition> rolledTraits = manager.tame(target, ownerId);
        item.remove();

        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null && owner.isOnline()) {
            String creature = target.getType().name().toLowerCase().replace('_', ' ');
            owner.sendMessage("§d[NexusHatchlings] §fTamed a " + creature + " at "
                    + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()
                    + " -- it'll grow up over time, staying loyal to you.");
            if (!rolledTraits.isEmpty()) {
                StringBuilder names = new StringBuilder();
                for (TraitDefinition trait : rolledTraits) {
                    if (names.length() > 0) {
                        names.append("§7, §d");
                    }
                    names.append(trait.displayName());
                }
                owner.sendMessage("§d[NexusHatchlings] §fIt rolled: §d" + names
                        + " §7(§f/nexushatchlings traits §7for details)");
            }
        }
    }

    private UUID parseUuid(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException badUuid) {
            return null;
        }
    }
}
