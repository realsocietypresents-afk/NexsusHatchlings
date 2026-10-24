package com.nexuscraft.nexushatchlings;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * {@code /dragon}. The part that matters is the command word: every dragon has its own, and {@code /dragon <word>} sends it at the
 * nearest player, {@code /dragon <word> <player>} at that player, {@code /dragon <word> all} at everyone near it.
 */
public final class DragonCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("help", "list", "word", "attack", "stop", "color", "egg");
    private static final List<String> EGG_SUBS = List.of("give", "hatch", "spawn", "list");

    private final DragonService dragons;
    private final DragonEggs eggs;

    public DragonCommand(DragonService dragons, DragonEggs eggs) {
        this.dragons = dragons;
        this.eggs = eggs;
    }

    private static boolean admin(CommandSender s) {
        return s.hasPermission("nexushatchlings.admin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            for (String line : DragonText.help(admin(sender))) {
                sender.sendMessage(line);
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list", "info" -> {
                return list(sender);
            }
            case "word" -> {
                return word(sender, args);
            }
            case "attack" -> {
                if (args.length < 2) {
                    sender.sendMessage(DragonText.PREFIX + "Usage: /dragon attack <word> [player|all]");
                    return true;
                }
                return attack(sender, args[1], args.length >= 3 ? args[2] : null);
            }
            case "stop" -> {
                return stop(sender, args.length >= 2 ? args[1] : null);
            }
            case "color", "colour" -> {
                return color(sender, args);
            }
            case "egg" -> {
                return egg(sender, args);
            }
            default -> {
                // Not one of the sub-commands: it is a dragon's own word.
                return attack(sender, args[0], args.length >= 2 ? args[1] : null);
            }
        }
    }

    private Player playerOnly(CommandSender sender) {
        if (sender instanceof Player p) {
            return p;
        }
        sender.sendMessage("§cRun this in game.");
        return null;
    }

    private boolean list(CommandSender sender) {
        Player player = playerOnly(sender);
        if (player == null) {
            return true;
        }
        List<EnderDragon> mine = dragons.dragonsOf(player.getUniqueId());
        if (mine.isEmpty()) {
            sender.sendMessage(DragonText.PREFIX + "You have no dragons loaded near enough to count. Put a Dragon Egg on a lit campfire to get one.");
            return true;
        }
        sender.sendMessage(DragonText.PREFIX + "Your dragons:");
        for (EnderDragon d : mine) {
            sender.sendMessage("§7 - " + dragons.describe(d));
        }
        return true;
    }

    private boolean word(CommandSender sender, String[] args) {
        Player player = playerOnly(sender);
        if (player == null) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(DragonText.PREFIX + "Usage: /dragon word <current word> <new word>");
            return true;
        }
        EnderDragon d = dragons.byWord(player.getUniqueId(), args[1]);
        if (d == null) {
            sender.sendMessage(DragonText.PREFIX + "You have no dragon with the word \"" + args[1] + "\" nearby.");
            return true;
        }
        DragonService.WordResult result = dragons.setWord(d, player.getUniqueId(), args[2]);
        switch (result) {
            case OK -> sender.sendMessage(DragonText.PREFIX + "Your dragon answers to §f" + CommandWords.normalize(args[2]) + "§f now.");
            case INVALID -> sender.sendMessage(DragonText.PREFIX + "A word is " + CommandWords.MIN_LENGTH + " to " + CommandWords.MAX_LENGTH
                    + " letters, nothing else, and not one of: " + String.join(", ", new java.util.TreeSet<>(CommandWords.RESERVED)) + ".");
            case TAKEN -> sender.sendMessage(DragonText.PREFIX + "Another dragon of yours already has that word.");
            case NOT_OWNER -> sender.sendMessage(DragonText.PREFIX + "That dragon is not yours.");
        }
        return true;
    }

    private boolean attack(CommandSender sender, String word, String who) {
        Player player = playerOnly(sender);
        if (player == null) {
            return true;
        }
        EnderDragon d = dragons.byWord(player.getUniqueId(), word);
        if (d == null) {
            sender.sendMessage(DragonText.PREFIX + "You have no dragon with the word \"" + word + "\" nearby. /dragon list shows your words.");
            return true;
        }
        Targeting.Mode mode = who == null ? Targeting.Mode.NEAREST
                : who.equalsIgnoreCase("all") ? Targeting.Mode.ALL : Targeting.Mode.NAMED;
        DragonService.OrderResult result = dragons.attack(player, d, mode, mode == Targeting.Mode.NAMED ? who : null);
        if (result.status() != DragonService.Status.STARTED) {
            sender.sendMessage(DragonText.orderFailure(result, word, mode == Targeting.Mode.NAMED ? who : null));
            return true;
        }
        sender.sendMessage(DragonText.PREFIX + "Your dragon roars and goes after " + DragonText.joinNames(result.names()) + ".");
        return true;
    }

    private boolean stop(CommandSender sender, String word) {
        Player player = playerOnly(sender);
        if (player == null) {
            return true;
        }
        int stopped = 0;
        if (word != null) {
            EnderDragon d = dragons.byWord(player.getUniqueId(), word);
            if (d == null) {
                sender.sendMessage(DragonText.PREFIX + "You have no dragon with the word \"" + word + "\" nearby.");
                return true;
            }
            stopped = dragons.stop(d) ? 1 : 0;
        } else {
            for (EnderDragon d : dragons.dragonsOf(player.getUniqueId())) {
                if (dragons.stop(d)) {
                    stopped++;
                }
            }
        }
        sender.sendMessage(DragonText.PREFIX + (stopped == 0 ? "None of your dragons was fighting." : "Called off " + stopped + " dragon(s)."));
        return true;
    }

    private boolean color(CommandSender sender, String[] args) {
        if (!admin(sender)) {
            sender.sendMessage("§cYou don't have permission to do that. Right-click a dragon with a dye instead.");
            return true;
        }
        Player player = playerOnly(sender);
        if (player == null) {
            return true;
        }
        DragonColor color = args.length >= 2 ? DragonColor.byId(args[1]) : null;
        if (color == null) {
            StringBuilder ids = new StringBuilder();
            for (DragonColor c : DragonColor.values()) {
                ids.append(ids.length() == 0 ? "" : ", ").append(c.id());
            }
            sender.sendMessage(DragonText.PREFIX + "Usage: /dragon color <" + ids + "> (changes the dragon nearest you)");
            return true;
        }
        EnderDragon nearest = null;
        double best = 16.0;
        Location here = player.getLocation();
        for (EnderDragon d : dragons.loaded()) {
            Location at = d.getLocation();
            if (at != null && here != null && at.getWorld() != null && here.getWorld() != null
                    && at.getWorld().getName().equals(here.getWorld().getName()) && at.distance(here) <= best) {
                best = at.distance(here);
                nearest = d;
            }
        }
        if (nearest == null) {
            sender.sendMessage(DragonText.PREFIX + "No dragon within 16 blocks.");
            return true;
        }
        dragons.setColor(nearest, color);
        sender.sendMessage(DragonText.PREFIX + "It is " + color.display().toLowerCase() + " now.");
        return true;
    }

    private boolean egg(CommandSender sender, String[] args) {
        if (!admin(sender)) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }
        String what = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (what) {
            case "give" -> {
                Player target = args.length >= 3 ? Bukkit.getPlayerExact(args[2]) : sender instanceof Player p ? p : null;
                if (target == null) {
                    sender.sendMessage(DragonText.PREFIX + "Usage: /dragon egg give <player> (they must be online)");
                    return true;
                }
                target.getInventory().addItem(new ItemStack(Material.DRAGON_EGG));
                sender.sendMessage(DragonText.PREFIX + "Gave " + target.getName() + " a Dragon Egg.");
                return true;
            }
            case "hatch" -> {
                Player player = playerOnly(sender);
                if (player == null) {
                    return true;
                }
                Block block = eggs.nearest(player.getLocation(), 10.0);
                if (block == null) {
                    sender.sendMessage(DragonText.PREFIX + "No tracked egg within 10 blocks. (An egg is tracked once it is placed or poked while warm.)");
                    return true;
                }
                eggs.hatch(DragonEggs.posOf(block), block);
                return true;
            }
            case "spawn" -> {
                Player player = playerOnly(sender);
                if (player == null) {
                    return true;
                }
                dragons.spawnBaby(player.getLocation());
                sender.sendMessage(DragonText.PREFIX + "A wild baby dragon stands at your feet.");
                return true;
            }
            case "list" -> {
                sender.sendMessage(DragonText.PREFIX + eggs.size() + " egg(s) tracked, " + dragons.loaded().size() + " dragon(s) loaded.");
                return true;
            }
            default -> {
                sender.sendMessage(DragonText.PREFIX + "Usage: /dragon egg <give [player]|hatch|spawn|list>");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length <= 1) {
            out.addAll(SUBS);
            addWords(sender, out);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "word", "attack", "stop" -> addWords(sender, out);
                case "color", "colour" -> {
                    for (DragonColor c : DragonColor.values()) {
                        out.add(c.id());
                    }
                }
                case "egg" -> out.addAll(EGG_SUBS);
                default -> addTargets(out);
            }
        } else if (args.length == 3) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (first.equals("attack")) {
                addTargets(out);
            } else if (first.equals("egg") && args[1].equalsIgnoreCase("give")) {
                addPlayers(out);
            }
        }
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(prefix));
        return out;
    }

    private void addWords(CommandSender sender, List<String> out) {
        if (sender instanceof Player p) {
            UUID id = p.getUniqueId();
            for (EnderDragon d : dragons.dragonsOf(id)) {
                String w = dragons.wordOf(d);
                if (w != null) {
                    out.add(w);
                }
            }
        }
    }

    private void addTargets(List<String> out) {
        out.add("all");
        addPlayers(out);
    }

    private void addPlayers(List<String> out) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            out.add(p.getName());
        }
    }
}
