package raccoonman.reterraforged.world.worldgen.noise.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import raccoonman.reterraforged.world.worldgen.noise.function.Interpolation;

/**
 * Perlin noise of the data reads the world's seed from generation version 1; the data of older presets is the same.
 * Noises (and so Noises.withSeededPerlin) needs the platform's registries: the server self test checks the preset's data
 */
class SeededPerlinTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static float sum(Noise noise, int seed) {
		float sum = 0.0F;
		for(int i = 0; i < 64; i++) {
			sum += noise.compute(i * 37.5F, i * -11.25F, seed) * (i + 1);
		}
		return sum;
	}

	private static void check(Noise unseeded, Noise seeded) {
		// as before: the seed it is computed with does nothing
		assertEquals(sum(unseeded, 1), sum(unseeded, 2));
		assertEquals(sum(unseeded, 0), sum(seeded, 0));
		assertNotEquals(sum(seeded, 1), sum(seeded, 2));
		// the X and Z of the biome edges: one noise in two ShiftSeeds (7 + 11, 7 + 12), which add to the seed
		assertNotEquals(sum(seeded, 7 + 11), sum(seeded, 7 + 12));
		assertEquals(sum(unseeded, 7 + 11), sum(unseeded, 7 + 12));
	}

	@Test
	void perlin() {
		check(new Perlin(5, 0.01F, 3, 2.0F, 0.5F, Interpolation.CURVE3), new Perlin(5, 0.01F, 3, 2.0F, 0.5F, Interpolation.CURVE3, true));
	}

	@Test
	void perlin2() {
		check(new Perlin2(5, 0.01F, 3, 2.0F, 0.5F, Interpolation.CURVE3), new Perlin2(5, 0.01F, 3, 2.0F, 0.5F, Interpolation.CURVE3, true));
	}

	@Test
	void theDataOfAnOlderPresetIsTheSame() {
		Perlin unseeded = new Perlin(5, 0.01F, 3, 2.0F, 0.5F, Interpolation.CURVE3);
		JsonObject json = Perlin.CODEC.codec().encodeStart(JsonOps.INSTANCE, unseeded).getOrThrow().getAsJsonObject();
		assertFalse(json.has("seeded"));
		assertEquals(unseeded, Perlin.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow());
		Perlin2 seeded = new Perlin2(5, 0.01F, 3, 2.0F, 0.5F, Interpolation.CURVE3, true);
		JsonObject seededJson = Perlin2.CODEC.codec().encodeStart(JsonOps.INSTANCE, seeded).getOrThrow().getAsJsonObject();
		assertTrue(seededJson.get("seeded").getAsBoolean());
		assertEquals(seeded, Perlin2.CODEC.codec().parse(JsonOps.INSTANCE, seededJson).getOrThrow());
	}

	// "simplex_ridge" was read as a PerlinRidge: a datapack that has one keeps it, a SimplexRidge can be written now
	@Test
	void simplexRidge() {
		SimplexRidge ridge = new SimplexRidge(0.01F, 3, 2.0F, 0.5F, Interpolation.CURVE3);
		JsonObject json = SimplexRidge.CODEC.codec().encodeStart(JsonOps.INSTANCE, ridge).getOrThrow().getAsJsonObject();
		Noise read = SimplexRidge.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
		assertInstanceOf(SimplexRidge.class, read);
		assertEquals(sum(ridge, 3), sum(read, 3));
		json.remove("simplex");
		Noise old = SimplexRidge.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
		assertEquals(new PerlinRidge(0.01F, 3, 2.0F, 0.5F, Interpolation.CURVE3), old);
	}
}
