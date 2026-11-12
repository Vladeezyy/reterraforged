#!/usr/bin/env python3
"""One test pass of a release, every version and both loaders, within an hour: tools/releasepass.py [options] <checkout>...

Each checkout (one Minecraft version) runs its cases one after another, the checkouts at the same time, at most --jobs
servers at once. The mod is built first. The numbers that need a quiet machine (tools/benchrepeat.sh) are not here: run
them alone afterwards. "widgets" opens a dev client window (tools/clienttest.sh): the desktop shows it.

Options: --jobs N (4), --only a,b (only the cases with these names), --skip a,b, --retake (fingerprints taken and written to
tools/fingerprint, for a change meant to change the world), --full <mc> (the slower cases on one version, default 26.3).
Logs: <checkout>/run/releasepass/<case>.log. The table: run/releasepass-summary.txt of the first checkout, a line per case
as it ends: version, case, PASS or FAIL, minutes, what the log ended with.
"""
import argparse
import os
import pathlib
import shutil
import signal
import subprocess
import threading
import time

PORTS = {"1.21.1": 25700, "1.21.11": 25701, "26.1.2": 25702, "26.2": 25703, "26.3": 25704}
QUICK = {"SKIP_SELFTEST": "1", "SKIP_STRUCTURES": "1"}
BIOME_MODS = "biomes-o-plenty terralith"
BASINS = "set:climate().swampBasins=0.25;climate().swampMaxHeight=12"
ISLANDS = "set:island().enableArchipelago=true"
# a case that takes longer has hung
TIMEOUT = 20 * 60
# the settings "rtf stress effects" finds no effect of in the terrain cells or the datapack: all four are read while
# chunks generate (RTFEffectTest). Another one is a setting that does nothing
WITHOUT_EFFECT = {"climate().swampMaxHeight", "miscellaneous().snowLine.height", "world().properties.altitudeCoolingFollowsHeight", "world().properties.lavaLevel"}


def prop(checkout, key, required=True):
    for line in (checkout / "gradle.properties").read_text().splitlines():
        if line.startswith(key + "="):
            return line.split("=", 1)[1].strip()
    if required:
        raise SystemExit(f"{checkout}: no {key}")
    return None


def fabric_test(checkout):
    # the Fabric test servers' loader and Fabric API (current C2ME and biome mods need new ones), from the checkout's
    # gradle.properties: each Minecraft version has its own; None when the checkout does not name them
    loader, api = prop(checkout, "fabric_loader_test_version", False), prop(checkout, "fabric_api_test_version", False)
    return {"FABRIC_LOADER": loader, "FABRIC_API": api} if loader and api else None


def last_release(checkout, mc):
    # the newest tag v<version>-<mc> below this version, or None
    current = tuple(int(p) for p in prop(checkout, "mod_version").split("."))
    tags = subprocess.run(["git", "tag", "--list", f"v*-{mc}"], cwd=checkout, capture_output=True, text=True).stdout.split()
    versions = []
    for tag in tags:
        version = tag[1:-len(mc) - 1]
        if version.replace(".", "").isdigit() and tuple(int(p) for p in version.split(".")) < current:
            versions.append(tuple(int(p) for p in version.split(".")))
    return ".".join(map(str, max(versions))) if versions else None


def server(*loaders, **env):
    return ["bash", "tools/servertest.sh", *loaders], env


def cases(mc, full, retake):
    # the mods a version's biome mods want (the Fabric they need: fabric_test in named)
    # Biomes O' Plenty lists no dependencies on Modrinth for 26.x
    mods = "biomes-o-plenty terrablender glitchcore terralith" if mc.startswith("26") else BIOME_MODS
    print_env = {"FINGERPRINT": "1"} if retake else None
    fingerprint = lambda name: print_env or {"FINGERPRINT_REF": f"tools/fingerprint/{name}.txt"}

    yield "build", (["./gradlew", "build", "--console=plain"], {})
    # the world without mods: each fingerprint run saves its heights too
    yield "fingerprint-neoforge", server("neoforge", **QUICK, **fingerprint(mc))
    yield "fingerprint-fabric", server("fabric", **QUICK, **fingerprint(mc))
    if retake:
        # the new references against a second world: the same generation from run to run
        yield "refcheck-neoforge", server("neoforge", **QUICK, FINGERPRINT_REF=f"tools/fingerprint/{mc}.txt")
        yield "refcheck-fabric", server("fabric", **QUICK, FINGERPRINT_REF=f"tools/fingerprint/{mc}.txt")
    yield "selftest-neoforge", server("neoforge")
    yield "selftest-fabric", server("fabric", SKIP_STRUCTURES="1")
    yield "fault-block", server("neoforge", FAULT="block")
    yield "fault-ticket", server("fabric", FAULT="ticket")
    # a world of the last release opened with this build has the same ground (worlds made before keep their terrain);
    # the release comes from the tags of the checkout (named)
    yield "legacy-neoforge", server("neoforge", LEGACY="", **QUICK)
    yield "vanilla-client", server("neoforge", "fabric", VANILLA_CLIENT="1", **QUICK)
    # against the world without mods; the heights were saved after the fingerprint runs
    yield "mods-neoforge", server("neoforge", MODS=mods, EXTRA_JVM="-Xmx6G", HEIGHTS_REF="run/releasepass/heights-neoforge.txt", **QUICK)
    yield "mods-fabric", server("fabric", MODS=mods, EXTRA_JVM="-Xmx6G", HEIGHTS_REF="run/releasepass/heights-fabric.txt", **QUICK)
    yield "swamps-neoforge", server("neoforge", MODS=mods, EXTRA_JVM="-Xmx6G", SWAMPS="1", PRESET=BASINS, **QUICK)
    yield "islands-neoforge", server("neoforge", ISLANDS="1", PRESET=ISLANDS, **QUICK)
    yield "vegetation-neoforge", server("neoforge", VEGETATION="1", **QUICK)
    # ModernFix has builds on Modrinth only for these
    yield "compat-neoforge", server("neoforge", MODS="c2me-neoforge modernfix" if mc in ("1.21.1", "26.1.2") else "c2me-neoforge", JAVA="/opt/homebrew/opt/openjdk@25/bin/java", **QUICK)
    yield "compat-fabric", server("fabric", MODS="c2me-fabric modernfix" if mc == "1.21.1" else "c2me-fabric", JAVA="/opt/homebrew/opt/openjdk@25/bin/java", **QUICK)
    yield "widgets", (["bash", "tools/clienttest.sh", "neoforge", "widgets"], {})
    if mc == full:
        yield "swamps-fabric", server("fabric", MODS=mods, EXTRA_JVM="-Xmx6G", SWAMPS="1", PRESET=BASINS, **QUICK)
        yield "islands-fabric", server("fabric", ISLANDS="1", PRESET=ISLANDS, **QUICK)
        yield "stress-abuse", (["bash", "tools/stresstest.sh", "neoforge", "abuse"], {})
        yield "stress-effects", (["bash", "tools/stresstest.sh", "neoforge", "effects"], {})


class Pass:
    def __init__(self, args):
        self.args = args
        self.slots = threading.Semaphore(args.jobs)
        self.lock = threading.Lock()
        self.first = pathlib.Path(args.checkouts[0]).expanduser().resolve()
        self.summary = self.first / "run/releasepass-summary.txt"
        self.summary.parent.mkdir(exist_ok=True)
        self.summary.write_text("")
        self.failed = 0

    def report(self, mc, name, ok, minutes, note):
        line = f"{mc:8} {name:22} {'PASS' if ok else 'FAIL'} {minutes:5.1f} {note}"
        with self.lock:
            self.failed += 0 if ok else 1
            with self.summary.open("a") as out:
                out.write(line + "\n")
            print(line, flush=True)

    def run_case(self, checkout, mc, name, command, env):
        log = checkout / "run/releasepass" / f"{name}.log"
        full_env = {**os.environ, "PORT": str(PORTS[mc]), "JAVA_HOME": f"/opt/homebrew/opt/openjdk@{25 if mc.startswith('26') else 21}", **env}
        with self.slots:
            start = time.time()
            with log.open("w") as out:
                process = subprocess.Popen(command, cwd=checkout, env=full_env, stdout=out, stderr=subprocess.STDOUT, start_new_session=True)
                try:
                    code = process.wait(timeout=TIMEOUT)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    code = "hung"
        lines = [l.strip() for l in log.read_text(errors="replace").splitlines() if l.strip()]
        if name == "stress-effects" and code != "hung":
            # stresstest.sh exits 1 for effects (no "0 failed" line): the list of settings without effect decides
            without = {l.split(": ", 1)[1] for l in lines if l.startswith("without effect: ")}
            done = any(l.startswith("stress: ") and "variants in" in l for l in lines)
            code = 0 if done and without == WITHOUT_EFFECT else 1
            lines.append(f"without effect: {len(without)}" + (f", not expected: {sorted(without - WITHOUT_EFFECT)}" if without - WITHOUT_EFFECT else "") + (f", now with an effect: {sorted(WITHOUT_EFFECT - without)}" if WITHOUT_EFFECT - without else ""))
        fails = [l for l in lines if "FAIL" in l or "HUNG" in l]
        note = (fails[0] if code != 0 and fails else lines[-1] if lines else "")[:200]
        self.report(mc, name, code == 0, (time.time() - start) / 60, f"exit {code}: {note}" if code != 0 else note)
        return code == 0

    def run_checkout(self, path):
        checkout = pathlib.Path(path).expanduser().resolve()
        mc = prop(checkout, "minecraft_version")
        (checkout / "run/releasepass").mkdir(parents=True, exist_ok=True)
        only = self.args.only.split(",") if self.args.only else None
        skip = self.args.skip.split(",") if self.args.skip else []
        named = self.named(checkout, mc)
        if named is None:
            self.report(mc, "config", False, 0, "no fabric_loader_test_version / fabric_api_test_version in gradle.properties")
            return
        for name, (command, env) in named:
            if (only and name not in only) or name in skip:
                continue
            ok = self.run_case(checkout, mc, name, command, env)
            if name == "build" and not ok:
                return
            if name.startswith("fingerprint-"):
                loader = name.split("-")[1]
                if (checkout / f"run/heights-{loader}.txt").exists():
                    shutil.copy(checkout / f"run/heights-{loader}.txt", checkout / f"run/releasepass/heights-{loader}.txt")
            if self.args.retake and name.startswith("fingerprint-") and ok:
                target = f"{mc}.txt" if loader == "neoforge" or not (checkout / f"tools/fingerprint/{mc}-fabric.txt").exists() else f"{mc}-fabric.txt"
                shutil.copy(checkout / f"run/fingerprint-{loader}.txt", checkout / "tools/fingerprint" / target)

    def named(self, checkout, mc):
        # the fingerprint names depend on the files of this checkout, the Fabric versions on its gradle.properties
        result = []
        fabric = fabric_test(checkout)
        if fabric is None:
            return None
        fabric_ref = f"{mc}-fabric" if (checkout / f"tools/fingerprint/{mc}-fabric.txt").exists() else mc
        for name, (command, env) in cases(mc, self.args.full, self.args.retake):
            if name == "legacy-neoforge":
                last = last_release(checkout, mc)
                if not last:
                    continue
                env = {**env, "LEGACY": last}
            if name in ("fingerprint-fabric", "refcheck-fabric") and "FINGERPRINT_REF" in env:
                env = {**env, "FINGERPRINT_REF": f"tools/fingerprint/{fabric_ref}.txt"}
            # every case on the same Fabric (servertest.sh reads these too; passed for checkouts whose script does not)
            result.append((name, (command, {**fabric, **env})))
        return result


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("checkouts", nargs="+")
    parser.add_argument("--jobs", type=int, default=4)
    parser.add_argument("--only", default="")
    parser.add_argument("--skip", default="")
    parser.add_argument("--retake", action="store_true")
    parser.add_argument("--full", default="26.3")
    args = parser.parse_args()
    run = Pass(args)
    start = time.time()
    threads = [threading.Thread(target=run.run_checkout, args=(c,)) for c in args.checkouts]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    print(f"{run.failed} failed, {(time.time() - start) / 60:.1f} minutes", flush=True)
    raise SystemExit(1 if run.failed else 0)


if __name__ == "__main__":
    main()
