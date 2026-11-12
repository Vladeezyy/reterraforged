package raccoonman.reterraforged.world.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class AltitudeCoolingTest {

	@AfterEach
	void clear() {
		AltitudeCooling.reset();
		AltitudeCooling.clearClient();
	}

	// a path that knows no level keeps the overworld's scale: a world made with the setting does not change
	@Test
	void withoutAContextTheDefaultApplies() {
		AltitudeCooling.set(0.5F);
		assertEquals(new BlockPos(1, 180, 2), AltitudeCooling.adjust(new BlockPos(1, 280, 2)));
	}

	// another mod's dimension (scale 1) inside the overworld's default: vanilla's cooling, the same position
	@Test
	void aDimensionOfItsOwnCoolsAsInVanilla() {
		AltitudeCooling.set(0.5F);
		BlockPos pos = new BlockPos(0, 280, 0);
		float previous = AltitudeCooling.enter(1.0F);
		try {
			assertSame(pos, AltitudeCooling.adjust(pos));
		} finally {
			AltitudeCooling.exit(previous);
		}
		assertEquals(180, AltitudeCooling.adjust(pos).getY());
	}

	@Test
	void entriesNestAndRestore() {
		float outer = AltitudeCooling.enter(0.5F);
		float inner = AltitudeCooling.enter(1.0F);
		assertEquals(1.0F, AltitudeCooling.scale());
		AltitudeCooling.exit(inner);
		assertEquals(0.5F, AltitudeCooling.scale());
		AltitudeCooling.exit(outer);
		assertEquals(1.0F, AltitudeCooling.scale());
	}

	// the client's scale (single player: its thread) is not the integrated server's
	@Test
	void theClientsScaleStaysOnItsThread() throws Exception {
		AltitudeCooling.set(0.5F);
		AltitudeCooling.setClient(1.0F);
		assertEquals(1.0F, AltitudeCooling.scale());
		assertEquals(0.5F, CompletableFuture.supplyAsync(AltitudeCooling::scale).get());
	}

	@Test
	void belowTheCoolingNothingChanges() {
		AltitudeCooling.set(0.25F);
		BlockPos pos = new BlockPos(0, 80, 0);
		assertSame(pos, AltitudeCooling.adjust(pos));
	}

	@Test
	void aBrokenScaleIsOne() {
		AltitudeCooling.enter(Float.NaN);
		assertEquals(1.0F, AltitudeCooling.scale());
		AltitudeCooling.exit(AltitudeCooling.UNSET);
	}
}
