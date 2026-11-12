package raccoonman.reterraforged.client.gui.screen.createworld;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

/**
 * The preset datapack a create world screen has in it, to skip an export that would write the same pack again (Done
 * without changes showed "Preparing..." and reloaded the datapacks for nothing).
 *
 * The export reads the preset, its name and the screen's worldgen registries, which come from the enabled datapacks. The
 * registries are compared by identity: every reload of the datapacks makes new ones, so the screen still having the
 * registries the reload with this pack made proves that no pack changed since. The preset is compared as its encoded
 * JSON. Anything unsure (no JSON, a reload that brought other packs too, the pack file gone) means export again.
 */
final class AppliedExport {
	// the screen's own preset packs (the export of each apply has a new file name)
	private final Predicate<String> ownPack;
	@Nullable
	private String presetJson;
	@Nullable
	private String name;
	@Nullable
	private String packId;
	@Nullable
	private List<String> otherPacks;
	@Nullable
	private Object registriesBefore;
	@Nullable
	private Object registriesAfter;

	AppliedExport(Predicate<String> ownPack) {
		this.ownPack = ownPack;
	}

	/** An export of presetJson was made from registriesBefore and goes in as packId now; the reload is still to come */
	void exported(@Nullable String presetJson, String name, String packId, Object registriesBefore, Collection<String> enabledBefore) {
		this.presetJson = presetJson;
		this.name = name;
		this.packId = packId;
		this.otherPacks = this.others(enabledBefore);
		this.registriesBefore = registriesBefore;
		this.registriesAfter = null;
	}

	/** The screen's settings changed: the seed, a game rule, or new registries after a reload of the datapacks */
	void settingsChanged(Object registries, Collection<String> enabledPacks) {
		if (this.packId == null) {
			return;
		}
		if (this.registriesAfter == null) {
			if (registries == this.registriesBefore) {
				// not the reload yet
				return;
			}
			// the reload of this pack, and only of it: the other packs the export was made with, and this one
			if (enabledPacks.contains(this.packId) && this.others(enabledPacks).equals(this.otherPacks)) {
				this.registriesAfter = registries;
				this.registriesBefore = null;
			} else {
				this.forget();
			}
		} else if (registries != this.registriesAfter) {
			this.forget();
		}
	}

	/** Whether an export of presetJson under name would make the pack the screen has now */
	boolean isCurrent(@Nullable String presetJson, String name, Object registries, Collection<String> enabledPacks, Predicate<String> packFileExists) {
		return presetJson != null
			&& this.packId != null
			&& this.registriesAfter != null
			&& registries == this.registriesAfter
			&& presetJson.equals(this.presetJson)
			&& name.equals(this.name)
			&& enabledPacks.contains(this.packId)
			&& this.others(enabledPacks).equals(this.otherPacks)
			&& packFileExists.test(this.packId);
	}

	void forget() {
		this.presetJson = null;
		this.name = null;
		this.packId = null;
		this.otherPacks = null;
		this.registriesBefore = null;
		this.registriesAfter = null;
	}

	// in their order: it is the order the packs override each other in
	private List<String> others(Collection<String> packs) {
		return packs.stream().filter(this.ownPack.negate()).toList();
	}
}
