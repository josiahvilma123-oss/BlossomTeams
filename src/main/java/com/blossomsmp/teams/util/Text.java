package com.blossomsmp.teams.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Colour codes and money formatting helpers. */
public final class Text {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T"};
    private static final double MAX_AMOUNT = 1_000_000_000_000_000D;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(ChatColor.COLOR_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private Text() {
    }

    /** Turns "&d&lHello &#FF69B4world" into a coloured component. */
    public static Component component(String input) {
        return LEGACY.deserialize(color(input));
    }

    /** Same as component(), without the italic that item names and lore get by default. */
    public static Component item(String input) {
        return component(input).decoration(TextDecoration.ITALIC, false);
    }

    /** Translates &-codes and &#RRGGBB hex colours. */
    public static String color(String input) {
        if (input == null) {
            return "";
        }
        Matcher matcher = HEX.matcher(input);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            StringBuilder replacement = new StringBuilder().append(ChatColor.COLOR_CHAR).append('x');
            for (char c : matcher.group(1).toCharArray()) {
                replacement.append(ChatColor.COLOR_CHAR).append(c);
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(out);
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }

    /** The text of a component without any colours. */
    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** $1,234.5 */
    public static String money(double amount) {
        DecimalFormat format = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return (amount < 0 ? "-$" : "$") + format.format(Math.abs(amount));
    }

    /** $1.2K, $3.5M */
    public static String shortMoney(double amount) {
        double abs = Math.abs(amount);
        int index = 0;
        while (abs >= 1000 && index < SUFFIXES.length - 1) {
            abs /= 1000;
            index++;
        }
        if (index == 0) {
            return money(amount);
        }
        String number = String.format(Locale.US, "%.1f", abs);
        if (number.endsWith(".0")) {
            number = number.substring(0, number.length() - 2);
        }
        return (amount < 0 ? "-$" : "$") + number + SUFFIXES[index];
    }

    /** Parses "100", "2.5k", "1m", "$1,000". Returns null if invalid or not positive. */
    public static Double parseAmount(String input) {
        if (input == null) {
            return null;
        }
        String s = input.toLowerCase(Locale.ROOT).replace(",", "").replace("$", "").trim();
        if (s.isEmpty()) {
            return null;
        }
        double multiplier = 1;
        char last = s.charAt(s.length() - 1);
        if (last == 'k') {
            multiplier = 1_000;
        } else if (last == 'm') {
            multiplier = 1_000_000;
        } else if (last == 'b') {
            multiplier = 1_000_000_000;
        }
        if (multiplier != 1) {
            s = s.substring(0, s.length() - 1);
        }
        if (!s.matches("\\d+(\\.\\d+)?|\\.\\d+")) {
            return null;
        }
        try {
            double value = Math.round(Double.parseDouble(s) * multiplier * 100.0) / 100.0;
            if (value <= 0 || value > MAX_AMOUNT) {
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
