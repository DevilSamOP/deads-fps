package me.alpha432.oyvey.mixin;

import me.alpha432.oyvey.event.impl.entity.player.AttackEntityEvent;
import me.alpha432.oyvey.event.impl.entity.player.DoAttackEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.alpha432.oyvey.util.traits.Util.EVENT_BUS;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void startAttack(CallbackInfo ci) {
        EVENT_BUS.post(new DoAttackEvent());

        Minecraft minecraft = (Minecraft) (Object) this;

        if (minecraft.hitResult instanceof EntityHitResult entityHitResult) {
            EVENT_BUS.post(new AttackEntityEvent(entityHitResult.getEntity()));
        }
    }
}
