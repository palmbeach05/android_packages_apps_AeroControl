# su root-shell spawn (Runtime.exec("su")) has no timeout — can hang UI/background thread indefinitely if su prompt is not answered

**Tool:** `harden`
**Severity:** medium
**Category:** security
**Location:** `src/com/aero/control/helpers/rootHelper.java:51`

## What's wrong

`shellHelper.openShell()` and `rootHelper`/`Shell.java` all call `Runtime.getRuntime().exec("su")` and then block on `mOutput.read(buf)` in `getRootResult()` with no timeout. On devices where the superuser manager (SuperSU/Magisk) shows an interactive grant prompt, if the user never responds (or denies without the shell noticing quickly), any code path that calls `getRootInfo`/`runCommandAndWaitForOutput` synchronously can block for the life of the request. Several call sites already work around this by moving invocation to `AsyncTask`, but the underlying primitive itself has no read timeout, so any future/other caller that invokes these methods on the main thread risks an ANR.

## What changed

Add a bounded read (e.g. use `Process.waitFor(timeout, unit)` for one-shot commands, or run the blocking read on a background thread with a `Future.get(timeout, unit)` and destroy the process / return null on timeout) inside `getRootResult()` and `getRootArray()`/`runCommandAndWaitForOutput()`.

## Detection query

```
grep -n 'Runtime.getRuntime().exec("su")' -r src
```
