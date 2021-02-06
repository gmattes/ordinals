package com.github.ordinals;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class OrdinalsFactory {
    public enum Gender { NEUTRAL, MASCULINE, FEMININE };

    private static final ConcurrentMap<Locale, OrdinalsFactory> factories = new ConcurrentHashMap<>();

    private final List<Rule> rules = new CopyOnWriteArrayList<>();

    public static OrdinalsFactory getInstance(final Locale locale) {
        return factories.computeIfAbsent(locale, OrdinalsFactory::new);
    }

    private OrdinalsFactory(final Locale locale) {
        final String resourceName = "ordinals-" + locale + ".txt";
        final List<String> source = ResourceReader.readAllLinesInResourceFile(resourceName);
        compile(source);
    }

    private Gender getGenderOf(final String g) {
        switch (g) {
            case "n": return Gender.NEUTRAL;
            case "m": return Gender.MASCULINE;
            case "f": return Gender.FEMININE;
            default: throw new RuntimeException("unexpected gender: " + g);
        }
    }

    private void compile(final List<String> source) {
        final AtomicInteger lineNo = new AtomicInteger(1);
        source.stream().forEach(l -> {
                final String[] components = l.split(";");
                if (components.length != 4) {
                    throw new RuntimeException("parse error:" + lineNo.get() + ": " + l);
                }
                final String rule     = components[0];
                final String suffix   = components[1];
                final String fullName = components[2];
                final String gender   = components[3];
                rules.add(rule.startsWith("%")
                          ? new ModuloRule(Integer.parseInt(rule.substring(1)), suffix, fullName, getGenderOf(gender))
                          : new ExactRule(Integer.parseInt(rule), suffix, fullName, getGenderOf(gender)));
                lineNo.getAndIncrement();
            });
    }

    private abstract class Rule {
        final String suffix;
        final String fullName;
        final Gender gender;

        Rule(final String suffix, final String fullName, final Gender gender) {
            this.suffix   = suffix;
            this.fullName = fullName;
            this.gender   = gender;
        }

        abstract boolean matches(int i);

        abstract String ruleToString();

        @Override public final String toString() {
            return "[rule: " + ruleToString() + ", suffix: " + suffix + ", fullName: " + fullName + ", gender: " + gender + "]";
        }
    }

    private final class ModuloRule extends Rule {
        final int congruent;

        ModuloRule(final int congruent, final String suffix, final String fullName, final Gender gender) {
            super(suffix, fullName, gender);
            this.congruent = congruent;
        }

        @Override boolean matches(final int i) {
            return congruent == i % 10;
        }

        @Override String ruleToString() {
            return "congruent to " + congruent + " modulo 10";
        }
    }

    private final class ExactRule extends Rule {
        final int ordinal;

        ExactRule(final int ordinal, final String suffix, final String fullName, final Gender gender) {
            super(suffix, fullName, gender);
            this.ordinal = ordinal;
        }

        @Override boolean matches(final int i) {
            return ordinal == i;
        }

        @Override String ruleToString() {
            return "equals to " + ordinal;
        }
    }

    public String getOrdinalWithSuffix(final int i) {
        return i + rules.stream().filter(r -> r.matches(i)).findFirst().get().suffix;
    }

    public String getOrdinalWithSuffix(final int i, final Gender g) {
        throw new RuntimeException("unimplemented");
    }

    public String getOrdinalFullName(final int i) {
        return rules.stream().filter(r -> r.matches(i)).findFirst().get().fullName;
    }

    public String getOrdinalFullName(final int i, final Gender g) {
        throw new RuntimeException("unimplemented");
    }

    /**
     * Get the ordinal suffix for the day of the month.
     *
     * @param dayOfTheMonth the day of the month.
     *
     * @throws IllegalArgumentException if the value of {@code dayOfTheMonth} is not a valid day of a month.
     *
     * @see https://stackoverflow.com/a/4011232/10030693
     */
    public static String getDayOfMonthSuffix(final int dayOfTheMonth) {
        if (dayOfTheMonth < 1 || dayOfTheMonth > 31) {
            throw new IllegalArgumentException("day of month must be between 1 and 31 inclusive: " + dayOfTheMonth);
        }

        if (dayOfTheMonth >= 11 && dayOfTheMonth <= 13) {
            return "th";
        }

        switch (dayOfTheMonth % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
    }

    /**
     * Produces the day of month suffix from a {@code Date}.
     *
     * @param date The {@code Date} of which the day of the month suffix is produced.
     *
     * @return suffix representing day of month.
     */
    public static String getDayOfMonthSuffix(final Date date) {
        return getDayOfMonthSuffix(getDayOfMonth(date));
    }

    /**
     * Produces the integer representing the day of the month, e.g., 17 in: January 17 2020.
     *
     * @param date The {@code Date} from which the day of the month is extracted.
     *
     * @return the day of the month for {@code date}.
     */
    public static int getDayOfMonth(final Date date) {
        if (date == null) {
            throw new NullPointerException("null: date");
        }

        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_MONTH);
    }
}
