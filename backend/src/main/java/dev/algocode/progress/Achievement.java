package dev.algocode.progress;

import com.fasterxml.jackson.annotation.JsonFormat;

/** A badge the user can unlock. Achievements are derived from history, never stored. */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Achievement {
    FIRST_SOLVE("Hello, World", "Solve your first problem", "🎯"),
    FIVE_SOLVES("Warming Up", "Solve five problems", "🔥"),
    ALL_EASY("Easy Peasy", "Solve every Easy problem", "🍃"),
    FIRST_HARD("Hard Hat", "Solve a Hard problem", "⛑️"),
    CLEAN_SHEET("Clean Sheet", "Get Accepted on your first submission to a problem", "✨"),
    SPEED_DEMON("Speed Demon", "Beat 90% of accepted runtimes", "⚡"),
    MEMORY_MISER("Memory Miser", "Beat 90% of accepted memory usage", "🧠"),
    ON_A_ROLL("On a Roll", "Practice three days in a row", "📆"),
    UNSTOPPABLE("Unstoppable", "Practice seven days in a row", "🚀"),
    TINKERER("Tinkerer", "Save three scratchpads", "🧪");

    private final String title;
    private final String description;
    private final String icon;

    Achievement(String title, String description, String icon) {
        this.title = title;
        this.description = description;
        this.icon = icon;
    }

    public String getId() { return name(); }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
}
