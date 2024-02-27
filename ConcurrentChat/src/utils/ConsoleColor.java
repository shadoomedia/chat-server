package utils;

public enum ConsoleColor {
    RESET("\u001B[0m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m"),
    PURPLE("\u001B[35m"),
    CYAN("\u001B[36m"),
    LIGHT_GRAY("\u001B[37m"),
    DARK_GRAY("\u001B[90m"),
    LIGHT_RED("\u001B[91m"),
    LIGHT_GREEN("\u001B[92m"),
    LIGHT_YELLOW("\u001B[93m"),
    LIGHT_BLUE("\u001B[94m"),
    LIGHT_PURPLE("\u001B[95m"),
    LIGHT_CYAN("\u001B[96m");

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
        int randomIndex = (int) (Math.random() * colors.length);
        return colors[randomIndex];
    }
}
