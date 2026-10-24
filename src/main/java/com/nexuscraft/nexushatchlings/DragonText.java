package com.nexuscraft.nexushatchlings;

import java.util.List;

/** The words the dragon system says to players, in one place. */
public final class DragonText {

    private DragonText() {
    }

    public static final String PREFIX = "§d[Dragons] §f";

    public static String joinNames(List<String> names) {
        if (names.isEmpty()) {
            return "";
        }
        if (names.size() == 1) {
            return names.get(0);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
    }

    /** Why an attack order did not start. */
    public static String orderFailure(DragonService.OrderResult r, String word, String named) {
        return switch (r.status()) {
            case STARTED -> "";
            case DISABLED -> PREFIX + "Dragons are not allowed to attack on this server.";
            case NOT_OWNER -> PREFIX + "That dragon is not yours.";
            case NOT_ADULT -> PREFIX + "Your dragon is not fully grown yet. It cannot fight until it is.";
            case BUSY -> PREFIX + "Your dragon is carrying a rider and cannot be sent anywhere.";
            case WRONG_WORLD -> PREFIX + "Your dragon is in another world and cannot hear you.";
            case FAR_AWAY -> PREFIX + "Your dragon is too far away to hear you.";
            case NO_TARGET -> switch (r.reason()) {
                case NOT_FOUND -> PREFIX + "There is nobody called " + (named == null ? "that" : named) + " close enough to see.";
                case TOO_FAR -> PREFIX + (named == null ? "That player" : named) + " is too far from your dragon.";
                case OWNER -> PREFIX + "Your dragon will not go after you.";
                case ALLY -> PREFIX + (named == null ? "That player" : named) + " stands with you, and your dragon will not touch them.";
                case CANNOT_BE_HURT -> PREFIX + (named == null ? "That player" : named) + " cannot be hurt right now.";
                default -> PREFIX + "There is nobody near your dragon to go after.";
            };
        };
    }

    public static String mountFailure(DragonService.MountResult r) {
        return switch (r) {
            case OK -> "";
            case DISABLED -> PREFIX + "Riding dragons is switched off on this server.";
            case WILD -> PREFIX + "This dragon is wild. Tame it with a Hatchling Crystal first.";
            case NOT_OWNER -> PREFIX + "This dragon is not yours to ride.";
            case TOO_YOUNG -> PREFIX + "It is too young to carry you. It has to be able to fly first.";
            case OCCUPIED -> PREFIX + "Somebody is already riding it.";
        };
    }

    public static List<String> help(boolean admin) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        lines.add("§8§m                                        ");
        lines.add("§d§lDragons");
        lines.add("§7Put a Dragon Egg on a lit campfire and wait. The baby is wild: throw a Hatchling Crystal at it to tame it.");
        lines.add("§7Right-click a dragon with a dye to change its colour. It walks while small, flies when grown enough to carry you");
        lines.add("§7(right-click it to climb on, right-click in the air to start or stop flying, sneak to get off), and fights once fully grown.");
        lines.add(" ");
        lines.add("§6/dragon <word> §7- your dragon attacks the nearest player close to it");
        lines.add("§6/dragon <word> <player> §7- it goes after that player");
        lines.add("§6/dragon <word> all §7- it goes after every player near it");
        lines.add("§6/dragon stop [word] §7- call it off");
        lines.add("§6/dragon list §7- your dragons and their words");
        lines.add("§6/dragon word <word> <new word> §7- give a dragon a different word (3-16 letters)");
        lines.add("§7Your own dragons and anyone allied with you in NexusHouses are never attacked.");
        if (admin) {
            lines.add("§6/dragon color <word> <colour> §7- change a colour (staff)");
            lines.add("§6/dragon egg give [player] | hatch | spawn §7- staff tools");
        }
        lines.add("§8§m                                        ");
        return lines;
    }
}
