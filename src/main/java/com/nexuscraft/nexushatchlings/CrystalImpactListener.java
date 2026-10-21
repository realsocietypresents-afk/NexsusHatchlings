package com.nexuscraft.nexushatchlings;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataContainer;

/**
 * Where a thrown crystal lands, it leaves behind a marked, undespawning item -- not a tamed mob
 * yet. {@link CrystalScanTask} is what actually reads a nearby mob and tames it, so a crystal that
 * lands nowhere near anything eligible just waits (and eventually expires) instead of taming
 * whatever happens to be standing on the impact block.
 */
public final class CrystalImpactListener implements Listener {

    private final HatchlingKeys keys;

    public CrystalImpactListener(HatchlingKeys keys) {
        this.keys = keys;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Snowball)) {
            return;
        }

        PersistentDataContainer projectilePdc = projectile.getPersistentDataContainer();
        if (!projectilePdc.has(keys.crystalMarker, HatchlingKeys.STRING)) {
            return;
        }

        String thrownBy = projectilePdc.get(keys.thrownBy, HatchlingKeys.STRING);
        Location dropLocation = projectile.getLocation();
        if (dropLocation == null || dropLocation.getWorld() == null) {
            return;
        }

        Item dropped = dropLocation.getWorld().dropItem(dropLocation, new CrystalItem(keys).create());
        dropped.setUnlimitedLifetime(true);
        dropped.setCustomName("§dHatchling Crystal");
        dropped.setCustomNameVisible(true);

        PersistentDataContainer itemPdc = dropped.getPersistentDataContainer();
        itemPdc.set(keys.crystalMarker, HatchlingKeys.STRING, "true");
        itemPdc.set(keys.droppedAt, HatchlingKeys.LONG, System.currentTimeMillis());
        if (thrownBy != null) {
            itemPdc.set(keys.thrownBy, HatchlingKeys.STRING, thrownBy);
        }

        Entity hitEntity = event.getHitEntity();
        if (hitEntity != null) {
            // A direct hit is still just a nearby-mob candidate for the scan task on its very next
            // pass -- we don't tame it here -- but removing the projectile immediately (rather than
            // waiting on vanilla) keeps a direct hit from ever visually lingering as a stuck snowball.
        }
        projectile.remove();
    }
}
