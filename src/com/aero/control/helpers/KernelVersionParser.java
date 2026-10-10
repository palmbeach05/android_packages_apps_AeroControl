package com.aero.control.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and formats the single-line contents of {@code /proc/version} into the
 * multi-line "release / builder / build id / date" form shown on the Device
 * Info screen. Pure string parsing with no file I/O or shell dependency, so it
 * is kept separate from the shell-session plumbing in {@link shellHelper}.
 */
final class KernelVersionParser {
    static final String NO_DATA_FOUND = "Unavailable";

    private static final Pattern KERNEL_VERSION_PREFIX_PATTERN =
            Pattern.compile("^Linux\\s+version\\s+(\\S+)\\s+");
    private static final Pattern KERNEL_BUILD_PATTERN = Pattern.compile("^(#\\S+)(?:\\s+(.*))?$");
    private static final Pattern KERNEL_DATE_START_PATTERN = Pattern.compile(
            "(?:^|\\s)(Mon|Tue|Wed|Thu|Fri|Sat|Sun)\\s+");

    private KernelVersionParser() {
    }

    /**
     * Formats /proc/version while retaining the raw value if its structure is unfamiliar.
     */
    static String format(String procVersion) {
        if (procVersion == null || procVersion.trim().length() == 0) {
            return NO_DATA_FOUND;
        }
        String raw = procVersion.trim();
        Matcher prefix = KERNEL_VERSION_PREFIX_PATTERN.matcher(raw);
        if (!prefix.find()) return raw;

        String release = prefix.group(1);
        int builderStart = skipWhitespace(raw, prefix.end());
        int builderEnd = findClosingParenthesis(raw, builderStart);
        if (builderEnd < 0) return raw;
        String builderDetails = raw.substring(builderStart + 1, builderEnd).trim();
        if (builderDetails.length() == 0) return raw;
        String builder = builderDetails.split("\\s+", 2)[0];

        int compilerStart = skipWhitespace(raw, builderEnd + 1);
        int compilerEnd = findClosingParenthesis(raw, compilerStart);
        if (compilerEnd < 0) return raw;
        String buildMetadata = raw.substring(compilerEnd + 1).trim();
        Matcher build = KERNEL_BUILD_PATTERN.matcher(buildMetadata);
        if (!build.matches()) return raw;

        StringBuilder formatted = new StringBuilder(release)
                .append('\n').append(builder).append(' ').append(build.group(1));
        String flagsAndDate = build.group(2);
        if (flagsAndDate == null || flagsAndDate.length() == 0) return formatted.toString();

        Matcher dateStart = KERNEL_DATE_START_PATTERN.matcher(flagsAndDate);
        if (!dateStart.find()) {
            return formatted.append('\n').append(flagsAndDate).toString();
        }
        String flags = flagsAndDate.substring(0, dateStart.start()).trim();
        String date = flagsAndDate.substring(dateStart.start()).trim();
        if (flags.length() > 0) formatted.append('\n').append(flags);
        if (date.length() > 0) formatted.append('\n').append(date);
        return formatted.toString();
    }

    private static int skipWhitespace(String value, int position) {
        while (position < value.length() && Character.isWhitespace(value.charAt(position))) {
            position++;
        }
        return position;
    }

    private static int findClosingParenthesis(String value, int openingPosition) {
        if (openingPosition >= value.length() || value.charAt(openingPosition) != '(') return -1;
        int depth = 0;
        for (int i = openingPosition; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '(') depth++;
            else if (character == ')' && --depth == 0) return i;
        }
        return -1;
    }
}
