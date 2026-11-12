package raccoonman.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.SystemReport;
import raccoonman.reterraforged.server.WorldInfo;

/**
 * The world's details (WorldInfo) in every crash report, the server watchdog's too: each one makes its SystemReport in
 * CrashReport's constructor. A mixin: Fabric has no event for it, NeoForge's crash callables would be a second way
 * for one loader (Fabric API adds its mod list here the same way). Only a snapshot is read, never the world.
 */
@Mixin(SystemReport.class)
public abstract class MixinSystemReport {

	@Inject(method = "<init>", at = @At("RETURN"))
	private void reterraforged$addWorldInfo(CallbackInfo callback) {
		// no world (the title screen, a client on another server): no section
		if (WorldInfo.current() != null) {
			// the CrashReportDetail overload catches what it throws (26.2 has it where 26.1 had a Supplier);
			// crashReportDetail does not throw anyway
			this.setDetail("ReTerraForged world", WorldInfo::crashReportDetail);
		}
	}

	@Shadow
	public abstract void setDetail(String id, net.minecraft.CrashReportDetail<Object> detail);
}
