package me.alpha432.oyvey.event.impl.entity.player;

import me.alpha432.oyvey.event.Event;
import net.minecraft.world.entity.Entity;

public class AttackEntityEvent extends Event {
    private final Entity entity;

    public AttackEntityEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
