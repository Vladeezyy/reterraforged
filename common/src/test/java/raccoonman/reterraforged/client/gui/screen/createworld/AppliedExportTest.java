package raccoonman.reterraforged.client.gui.screen.createworld;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AppliedExportTest {
	private static final String OWN_1 = "file/reterraforged-preset-1.zip";
	private static final String OWN_2 = "file/reterraforged-preset-2.zip";
	private final AppliedExport applied = new AppliedExport((id) -> id.startsWith("file/reterraforged-preset-"));
	// the screen's registries: each reload of the datapacks makes new ones
	private final Object before = new Object();
	private final Object after = new Object();
	private final List<String> packsBefore = List.of("vanilla", "file/terralith.zip", OWN_1);
	private final List<String> packsAfter = List.of("vanilla", "file/terralith.zip", OWN_2);

	// an export of {"a":1} went in as OWN_2 and the reload with it is over
	private void exportAndReload() {
		this.applied.exported("{\"a\":1}", "Default", OWN_2, this.before, this.packsBefore);
		this.applied.settingsChanged(this.before, this.packsBefore);
		this.applied.settingsChanged(this.after, this.packsAfter);
	}

	private boolean current(String json, String name, Object registries, List<String> packs, boolean fileThere) {
		return this.applied.isCurrent(json, name, registries, packs, (id) -> fileThere);
	}

	@Test
	void theSamePresetAfterItsReloadIsCurrent() {
		this.exportAndReload();
		assertTrue(this.current("{\"a\":1}", "Default", this.after, this.packsAfter, true));
	}

	@Test
	void otherSettingsOfTheScreenKeepItCurrent() {
		this.exportAndReload();
		// the seed or a game rule: new settings with the same registries
		this.applied.settingsChanged(this.after, this.packsAfter);
		assertTrue(this.current("{\"a\":1}", "Default", this.after, this.packsAfter, true));
	}

	@Test
	void anotherPresetOrNameIsExported() {
		this.exportAndReload();
		assertFalse(this.current("{\"a\":2}", "Default", this.after, this.packsAfter, true));
		assertFalse(this.current("{\"a\":1}", "Archipelago", this.after, this.packsAfter, true));
		assertFalse(this.current(null, "Default", this.after, this.packsAfter, true));
	}

	@Test
	void nothingIsCurrentBeforeTheReload() {
		this.applied.exported("{\"a\":1}", "Default", OWN_2, this.before, this.packsBefore);
		assertFalse(this.current("{\"a\":1}", "Default", this.before, this.packsBefore, true));
		this.applied.settingsChanged(this.before, this.packsBefore);
		assertFalse(this.current("{\"a\":1}", "Default", this.before, this.packsBefore, true));
	}

	@Test
	void anyLaterReloadIsExported() {
		this.exportAndReload();
		// the Data Packs screen: another pack, or the same ones again (a pack's files may have changed)
		Object reloaded = new Object();
		this.applied.settingsChanged(reloaded, this.packsAfter);
		assertFalse(this.current("{\"a\":1}", "Default", reloaded, this.packsAfter, true));
		// and the registries of before do not count either once forgotten
		assertFalse(this.current("{\"a\":1}", "Default", this.after, this.packsAfter, true));
	}

	@Test
	void aReloadThatBroughtOtherPacksTooIsNotTheExportsOwn() {
		this.applied.exported("{\"a\":1}", "Default", OWN_2, this.before, this.packsBefore);
		List<String> more = List.of("vanilla", "file/terralith.zip", "file/other.zip", OWN_2);
		this.applied.settingsChanged(this.after, more);
		assertFalse(this.current("{\"a\":1}", "Default", this.after, more, true));
	}

	@Test
	void aReloadWithoutThePackForgetsIt() {
		this.applied.exported("{\"a\":1}", "Default", OWN_2, this.before, this.packsBefore);
		// vanilla generation, or the reload failed and fell back
		List<String> vanilla = List.of("vanilla");
		this.applied.settingsChanged(this.after, vanilla);
		assertFalse(this.current("{\"a\":1}", "Default", this.after, vanilla, true));
	}

	@Test
	void theOrderOfTheOtherPacksCounts() {
		this.applied.exported("{\"a\":1}", "Default", OWN_2, this.before, this.packsBefore);
		List<String> swapped = List.of("file/terralith.zip", "vanilla", OWN_2);
		this.applied.settingsChanged(this.after, swapped);
		assertFalse(this.current("{\"a\":1}", "Default", this.after, swapped, true));
	}

	@Test
	void aMissingPackFileIsExported() {
		this.exportAndReload();
		assertFalse(this.current("{\"a\":1}", "Default", this.after, this.packsAfter, false));
	}

	@Test
	void forgetIsFinal() {
		this.exportAndReload();
		this.applied.forget();
		assertFalse(this.current("{\"a\":1}", "Default", this.after, this.packsAfter, true));
		this.applied.settingsChanged(this.after, this.packsAfter);
		assertFalse(this.current("{\"a\":1}", "Default", this.after, this.packsAfter, true));
	}
}
