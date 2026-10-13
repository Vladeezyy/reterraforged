package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.MaterialSystem;

@Mixin(MaterialRuleContext.class)
public interface MaterialRuleContextAccessor {

	@Accessor("system")
	MaterialSystem reterraforged$system();

	@Accessor("randomState")
	RandomState reterraforged$randomState();
}
