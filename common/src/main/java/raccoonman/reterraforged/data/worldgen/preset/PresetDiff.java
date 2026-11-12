package raccoonman.reterraforged.data.worldgen.preset;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;

/**
 * The settings of a preset that differ from another one (Default unless said otherwise), one line each:
 * "climate.swampBasins = 0.25 (default 0.0)". For bug reports: the log and crash report of a world, the preset screen.
 *
 * The preset's objects are walked by reflection (record components, instance fields), not its JSON: the codecs leave
 * out a value equal to their default (optionalFieldOf), so the JSON of Default lacks grassDensity while Lush Wilds has
 * it, and the value behind a missing key is unknown. The generated tables (PresetStressTable, PresetLimitTable) only
 * hold what the screen has a slider or toggle for, without the structure sets or the hidden fields. A new field is in
 * the comparison by itself; the paths are the Java field names, which are the JSON keys.
 */
public final class PresetDiff {
	/** Not settings someone chose: the log has them on a line of their own, and an older world of a built-in preset has another one */
	private static final Set<String> GENERATION = Set.of("world.properties.generationVersion", "world.properties.generationFixes");
	// a preset is a few levels deep; the limit only guards against a cycle a later field might bring
	private static final int MAX_DEPTH = 10;
	private static final Map<Class<?>, List<Member>> MEMBERS = new ConcurrentHashMap<>();

	private PresetDiff() {
	}

	/** The settings of the preset that differ from Default; none for Default */
	public static List<String> lines(Preset preset) {
		return lines(preset, Presets.makeDefault());
	}

	/** The settings of the preset that differ from the base */
	public static List<String> lines(Preset preset, Preset base) {
		return changes(preset, base).stream().map(Change::line).toList();
	}

	/** The name of the built-in preset this one is (its generation version and fixes aside), if it is one */
	public static Optional<String> builtInName(Preset preset) {
		for (Map.Entry<String, Supplier<Preset>> entry : BuiltIn.PRESETS.entrySet()) {
			if (changes(preset, entry.getValue().get()).stream().allMatch((change) -> GENERATION.contains(change.path()))) {
				return Optional.of(entry.getKey());
			}
		}
		return Optional.empty();
	}

	/** The built-in presets by name, in the order of the preset list (PresetListPage) */
	public static Map<String, Supplier<Preset>> builtIn() {
		return BuiltIn.PRESETS;
	}

	// made on first use only: lines(Object, Object) must work where Preset cannot be loaded (a unit test, whose
	// platform methods throw)
	private static final class BuiltIn {
		static final Map<String, Supplier<Preset>> PRESETS = make();

		private static Map<String, Supplier<Preset>> make() {
			Map<String, Supplier<Preset>> presets = new LinkedHashMap<>();
			presets.put("Default", Presets::makeDefault);
			presets.put("Archipelago", Presets::makeArchipelago);
			presets.put("Wetlands", Presets::makeWetlands);
			presets.put("Towering Peaks", Presets::makeToweringPeaks);
			presets.put("Endless Continent", Presets::makeEndlessContinent);
			presets.put("River Valleys", Presets::makeRiverValleys);
			presets.put("Lush Wilds", Presets::makeLushWilds);
			presets.put("Rolling Plains", Presets::makeRollingPlains);
			presets.put("Rugged Highlands", Presets::makeRuggedHighlands);
			presets.put("Beautiful", Presets::makeLegacyBeautiful);
			presets.put("Huge Biomes", Presets::makeLegacyHugeBiomes);
			presets.put("Lite", Presets::makeLegacyLite);
			presets.put("Vanillaish", Presets::makeLegacyVanillaish);
			// the Default of ReTerraForged 0.0.6, for its older worlds
			presets.put("Legacy Default", Presets::makeLegacyDefault);
			return Collections.unmodifiableMap(presets);
		}
	}

	private record Change(String path, String line) {
	}

	// any two settings objects of one kind (a preset, a part of one)
	private static List<Change> changes(Object preset, Object base) {
		List<Change> changed = new ArrayList<>();
		// map entries only one side has (structure sets the structure page filled in) after the changed values:
		// with the log's limit those are the ones that get cut
		List<Change> oneSide = new ArrayList<>();
		compare("", preset, base, 0, changed, oneSide);
		changed.addAll(oneSide);
		return changed;
	}

	private static void compare(String path, Object value, Object base, int depth, List<Change> changed, List<Change> oneSide) {
		if (isLeaf(value, depth) || isLeaf(base, depth) || !sameKind(value, base)) {
			String text = text(value, depth);
			String baseText = text(base, depth);
			if (!text.equals(baseText)) {
				changed.add(new Change(path, line(path, text, baseText)));
			}
			return;
		}
		if (value instanceof Map<?, ?> map) {
			Map<String, Object> entries = keyed(map);
			Map<String, Object> baseEntries = keyed((Map<?, ?>) base);
			TreeMap<String, Boolean> keys = new TreeMap<>();
			entries.keySet().forEach((key) -> keys.put(key, true));
			baseEntries.keySet().forEach((key) -> keys.put(key, true));
			for (String key : keys.keySet()) {
				compareEntry(child(path, key), entries.containsKey(key), entries.get(key), baseEntries.containsKey(key), baseEntries.get(key), depth, changed, oneSide);
			}
			return;
		}
		if (value instanceof Collection<?> || value.getClass().isArray()) {
			List<Object> items = items(value);
			List<Object> baseItems = items(base);
			for (int i = 0; i < Math.max(items.size(), baseItems.size()); i++) {
				compareEntry(path + "[" + i + "]", i < items.size(), i < items.size() ? items.get(i) : null, i < baseItems.size(), i < baseItems.size() ? baseItems.get(i) : null, depth, changed, oneSide);
			}
			return;
		}
		for (Member member : members(value.getClass())) {
			compare(child(path, member.name()), member.get(value), member.get(base), depth + 1, changed, oneSide);
		}
	}

	// a HashMap and an ImmutableMap are both a map; other objects are compared field by field only of one class
	private static boolean sameKind(Object value, Object base) {
		if (value instanceof Map<?, ?>) {
			return base instanceof Map<?, ?>;
		}
		if (value instanceof Collection<?> || value.getClass().isArray()) {
			return base instanceof Collection<?> || base.getClass().isArray();
		}
		return value.getClass() == base.getClass();
	}

	private static void compareEntry(String path, boolean present, Object value, boolean basePresent, Object base, int depth, List<Change> changed, List<Change> oneSide) {
		if (present && basePresent) {
			compare(path, value, base, depth + 1, changed, oneSide);
		} else {
			oneSide.add(new Change(path, line(path, present ? text(value, depth + 1) : "none", basePresent ? text(base, depth + 1) : "none")));
		}
	}

	private static String line(String path, String value, String base) {
		return path + " = " + value + " (default " + base + ")";
	}

	private static String child(String path, String name) {
		return path.isEmpty() ? name : path + "." + name;
	}

	// values compared as their text: NaN equals NaN, and what is written is what is compared
	private static boolean isLeaf(Object value, int depth) {
		return value == null || depth > MAX_DEPTH || value instanceof Number || value instanceof Boolean || value instanceof CharSequence || value instanceof Character
			|| value instanceof Enum<?> || value instanceof ResourceKey<?> || value instanceof ResourceLocation || value instanceof Optional<?>
			// another library's object (a noise, a holder) is not walked into
			|| (!(value instanceof Map<?, ?>) && !(value instanceof Collection<?>) && !value.getClass().isArray() && !value.getClass().getName().startsWith("raccoonman."));
	}

	private static String text(Object value, int depth) {
		if (value == null) {
			return "null";
		}
		if (value instanceof StringRepresentable representable) {
			return representable.getSerializedName();
		}
		if (value instanceof Enum<?> e) {
			return e.name();
		}
		if (value instanceof ResourceKey<?> key) {
			return key.location().toString();
		}
		if (value instanceof Optional<?> optional) {
			return optional.map((v) -> text(v, depth + 1)).orElse("none");
		}
		if (isLeaf(value, depth)) {
			return String.valueOf(value);
		}
		// a whole object only one side has (a structure set): one line instead of one per field
		StringBuilder builder = new StringBuilder("{");
		if (value instanceof Map<?, ?> map) {
			keyed(map).forEach((key, v) -> append(builder, key, text(v, depth + 1)));
		} else if (value instanceof Collection<?> || value.getClass().isArray()) {
			List<Object> items = items(value);
			for (int i = 0; i < items.size(); i++) {
				append(builder, String.valueOf(i), text(items.get(i), depth + 1));
			}
		} else {
			for (Member member : members(value.getClass())) {
				append(builder, member.name(), text(member.get(value), depth + 1));
			}
		}
		return builder.append("}").toString();
	}

	private static void append(StringBuilder builder, String key, String value) {
		if (builder.length() > 1) {
			builder.append(", ");
		}
		builder.append(key).append('=').append(value);
	}

	private static Map<String, Object> keyed(Map<?, ?> map) {
		TreeMap<String, Object> keyed = new TreeMap<>();
		map.forEach((key, value) -> keyed.put(key instanceof ResourceKey<?> resourceKey ? resourceKey.location().toString() : String.valueOf(key), value));
		return keyed;
	}

	private static List<Object> items(Object value) {
		if (value instanceof Collection<?> collection) {
			return new ArrayList<>(collection);
		}
		List<Object> items = new ArrayList<>();
		for (int i = 0; i < Array.getLength(value); i++) {
			items.add(Array.get(value, i));
		}
		return items;
	}

	private interface Member {
		String name();

		Object get(Object owner);
	}

	private static List<Member> members(Class<?> type) {
		return MEMBERS.computeIfAbsent(type, PresetDiff::findMembers);
	}

	private static List<Member> findMembers(Class<?> type) {
		List<Member> members = new ArrayList<>();
		if (type.isRecord()) {
			for (RecordComponent component : type.getRecordComponents()) {
				Method accessor = component.getAccessor();
				if (accessor.trySetAccessible()) {
					members.add(member(component.getName(), (owner) -> accessor.invoke(owner)));
				}
			}
			return List.copyOf(members);
		}
		// superclass fields first, each class in the order of its source
		List<Class<?>> hierarchy = new ArrayList<>();
		for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
			hierarchy.add(0, c);
		}
		for (Class<?> c : hierarchy) {
			for (Field field : c.getDeclaredFields()) {
				int modifiers = field.getModifiers();
				if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic() || !field.trySetAccessible()) {
					continue;
				}
				members.add(member(field.getName(), field::get));
			}
		}
		return List.copyOf(members);
	}

	private interface Getter {
		Object get(Object owner) throws ReflectiveOperationException;
	}

	private static Member member(String name, Getter getter) {
		return new Member() {
			@Override
			public String name() {
				return name;
			}

			@Override
			public Object get(Object owner) {
				try {
					return getter.get(owner);
				} catch (ReflectiveOperationException | RuntimeException e) {
					// shown as "?" on both sides: no line rather than a broken log
					return "?";
				}
			}
		};
	}

	/** The lines of any two settings objects of one kind: a part of a preset, for a test where Preset cannot be loaded */
	public static List<String> lines(Object value, Object base) {
		return changes(value, base).stream().map(Change::line).toList();
	}

	/** The paths of the changed settings */
	public static List<String> paths(Object value, Object base) {
		return changes(value, base).stream().map(Change::path).toList();
	}
}
