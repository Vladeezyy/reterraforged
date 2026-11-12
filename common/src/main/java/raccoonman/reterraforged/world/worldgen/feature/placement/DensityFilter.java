package raccoonman.reterraforged.world.worldgen.feature.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

// on average density times every position that comes in: 0.5 one in two, 2.5 two of each and a third for every second.
// Only where the preset builds the world (a ReTerraForged context): the biome modifier puts it on a biome, and a biome is
// also in other mods' dimensions. Elsewhere every position goes through untouched, as without the filter; in worlds of
// generation version 0 everywhere, as their chunks were made
public final class DensityFilter implements PlacementModifier {
	public static final MapCodec<DensityFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.FLOAT.fieldOf("density").forGetter((f) -> f.density)
	).apply(instance, DensityFilter::new));

	private final float density;
	// the random state (one per dimension) the filter last ran in and whether it scales there: worked out again only when
	// a feature of another dimension comes, not for every position
	@Nullable
	private volatile Scope scope;

	public DensityFilter(float density) {
		this.density = density;
	}

	@Override
	public void modify(PlacementContext ctx, RandomSource random, BlockPos pos, Consumer<BlockPos> output) {
		if (!this.scales(ctx)) {
			output.accept(pos);
			return;
		}
		int count = (int) this.density;
		if (random.nextFloat() < this.density - count) {
			count++;
		}
		for (int i = 0; i < count; i++) {
			output.accept(pos);
		}
	}

	private boolean scales(PlacementContext ctx) {
		RandomState randomState = ctx.getLevel().getLevel().getChunkSource().randomState();
		Scope scope = this.scope;
		if (scope == null || scope.randomState() != randomState) {
			scope = new Scope(randomState, scalesIn(randomState, ctx.getLevel().registryAccess()));
			this.scope = scope;
		}
		return scope.scales();
	}

	public static boolean scalesIn(RandomState randomState, RegistryAccess registries) {
		boolean presetWorld = (Object) randomState instanceof RTFRandomState rtfRandomState && rtfRandomState.generatorContext() != null;
		return scales(presetWorld, registries.lookup(RTFRegistries.PRESET).flatMap((presets) -> presets.getOptional(Preset.KEY)).map((preset) -> preset.world().properties).orElse(null));
	}

	// a world of generation version 0 had the sliders on its biomes in every dimension
	public static boolean scales(boolean presetWorld, @Nullable WorldSettings.Properties properties) {
		return presetWorld || (properties != null && !properties.fixes(1));
	}

	@Override
	public MapCodec<DensityFilter> codec() {
		return CODEC;
	}

	private record Scope(RandomState randomState, boolean scales) {
	}
}
