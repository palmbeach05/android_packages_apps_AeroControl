package com.aero.control.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Selects an advertised I/O scheduler and verifies the kernel's bracketed readback. */
public final class IoSchedulerController {
    private final SysfsRepository repository;
    private final SysfsNode node = new SysfsNode(FilePath.GOV_IO_FILE);

    IoSchedulerController(SysfsRepository repository) {
        this.repository = repository;
    }

    public SysfsResult<String[]> readAvailableSchedulers() {
        SysfsResult<String> result = repository.readValue(node);
        if (!result.isSuccess()) return SysfsResult.failure(result.getError());
        String[] schedulers = parseSchedulers(result.getValue());
        return schedulers.length == 0
                ? SysfsResult.<String[]>failure("No I/O schedulers are available")
                : SysfsResult.success(schedulers);
    }

    public SysfsResult<String> readCurrentScheduler() {
        SysfsResult<String> result = repository.readValue(node);
        if (!result.isSuccess()) return SysfsResult.failure(result.getError());
        String selected = parseSelectedScheduler(result.getValue());
        return selected == null
                ? SysfsResult.<String>failure("Unable to determine the active I/O scheduler")
                : SysfsResult.success(selected);
    }

    public SysfsResult<String> writeScheduler(String value) {
        SysfsResult<String[]> available = readAvailableSchedulers();
        if (!available.isSuccess()) return SysfsResult.failure(available.getError());
        if (!contains(available.getValue(), value)) {
            return SysfsResult.failure("I/O scheduler is not available");
        }
        return repository.writeValue(new SysfsNode(node.getPath(),
                Arrays.asList(available.getValue())), value,
                new SysfsRepository.ReadbackVerifier() {
                    @Override
                    public boolean matches(String requestedValue, String readback) {
                        return requestedValue.equals(parseSelectedScheduler(readback));
                    }
                });
    }

    private static String[] parseSchedulers(String contents) {
        if (contents == null) return new String[0];
        List<String> values = new ArrayList<>();
        for (String token : contents.trim().split("\\s+")) {
            String scheduler = token;
            if (scheduler.startsWith("[") && scheduler.endsWith("]")) {
                scheduler = scheduler.substring(1, scheduler.length() - 1);
            }
            if (scheduler.length() > 0 && !values.contains(scheduler)) {
                values.add(scheduler);
            }
        }
        return values.toArray(new String[0]);
    }

    private static String parseSelectedScheduler(String contents) {
        if (contents == null) return null;
        for (String token : contents.trim().split("\\s+")) {
            if (token.startsWith("[") && token.endsWith("]")
                    && token.length() > 2) {
                return token.substring(1, token.length() - 1);
            }
        }
        return null;
    }

    private static boolean contains(String[] values, String value) {
        if (value == null) return false;
        for (String available : values) {
            if (available.equals(value.trim())) return true;
        }
        return false;
    }
}
