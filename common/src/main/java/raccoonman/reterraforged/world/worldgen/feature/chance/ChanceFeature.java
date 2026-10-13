package raccoonman.reterraforged.world.worldgen.feature.chance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import raccoonman.reterraforged.world.worldgen.feature.chance.ChanceFeature.Config;
import raccoonman.reterraforged.world.worldgen.feature.util.PlaceContext;

public record ChanceFeature(Config config) implements Feature {
	public static final MapCodec<ChanceFeature> CODEC = Config.CODEC.fieldOf("config").xmap(ChanceFeature::new, ChanceFeature::config);


	@Override
	public MapCodec<ChanceFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		return this.place(new PlaceContext<>(this.config, level, chunkGenerator, random, origin));
	}

	private boolean place(PlaceContext<Config> ctx) {
		RandomSource random = ctx.random();
		Config config = ctx.config();
		
		int entryCount = config.entries.size();
		ChanceContext chanceCtx = ChanceContext.make(entryCount);
        for (int i = 0; i < entryCount; i++) {
            Entry entry = config.entries.get(i);
            float chance = entry.getChance(chanceCtx, ctx);
            chanceCtx.record(i, chance);
        }

        int index = chanceCtx.nextIndex(random);
        if (index > -1) {
            return config.entries.get(index).feature.value().place(ctx.level(), ctx.chunkGenerator(), random, ctx.origin());
        }
		return false;
	}
	
	public record Entry(Holder<PlacedFeature> feature, float chance, List<ChanceModifier> modifiers) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			PlacedFeature.CODEC.fieldOf("feature").forGetter(Entry::feature),
			Codec.FLOAT.fieldOf("chance").forGetter(Entry::chance),
			ChanceModifier.CODEC.listOf().fieldOf("modifiers").forGetter(Entry::modifiers)
		).apply(instance, Entry::new));
		
		public float getChance(ChanceContext chanceCtx, PlaceContext<?> placeCtx) {
			float chance = this.chance;
			for (ChanceModifier modifier : this.modifiers) {
				chance *= modifier.getChance(chanceCtx, placeCtx);
			}
			return chance;
		}
	}
	
	public record Config(List<Entry> entries) {
		public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Entry.CODEC.listOf().fieldOf("entries").forGetter(Config::entries)
		).apply(instance, Config::new));
	}
}
