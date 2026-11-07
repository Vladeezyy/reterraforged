#!/bin/bash
# A client without any mod on the test server that runs now: tools/vanillaclient.sh <server dir> <server log> <port> <loader>,
# from tools/servertest.sh VANILLA_CLIENT=1. macOS. The game's jar, libraries and assets come from Fabric Loom's Gradle caches
# (the version was built once). It joins, flies over the spawn and over a desert and a swamp, and stays connected; the window
# stays behind the person's work. Ends with one line: "RTF vanilla client] PASS|FAIL ...".
# VANILLA_SHOTS=1 also a screenshot of the window at each place: macOS then asks the person for access to the screen, so it is off by default.
# Every wait has a limit, and the client is always stopped at the end.
cd "$(dirname "$0")/.."
SERVER=$1; SERVER_LOG=$1/$2; PORT=$3; LOADER=$4
JAVA=${JAVA:-${JAVA_HOME:+$JAVA_HOME/bin/}java}
MC=$(grep "^minecraft_version=" gradle.properties | cut -d= -f2 | tr -d '\r')
NAME=RTFVanilla
TAG=$(pwd | cksum | cut -d' ' -f1)
OUT=run/vanillaclient-$LOADER
LOG=$OUT/client.log
ROOT=$HOME/.gradle/caches/fabric-loom
rm -rf "$OUT"; mkdir -p "$OUT/game"
: > "$LOG"

result() { echo "RTF vanilla client] $1"; exit 0; }
say() { echo "$1" >> "$SERVER/console.in"; }

# the agent that keeps the window from taking the focus: built once per checkout
ASM=$(find "$HOME/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm" -name 'asm-9*.jar' 2>/dev/null | grep -v sources | sort | tail -1)
[ -n "$ASM" ] || result "FAIL no ASM in the Gradle cache"
AGENT=run/vanillaclient-agent.jar
if [ ! -s "$AGENT" ]; then
    build=$(mktemp -d)
    "${JAVA%java}javac" -cp "$ASM" -d "$build" tools/vanillaclient/NoFocusAgent.java || result "FAIL the agent did not build"
    printf 'Premain-Class: rtf.vanillaclient.NoFocusAgent\n' > "$build/manifest.txt"
    "${JAVA%java}jar" cfm "$AGENT" "$build/manifest.txt" -C "$build" . || result "FAIL the agent did not build"
    rm -rf "$build"
fi

# the version's own arguments and libraries for this Mac, one per line: "jvm <arg>", "cp <jar>", "index <id>", "missing <path>"
lines=$(python3 - "$ROOT/$MC/minecraft-info.json" "$HOME/.gradle/caches/modules-2/files-2.1" "$OUT/natives" "$ROOT/assets/indexes" <<'EOF'
import glob, json, os, platform, sys
info = json.load(open(sys.argv[1]))
modules, natives, indexes = sys.argv[2:5]
def allowed(rules):
    if not rules:
        return True
    ok = False
    for r in rules:
        o = r.get("os")
        if r.get("features"):
            continue
        if not o or ((not o.get("name") or o["name"] == "osx") and (not o.get("arch") or o["arch"] == platform.machine())):
            ok = r["action"] == "allow"
    return ok
seen = set()
for lib in info["libraries"]:
    art = lib.get("downloads", {}).get("artifact")
    if not art or not allowed(lib.get("rules")):
        continue
    parts = lib["name"].split(":")
    file = os.path.basename(art["path"])
    hits = glob.glob(f"{modules}/{parts[0]}/{parts[1]}/{parts[2]}/*/{file}")
    if file in seen:
        continue
    seen.add(file)
    print(("cp " + hits[0]) if hits else ("missing " + art["path"]))
for a in info.get("arguments", {}).get("jvm", []):
    if isinstance(a, str):
        values = [a]
    elif allowed(a.get("rules")):
        values = a["value"] if isinstance(a["value"], list) else [a["value"]]
    else:
        values = []
    for v in values:
        if v in ("-cp", "${classpath}"):
            continue
        print("jvm " + v.replace("${natives_directory}", natives).replace("${launcher_name}", "rtf-test").replace("${launcher_version}", "1"))
name = info["assets"]
found = glob.glob(f"{indexes}/{info['id']}-{name}.json") or glob.glob(f"{indexes}/{name}.json")
print("index " + (os.path.basename(found[0])[:-5] if found else ""))
EOF
)
missing=$(echo "$lines" | sed -n 's/^missing //p')
[ -z "$missing" ] || result "FAIL libraries not in the Gradle cache: $(echo "$missing" | head -3 | tr '\n' ' ')"
index=$(echo "$lines" | sed -n 's/^index //p')
[ -n "$index" ] || result "FAIL no asset index in the Gradle cache"
[ -s "$ROOT/$MC/minecraft-client.jar" ] || result "FAIL no client jar in the Gradle cache"
cp="$ASM:$AGENT:$ROOT/$MC/minecraft-client.jar:$(echo "$lines" | sed -n 's/^cp //p' | tr '\n' ':')"
jvm=(); while IFS= read -r a; do jvm+=("$a"); done < <(echo "$lines" | sed -n 's/^jvm //p')

cat > "$OUT/game/options.txt" <<'EOF'
pauseOnLostFocus:false
maxFps:30
renderDistance:8
simulationDistance:5
soundCategory_master:0.0
onboardAccessibility:false
skipMultiplayerWarning:true
joinedFirstServer:true
tutorialStep:none
realmsNotifications:false
EOF

# SDL of 26.3 reads these, GLFW gets the agent
SDL_WINDOW_ACTIVATE_WHEN_SHOWN=0 SDL_WINDOW_ACTIVATE_WHEN_RAISED=0 SDL_MAC_BACKGROUND_APP=1 \
"$JAVA" "${jvm[@]}" -Xmx2G "-Drtf.vanillaclient=$TAG" "-javaagent:$AGENT" -cp "$cp" net.minecraft.client.main.Main \
    --username "$NAME" --version "$MC" --gameDir "$OUT/game" --assetsDir "$ROOT/assets" --assetIndex "$index" \
    --uuid 00000000000000000000000000000000 --accessToken 0 --userType legacy --versionType release \
    --width 854 --height 480 --quickPlayMultiplayer "localhost:$PORT" > "$LOG" 2>&1 &
client=$!
trap 'kill "$client" 2>/dev/null' EXIT

# joined, or refused, or the client died: at most 3 minutes
start=$SECONDS; joined=
while [ $((SECONDS - start)) -lt 180 ]; do
    grep -q "$NAME joined the game" "$SERVER_LOG" && { joined=1; break; }
    grep -q "$NAME.*lost connection" "$SERVER_LOG" && break
    grep -q "Disconnecting $NAME\|Disconnecting com.mojang.authlib.*$NAME" "$SERVER_LOG" && break
    kill -0 "$client" 2>/dev/null || break
    sleep 1
done
if [ -z "$joined" ]; then
    reason=$(grep -E "$NAME.*(lost connection|Disconnecting)|lost connection" "$SERVER_LOG" | tail -1 | cut -c1-200)
    [ -n "$reason" ] || reason=$(grep -E 'Disconnected|Failed|Exception' "$LOG" | tail -1 | cut -c1-200)
    result "FAIL did not join after $((SECONDS - start)) s: ${reason:-no reason in the logs}"
fi
join_seconds=$((SECONDS - start))

window=
if [ -n "$VANILLA_SHOTS" ]; then
    # window of a process, for the screenshots
    cat > "$OUT/window.swift" <<'EOF'
import CoreGraphics
let pid = Int(CommandLine.arguments[1])!
let list = CGWindowListCopyWindowInfo([.optionAll], kCGNullWindowID) as! [[String: Any]]
for w in list where (w["kCGWindowOwnerPID"] as? Int) == pid && (w["kCGWindowLayer"] as? Int) == 0 {
    if let b = w["kCGWindowBounds"] as? [String: Any], (b["Height"] as? Double ?? 0) > 100 {
        print(w["kCGWindowNumber"]!)
        break
    }
}
EOF
    window=$(swift "$OUT/window.swift" "$client" 2>/dev/null | head -1)
fi
shot() { [ -z "$window" ] || screencapture -x -o -l"$window" "$OUT/$1.png" 2>/dev/null; }

say "time set noon"; say "weather clear"; say "gamemode spectator $NAME"
say "tp $NAME 8 150 8 0 30"
sleep 25; shot spawn

places=spawn
for biome in desert swamp; do
    say "locate biome minecraft:$biome"
    at=
    for i in $(seq 1 60); do
        at=$(grep -oE "nearest minecraft:$biome is at \[-?[0-9]+, [^,]+, -?[0-9]+\]" "$SERVER_LOG" | tail -1)
        [ -n "$at" ] && break
        sleep 1
    done
    [ -n "$at" ] || continue
    x=$(echo "$at" | sed -E 's/.*\[(-?[0-9]+),.*/\1/'); z=$(echo "$at" | sed -E 's/.*, (-?[0-9]+)\]/\1/')
    say "tp $NAME $x 110 $z 0 30"
    sleep 25; shot "$biome"
    places="$places $biome ($x $z)"
done

say "list"; sleep 3
online=$(grep -E "players online: .*$NAME" "$SERVER_LOG" | tail -1)
lost=$(grep -E "$NAME.*lost connection" "$SERVER_LOG" | tail -1 | cut -c1-200)
kill -0 "$client" 2>/dev/null; alive=$?
# the server ends the connection first: a client that just dies is a "Connection reset" error in the server's log
say "kick $NAME the test is over"; sleep 3
kill "$client" 2>/dev/null; wait "$client" 2>/dev/null; trap - EXIT
errors=$(grep -E '/ERROR\]|\[ERROR\]|Exception' "$LOG" | grep -vE 'Yggdrasil|authlib|minecraftservices|sessionserver|profile key|Failed to fetch|realms|Realms|narrator|Narrator|Couldn.t connect to auth|telemetry|UnknownHost' | cut -c1-200 | sort -u | head -5)
bad=
[ -n "$online" ] || bad="not in the player list at the end; "
[ -z "$lost" ] || bad="$bad$lost; "
[ "$alive" = 0 ] || bad="${bad}the client ended by itself; "
[ -z "$errors" ] || bad="${bad}errors in the client's log: $(echo "$errors" | tr '\n' ' '); "
[ -z "$VANILLA_SHOTS" ] || [ -n "$window" ] || echo "  no window found for the screenshots"
[ -z "$bad" ] && result "PASS joined after $join_seconds s, stayed online over $places${window:+; shots in $OUT}" || result "FAIL $bad after joining in $join_seconds s, over $places${window:+; shots in $OUT}"
