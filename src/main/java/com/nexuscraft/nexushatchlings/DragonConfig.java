package com.nexuscraft.nexushatchlings;

import org.bukkit.configuration.ConfigurationSection;

import java.util.function.Consumer;

/** Everything under {@code dragons:} in config.yml, with each number kept inside a sensible range (and a warning when one had to be moved). */
public final class DragonConfig {

    public boolean enabled = true;

    // eggs
    public double hatchMinutes = 15.0;
    public int hatchCheckSeconds = 5;

    // growth stages (fractions of full growth)
    public double flyAt = 0.5;
    public double adultAt = 1.0;

    // movement
    public int moveIntervalTicks = 2;
    public double walkSpeed = 3.0;
    public double followFlySpeed = 10.0;
    public double rideSpeed = 14.0;
    public double descendSpeed = 6.0;
    public double wanderRadius = 6.0;
    public boolean wander = true;
    public double yawOffset = 180.0;

    // riding
    public boolean rideEnabled = true;
    public boolean rideOwnerOnly = true;
    public int rideClearance = 3;
    public int slowFallSeconds = 6;

    // looks
    public boolean glow = true;
    public boolean aura = true;
    public int auraIntervalTicks = 10;

    // attacks
    public boolean attackEnabled = true;
    public double commandRange = 200.0;
    public double targetRange = 64.0;
    public double strikeRange = 22.0;
    public double attackSpeed = 14.0;
    public double hoverHeight = 6.0;
    public double damage = 8.0;
    public int intervalTicks = 30;
    public int fireSeconds = 5;
    public int durationSeconds = 45;

    // allies
    public boolean useHouses = true;

    public long hatchMillis() {
        return Math.round(hatchMinutes * 60_000.0);
    }

    /** Reads the section (it may be null, which means all defaults). Each number outside its range is moved inside and reported. */
    public void load(ConfigurationSection root, Consumer<String> warn) {
        ConfigurationSection c = root;
        enabled = bool(c, "dragons.enabled", true);

        hatchMinutes = num(c, "dragons.egg.hatch-minutes", 15.0, 0.1, 1440.0, warn);
        hatchCheckSeconds = (int) num(c, "dragons.egg.check-seconds", 5, 1, 120, warn);

        double fly = num(c, "dragons.stages.fly-at-percent", 50.0, 0.0, 100.0, warn) / 100.0;
        double adult = num(c, "dragons.stages.adult-at-percent", 100.0, 0.0, 100.0, warn) / 100.0;
        if (adult < fly) {
            warn.accept("dragons.stages.adult-at-percent is lower than fly-at-percent; using " + (int) Math.round(fly * 100) + " for both.");
            adult = fly;
        }
        flyAt = fly;
        adultAt = adult;

        moveIntervalTicks = (int) num(c, "dragons.movement.interval-ticks", 2, 1, 20, warn);
        walkSpeed = num(c, "dragons.movement.walk-speed", 3.0, 0.5, 20.0, warn);
        followFlySpeed = num(c, "dragons.movement.follow-fly-speed", 10.0, 1.0, 40.0, warn);
        descendSpeed = num(c, "dragons.movement.descend-speed", 6.0, 1.0, 40.0, warn);
        wander = bool(c, "dragons.movement.wild-wander", true);
        wanderRadius = num(c, "dragons.movement.wander-radius", 6.0, 1.0, 32.0, warn);
        yawOffset = num(c, "dragons.movement.yaw-offset", 180.0, -360.0, 360.0, warn);

        rideEnabled = bool(c, "dragons.ride.enabled", true);
        rideOwnerOnly = bool(c, "dragons.ride.owner-only", true);
        rideSpeed = num(c, "dragons.ride.speed", 14.0, 1.0, 40.0, warn);
        rideClearance = (int) num(c, "dragons.ride.clearance-blocks", 3, 1, 12, warn);
        slowFallSeconds = (int) num(c, "dragons.ride.slow-fall-seconds", 6, 0, 60, warn);

        glow = bool(c, "dragons.look.glow", true);
        aura = bool(c, "dragons.look.aura", true);
        auraIntervalTicks = (int) num(c, "dragons.look.aura-interval-ticks", 10, 2, 100, warn);

        attackEnabled = bool(c, "dragons.attack.enabled", true);
        commandRange = num(c, "dragons.attack.command-range", 200.0, 10.0, 1000.0, warn);
        targetRange = num(c, "dragons.attack.target-range", 64.0, 8.0, 256.0, warn);
        strikeRange = num(c, "dragons.attack.strike-range", 22.0, 4.0, 64.0, warn);
        attackSpeed = num(c, "dragons.attack.speed", 14.0, 1.0, 40.0, warn);
        hoverHeight = num(c, "dragons.attack.hover-height", 6.0, 0.0, 30.0, warn);
        damage = num(c, "dragons.attack.damage", 8.0, 0.5, 100.0, warn);
        intervalTicks = (int) num(c, "dragons.attack.interval-ticks", 30, 5, 400, warn);
        fireSeconds = (int) num(c, "dragons.attack.fire-seconds", 5, 0, 60, warn);
        durationSeconds = (int) num(c, "dragons.attack.duration-seconds", 45, 5, 600, warn);

        useHouses = bool(c, "dragons.allies.use-nexushouses", true);
    }

    private static boolean bool(ConfigurationSection c, String path, boolean def) {
        return c == null ? def : c.getBoolean(path, def);
    }

    private static double num(ConfigurationSection c, String path, double def, double min, double max, Consumer<String> warn) {
        double value = c == null ? def : c.getDouble(path, def);
        if (Double.isNaN(value) || value < min || value > max) {
            double fixed = Double.isNaN(value) ? def : Math.max(min, Math.min(max, value));
            warn.accept(path + " must be between " + trim(min) + " and " + trim(max) + "; using " + trim(fixed) + ".");
            return fixed;
        }
        return value;
    }

    private static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
