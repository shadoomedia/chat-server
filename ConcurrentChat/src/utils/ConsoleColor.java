package utils;

public enum ConsoleColor {
    DEFAULT("\u001B[0m"), //white
    WHISPER("\u001B[96m"), //cyan
    ERROR_WARNING("\u001B[31m"), //red
    ADMIN("\u001B[33m"), //yellow
    CHAT_HISTORY("\u001B[38;5;240m"),
    GREEN("\u001B[32m"),
    BLUE("\u001B[34m"),
    PURPLE("\u001B[35m"),
    LIGHT_GRAY("\u001B[37m"),
    LIGHT_RED("\u001B[91m"),
    LIGHT_GREEN("\u001B[92m"),
    LIGHT_YELLOW("\u001B[93m"),
    LIGHT_BLUE("\u001B[94m"),
    LIGHT_PURPLE("\u001B[95m"),
    LIGHT_ORANGE("\u001B[38;5;208m"),
    LIGHT_PINK("\u001B[38;5;213m"),
    LIGHT_AQUA("\u001B[38;5;123m"),
    LIGHT_LAVENDER("\u001B[38;5;183m"),
    LIGHT_MINT("\u001B[38;5;156m"),
    LIGHT_PEACH("\u001B[38;5;203m"),
    LIGHT_SALMON("\u001B[38;5;217m");

    private final String code;

    ConsoleColor(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    // Get a random ConsoleColor
    public static ConsoleColor getRandomColor() {
        ConsoleColor[] colors = ConsoleColor.values();
        int randomIndex = 5 + (int) (Math.random() * colors.length-5);
        return colors[randomIndex];
    }
}
