package com.thaumaturgicconcilium.mixin;

import com.thaumaturgicconcilium.boss.Ethereal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An ethereal player (Peeler of Matter or the Pontifex robe) walks through blocks, as TC4 Concilium's
 * {@code noClip = true} did. Player.tick resets {@code noPhysics} to "is spectator" every tick before moving, so this
 * sets it again just after that and before the move (both sides: the state is a synced attachment).
 */
@Mixin(Player.class)
public abstract class EtherealPlayerMixin {
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;updateIsUnderwater()Z"))
    private void thaumaturgicconcilium$ethereal(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (Ethereal.isEthereal(self)) {
            self.noPhysics = true;
        }
    }
}
