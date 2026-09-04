package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.event.impl.entity.player.AttackEntityEvent;
import me.alpha432.oyvey.event.impl.entity.player.DoAttackEvent;
import me.alpha432.oyvey.event.impl.entity.player.TickEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.EnchantmentUtil;
import me.alpha432.oyvey.util.inventory.InventoryUtil;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AttributeSwapModule extends Module {

    private final Setting<String> mode = str("Mode", "Simple");
    private final Setting<Integer> targetSlot = num("TargetSlot", 1, 1, 9);
    private final Setting<Boolean> swapOnMiss = bool("SwapOnMiss", false);
    private final Setting<Boolean> swapBack = bool("SwapBack", true);
    private final Setting<Integer> swapBackDelay = num("SwapBackDelay", 2, 0, 100);

    private final Setting<Boolean> smartShieldBreak = bool("ShieldBreaker", true);
    private final Setting<Boolean> smartDurability = bool("DurabilitySaver", true);

    private final Setting<Boolean> swordSwapping = bool("SwordSwapping", true);
    private final Setting<Boolean> maceSwapping = bool("MaceSwapping", true);
    private final Setting<Boolean> spearSwapping = bool("SpearSwapping", true);
    private final Setting<Boolean> otherSwapping = bool("OtherSwapping", true);

    private final Setting<Boolean> enchantFireAspect = bool("FireAspect", true);
    private final Setting<Boolean> enchantLooting = bool("Looting", true);
    private final Setting<Boolean> enchantSharpness = bool("Sharpness", true);
    private final Setting<Boolean> enchantSmite = bool("Smite", true);
    private final Setting<Boolean> enchantBaneOfArthropods =
            bool("BaneOfArthropods", true);
    private final Setting<Boolean> enchantSweepingEdge =
            bool("SweepingEdge", true);

    private final Setting<Boolean> regularMace = bool("RegularMace", true);
    private final Setting<Boolean> enchantDensity = bool("Density", true);
    private final Setting<Boolean> enchantBreach = bool("Breach", true);
    private final Setting<Boolean> enchantWindBurst = bool("WindBurst", true);
    private final Setting<Boolean> enchantImpaling = bool("Impaling", true);

    private final Setting<Boolean> enchantLunge = bool("Lunge", true);
    private final Setting<Boolean> spearHitbox = bool("Hitbox", true);
    private final Setting<Boolean> excludeLungeFromHitbox =
            bool("ExcludeLungeFromHitbox", true);

    private final Setting<Boolean> onlyOnWeapon = bool("OnlyOnWeapon", false);
    private final Setting<Boolean> sword = bool("Sword", true);
    private final Setting<Boolean> axe = bool("Axe", true);
    private final Setting<Boolean> pickaxe = bool("Pickaxe", true);
    private final Setting<Boolean> shovel = bool("Shovel", true);
    private final Setting<Boolean> hoe = bool("Hoe", true);
    private final Setting<Boolean> mace = bool("Mace", true);
    private final Setting<Boolean> trident = bool("Trident", true);

    private int backTimer;
    private boolean awaitingBack;
    private int lastSlot;

    public AttributeSwapModule() {
        super(
                "AttributeSwap",
                "Swaps to the best weapon when attacking.",
                Category.COMBAT
        );
    }

    @Override
    public void onDisable() {
        backTimer = 0;
        awaitingBack = false;
    }

    @Subscribe
    public void onAttack(DoAttackEvent event) {
        if (mc.hitResult == null) return;

        if (mc.hitResult.getType() ==
                net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return;
        }

        if (!canSwapByWeapon()) return;

        if (mode.getValue().equals("Smart")
                && spearSwapping.getValue()) {

            if (spearHitbox.getValue()) {
                Entity target = getTargetEntity();

                if (target != null) {
                    if (mc.player.distanceTo(target)
                            <= mc.player.entityInteractionRange() + 0.5) {
                        return;
                    }

                    int slot = getSmartSpearSlot(false);

                    if (slot != -1) {
                        doSwap(slot);
                        return;
                    }
                }
            }

            if (enchantLunge.getValue()) {
                int slot = getSmartSpearSlot(true);

                if (slot != -1) {
                    doSwap(slot);
                    return;
                }
            }
        }

        if (mode.getValue().equals("Smart")
                || !swapOnMiss.getValue()) {
            return;
        }

        doSwap(targetSlot.getValue() - 1);
    }

    @Subscribe
    public void onAttackEntity(AttackEntityEvent event) {
        if (!canSwapByWeapon()) return;

        if (mode.getValue().equals("Simple")
                && swapOnMiss.getValue()) {
            return;
        }

        performSwap(event.getEntity());
    }

    private void performSwap(Entity target) {
        if (awaitingBack) return;

        if (mode.getValue().equals("Simple")) {
            doSwap(targetSlot.getValue() - 1);
        } else {
            doSwap(getSmartSlot(target));
        }
    }

    private void doSwap(int slot) {
        if (awaitingBack) return;
        if (slot < 0 || slot > 8) return;

        if (slot == mc.player.getInventory().getSelectedSlot()) {
            return;
        }

        lastSlot = mc.player.getInventory().getSelectedSlot();

        InventoryUtil.swap(slot);

        awaitingBack = swapBack.getValue();

        if (awaitingBack) {
            backTimer = swapBackDelay.getValue();
        }
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (!awaitingBack) return;

        if (backTimer-- > 0) return;

        InventoryUtil.swap(lastSlot);
        awaitingBack = false;
    }

    private boolean canSwapByWeapon() {
        if (!onlyOnWeapon.getValue()) return true;

        ItemStack item = mc.player.getMainHandItem();

        return (sword.getValue() && item.is(ItemTags.SWORDS))
                || (axe.getValue() && item.is(ItemTags.AXES))
                || (pickaxe.getValue() && item.is(ItemTags.PICKAXES))
                || (shovel.getValue() && item.is(ItemTags.SHOVELS))
                || (hoe.getValue() && item.is(ItemTags.HOES))
                || (mace.getValue()
                    && item.getItem() instanceof MaceItem)
                || (trident.getValue()
                    && item.getItem() instanceof TridentItem);
    }

    private int getSmartSlot(Entity target) {
        ItemStack current = mc.player.getMainHandItem();

        if (target != null
                && smartShieldBreak.getValue()
                && target instanceof LivingEntity living
                && living.isBlocking()) {

            if (current.getItem() instanceof
