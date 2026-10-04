package org.hypixelskyblockmods.crittersafariesp.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.hypixelskyblockmods.crittersafariesp.FloorDrops;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ParticlePacketMixin {
    @Inject(method="handleParticleEvent",at=@At("TAIL"))
    private void critterEsp$observeFloorDrop(ClientboundLevelParticlesPacket packet, CallbackInfo ci) { FloorDrops.observe(packet); }
}
