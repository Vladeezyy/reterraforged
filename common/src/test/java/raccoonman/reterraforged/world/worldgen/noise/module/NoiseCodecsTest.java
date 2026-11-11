package raccoonman.reterraforged.world.worldgen.noise.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import raccoonman.reterraforged.world.worldgen.noise.function.Interpolation;

class NoiseCodecsTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void octavesKeepTheCountsThatWorked() {
		for(int octaves : new int[] { 0, 1, 8, 30, 31, 200, NoiseCodecs.MAX_OCTAVES }) {
			assertEquals(octaves, read(NoiseCodecs.OCTAVES, octaves));
		}
		assertEquals(-5, read(NoiseCodecs.OCTAVES_ANY_LOW, -5));
		assertEquals(4, read(NoiseCodecs.TERRACE_STEPS, 4));
	}

	@Test
	void octavesRefuseTheCountsThatFailedOrHung() {
		assertTrue(fails(NoiseCodecs.OCTAVES, -1));
		assertTrue(fails(NoiseCodecs.OCTAVES, NoiseCodecs.MAX_OCTAVES + 1));
		assertTrue(fails(NoiseCodecs.OCTAVES, Integer.MAX_VALUE));
		assertTrue(fails(NoiseCodecs.OCTAVES_ANY_LOW, Integer.MAX_VALUE));
		assertTrue(fails(NoiseCodecs.TERRACE_STEPS, 0));
		assertTrue(fails(NoiseCodecs.TERRACE_STEPS, -3));
		assertTrue(fails(NoiseCodecs.TERRACE_STEPS, Integer.MAX_VALUE));
	}

	// the presets' erosion and ridges noise (PresetTerrainNoise) is in the datapacks of the worlds made so far
	@Test
	void perlinOfThePresetsStillReads() {
		Perlin perlin = new Perlin(0, 1.0F, 200, 2.0F, 0.5F, Interpolation.CURVE3);
		JsonElement json = Perlin.CODEC.codec().encodeStart(JsonOps.INSTANCE, perlin).getOrThrow();
		assertEquals(perlin, Perlin.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow());

		JsonObject negative = json.getAsJsonObject().deepCopy();
		negative.add("octaves", new JsonPrimitive(-1));
		assertTrue(Perlin.CODEC.codec().parse(JsonOps.INSTANCE, negative).error().isPresent());
	}

	@Test
	void billowRefusesANegativeCount() {
		Billow billow = new Billow(0.01F, 3, 2.0F, 0.5F, Interpolation.CURVE3);
		JsonObject json = Billow.CODEC.codec().encodeStart(JsonOps.INSTANCE, billow).getOrThrow().getAsJsonObject();
		assertEquals(billow, Billow.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow());
		json.add("octaves", new JsonPrimitive(-2));
		assertTrue(Billow.CODEC.codec().parse(JsonOps.INSTANCE, json).error().isPresent());
	}

	private static int read(Codec<Integer> codec, int value) {
		return codec.parse(JsonOps.INSTANCE, new JsonPrimitive(value)).getOrThrow();
	}

	private static boolean fails(Codec<Integer> codec, int value) {
		return codec.parse(JsonOps.INSTANCE, new JsonPrimitive(value)).error().isPresent();
	}
}
