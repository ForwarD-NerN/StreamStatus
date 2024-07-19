package ru.nern.streamstatus.mixin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.nern.streamstatus.IServerPlayerAccessor;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements IServerPlayerAccessor {

	@Unique
	private boolean isStreaming = false;

	@Override
	public boolean streamstatus$isStreaming() {
		return this.isStreaming;
	}

	@Override
	public void streamstatus$setStreaming(boolean flag) {
		this.isStreaming = flag;
	}

	@Inject(method = "copyFrom", at = @At("TAIL"))
	public void streamstatus$moveStreamData(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
		((IServerPlayerAccessor)this).streamstatus$setStreaming(((IServerPlayerAccessor) oldPlayer).streamstatus$isStreaming());
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void streamstatus$writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
		if(this.isStreaming) nbt.putBoolean("IsStreaming", true);
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void streamstatus$readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
		if(nbt.contains("IsStreaming")) this.streamstatus$setStreaming(true);
	}


}
