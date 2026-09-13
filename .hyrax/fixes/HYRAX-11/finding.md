# shellHelper mixes 6+ unrelated responsibilities in a single 1044-line class

**Tool:** `code-quality`
**Severity:** medium
**Category:** maintainability
**Location:** `src/com/aero/control/helpers/shellHelper.java:26`

## What's wrong

`shellHelper` is a singleton that simultaneously: manages a persistent root shell process/queue, does plain (non-root) file I/O, parses `/proc/version` kernel strings with hand-rolled regex, formats frequency values to MHz, handles Tegra-I2C and hwmon-specific sysfs enumeration with device-specific allowlist regexes, and even contains a legacy overclock address-patching routine for one specific device family (`setOverclockAddress`). It imports from file I/O, process management, regex parsing, and device-specific hardware logic — five+ unrelated domains in one class.

This makes the class hard to reason about: a change to root-shell session handling risks touching kernel-version parsing code by proximity, and the Tegra-I2C/hwmon-specific logic (device-specific hacks) has no clear reason to live inside the generic shell-execution helper.

## What changed

Split shellHelper into: (1) a `RootShellSession` class owning the persistent `su` process, command queue, and read/write plumbing; (2) a `SysfsReader` utility for generic file reads with root fallback; (3) a `KernelVersionParser` for `/proc/version` formatting; (4) device-specific helpers (`TegraI2cInfo`, `HwmonInfo`, `OverclockLegacy`) kept separate from the core shell session. Keep `shellHelper` (or its renamed successor) as the thin session/queue API that the other pieces depend on.

## Detection query

```
wc -l src/com/aero/control/helpers/shellHelper.java
```
