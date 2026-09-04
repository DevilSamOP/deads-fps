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

    private final Setting<Integer> targetSlot =
            num("TargetSlot", 1, 1, 9);

    private final Setting<Boolean> swapOnMiss =
            bool("SwapOnMiss", false);

    private final Setting<Boolean> swapBack =
            bool("SwapBack", true);

    private final Setting<Integer> swapBackDelay =
            num("SwapBackDelay", 2, 0, 100);

    private final Setting<Boolean> smartShieldBreak =
            bool("ShieldBreaker", true);

    private final Setting<Boolean> smartDurability =
            bool("DurabilitySaver", true);

    private final Setting<Boolean> swordSwapping =
            bool("SwordSwapping", true);

    private final Setting<Boolean> maceSwapping =
            bool("MaceSwapping", true);

    private final Setting<Boolean> spearSwapping =
            bool("SpearSwapping", true);

    private final Setting<Boolean> otherSwapping =
            bool("OtherSwapping", true);

    private final Setting<Boolean> enchantFireAspect =
            bool("FireAspect", true);

    private final Setting<Boolean> enchantLooting =
            bool("Looting", true);

    private final Setting<Boolean> enchantSharpness =
            bool("Sharpness", true);

    private final Setting<Boolean> enchantSmite =
            bool("Smite", true);

    private final Setting<Boolean> enchantBaneOfArthropods =
            bool("BaneOfArthropods", true);

    private final Setting<Boolean> enchantSweepingEdge =
            bool("SweepingEdge", true);

    private final Setting<Boolean> regularMace =
            bool("RegularMace", true);

    private final Setting<Boolean> enchantDensity =
            bool("Density", true);

    private final Setting<Boolean> enchantBreach =
            bool("Breach", true);

    private final Setting<Boolean> enchantWindBurst =
            bool("WindBurst", true);

    private final Setting<Boolean> enchantImpaling =
            bool("Impaling", true);

    private final Setting<Boolean> enchantLunge =
            bool("Lunge", true);

    private final Setting<Boolean> spearHitbox =
            bool("Hitbox", true);

    private final Setting<Boolean> excludeLungeFromHitbox =
            bool("ExcludeLungeFromHitbox", true);

    private final Setting<Boolean> onlyOnWeapon =
            bool("OnlyOnWeapon", false);

    private final Setting<Boolean> sword =
            bool("Sword", true);

    private final Setting<Boolean> axe =
            bool("Axe", true);

    private final Setting<Boolean> pickaxe =
            bool("Pickaxe", true);

    private final Setting<Boolean> shovel =
            bool("Shovel", true);

    private final Setting<Boolean> hoe =
            bool("Hoe", true);

    private final Setting<Boolean> mace =
            bool("Mace", true);

    private final Setting<Boolean> trident =
            bool("Trident", true);

    private int backTimer;
    private boolean awaitingBack;
    private int lastSlot;

    public AttributeSwapModule() {
        super("AttributeSwap", "Swaps to the best weapon when attacking.", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        backTimer = 0;
        awaitingBack = false;
    }

    @Subscribe
    public void onAttack(DoAttackEvent event) {
        if (mc.hitResult == null) return;

        if (mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return;
        }

        if (!canSwapByWeapon()) {
            return;
        }

        if (mode.getValue().equals("Smart") && spearSwapping.getValue()) {

            if (spearHitbox.getValue()) {
                Entity target = getTargetEntity();

                if (target != null) {
                    if (mc.player.distanceTo(target)
                            <= mc.player.entityInteractionRange() + 0.5) {
                        return;
                    }

                    int spearSlot = getSmartSpearSlot(false);

                    if (spearSlot != -1) {
                        doSwap(spearSlot);
                        return;
                    }
                }
            }

            if (enchantLunge.getValue()) {
                int lungeSlot = getSmartSpearSlot(true);

                if (lungeSlot != -1) {
                    doSwap(lungeSlot);
                    return;
                }
            }
        }

        if (mode.getValue().equals("Smart") || !swapOnMiss.getValue()) {
            return;
        }

        doSwap(targetSlot.getValue() - 1);
    }

    @Subscribe
    public void onAttackEntity(AttackEntityEvent event) {
        if (!canSwapByWeapon()) {
            return;
        }

        if (mode.getValue().equals("Simple") && swapOnMiss.getValue()) {
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

    private void doSwap(int slotIndex) {
        if (awaitingBack) return;

        if (slotIndex < 0 || slotIndex > 8) {
            return;
        }

        if (slotIndex == mc.player.getInventory().getSelectedSlot()) {
            return;
        }

        lastSlot = mc.player.getInventory().getSelectedSlot();

        InventoryUtil.swap(slotIndex);

        awaitingBack = swapBack.getValue();

        if (awaitingBack) {
            backTimer = swapBackDelay.getValue();
        }
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (!awaitingBack) {
            return;
        }

        if (backTimer-- > 0) {
            return;
        }

        InventoryUtil.swap(lastSlot);
        awaitingBack = false;
    }

    private boolean canSwapByWeapon() {
        if (!onlyOnWeapon.getValue()) {
            return true;
        }

        ItemStack item = mc.player.getMainHandItem();

        return (sword.getValue() && item.is(ItemTags.SWORDS))
                || (axe.getValue() && item.is(ItemTags.AXES))
                || (pickaxe.getValue() && item.is(ItemTags.PICKAXES))
                || (shovel.getValue() && item.is(ItemTags.SHOVELS))
                || (hoe.getValue() && item.is(ItemTags.HOES))
                || (mace.getValue() && item.getItem() instanceof MaceItem)
                || (trident.getValue() && item.getItem() instanceof TridentItem);
    }

    private int getSmartSlot(Entity target) {

        ItemStack currentStack = mc.player.getMainHandItem();

        if (target != null
                && smartShieldBreak.getValue()
                && target instanceof LivingEntity living
                && living.isBlocking()) {

            if (currentStack.getItem() instanceof AxeItem) {
                return -1;
            }

            for (int i = 0; i < 9; i++) {
                if (i == mc.player.getInventory().getSelectedSlot()) {
                    continue;
                }

                ItemStack stack = mc.player.getInventory().getItem(i);

                if (stack.getItem() instanceof AxeItem) {
                    return i;
                }
            }
        }

        boolean isFalling = mc.player.fallDistance > 1.5f;
        boolean durability = smartDurability.getValue();

        boolean isLiving = target instanceof LivingEntity;
        boolean isPlayer = target instanceof Player;
        boolean isOnFire = target != null && target.isOnFire();

        boolean isUndead = target != null
                && target.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE);

        boolean isArthropod = target != null
                && target.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS);

        boolean isAquatic = target != null
                && target.getType().is(EntityTypeTags.SENSITIVE_TO_IMPALING);

        boolean hasFireResistance = isLiving
                && (((LivingEntity) target).hasEffect(MobEffects.FIRE_RESISTANCE)
                || hasFireProtectionArmor((LivingEntity) target));

        double armor = isLiving
                ? ((LivingEntity) target).getAttributeValue(Attributes.ARMOR)
                : 0;

        float health = isLiving
                ? ((LivingEntity) target).getHealth()
                : 0;

        int bestSlot = -1;

        double bestScore = getItemScore(
                currentStack,
                isFalling,
                durability,
                isLiving,
                isPlayer,
                isOnFire,
                hasFireResistance,
                isUndead,
                isArthropod,
                isAquatic,
                armor,
                health
        );

        for (int i = 0; i < 9; i++) {

            if (i == mc.player.getInventory().getSelectedSlot()) {
                continue;
            }

            ItemStack stack = mc.player.getInventory().getItem(i);

            if (stack.isEmpty() && !durability) {
                continue;
            }

            double score = getItemScore(
                    stack,
                    isFalling,
                    durability,
                    isLiving,
                    isPlayer,
                    isOnFire,
                    hasFireResistance,
                    isUndead,
                    isArthropod,
                    isAquatic,
                    armor,
                    health
            );

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private int getSmartSpearSlot(boolean requireLunge) {

        for (int i = 0; i < 9; i++) {

            if (i == mc.player.getInventory().getSelectedSlot()) {
                continue;
            }

            ItemStack stack = mc.player.getInventory().getItem(i);

            if (!stack.is(ItemTags.SPEARS)) {
                continue;
            }

            boolean hasLunge =
                    EnchantmentUtil.getLevel(Enchantments.LUNGE, stack) > 0;

            if (requireLunge && !hasLunge) {
                continue;
            }

            if (!requireLunge
                    && excludeLungeFromHitbox.getValue()
                    && hasLunge) {
                continue;
            }

            return i;
        }

        return -1;
    }

    private Entity getTargetEntity() {

        double maxDistance = 7;

        Vec3 start = mc.player.getEyePosition(1.0f);
        Vec3 look = mc.player.getViewVector(1.0f);
        Vec3 end = start.add(look.scale(maxDistance));

        AABB box = mc.player.getBoundingBox()
                .expandTowards(look.scale(maxDistance))
                .inflate(1.0);

        Entity target = null;

        double closestDistance = maxDistance * maxDistance;

        for (Entity entity : mc.level.getEntities(
                mc.player,
                box,
                e -> !e.isSpectator() && e.isPickable())) {

            AABB expandedBox = entity.getBoundingBox().inflate(0.150);

            if (expandedBox.clip(start, end).isPresent()) {

                double distSq = start.distanceToSqr(
                        entity.getX(),
                        entity.getY(),
                        entity.getZ()
                );

                if (distSq < closestDistance) {
                    closestDistance = distSq;
                    target = entity;
                }
            }
        }

        return target;
    }

    private double getItemScore(
            ItemStack stack,
            boolean isFalling,
            boolean durability,
            boolean isLiving,
            boolean isPlayer,
            boolean isOnFire,
            boolean hasFireResistance,
            boolean isUndead,
            boolean isArthropod,
            boolean isAquatic,
            double armor,
            float health) {

        double score = 0;

        if (durability) {
            score += getDurabilityScore(stack);
        }

        if (stack.isEmpty()) {
            return score;
        }

        score += getCombatScore(
                stack,
                isFalling,
                isLiving,
                isPlayer,
                isOnFire,
                hasFireResistance,
                isUndead,
                isArthropod,
                isAquatic,
                armor,
                health
        );

        return score;
    }

    private double getDurabilityScore(ItemStack stack) {

        if (!stack.isDamageableItem()) {
            return 4;
        }

        int unbreaking =
                EnchantmentUtil.getLevel(Enchantments.UNBREAKING, stack);

        if (unbreaking > 0) {
            return unbreaking * 0.05;
        }

        return 0;
    }

    private double getCombatScore(
            ItemStack stack,
            boolean isFalling,
            boolean isLiving,
            boolean isPlayer,
            boolean isOnFire,
            boolean hasFireResistance,
            boolean isUndead,
            boolean isArthropod,
            boolean isAquatic,
            double armor,
            float health) {

        double score = 0;

        if (swordSwapping.getValue()) {
            score += getFireAspectScore(
                    stack, isOnFire, hasFireResistance);

            score += getLootingScore(
                    stack, isLiving, isPlayer, isOnFire, health);

            score += getSharpnessScore(
                    stack, isOnFire);

            score += getSmiteScore(
                    stack, isUndead, isOnFire);

            score += getBaneOfArthropodsScore(
                    stack, isArthropod, isOnFire);

            score += getSweepingEdgeScore(stack);
        }

        if (maceSwapping.getValue()) {
            score += getBreachScore(
                    stack, isLiving, armor);

            score += getDensityScore(
                    stack, isFalling);

            score += getWindBurstScore(
                    stack, isFalling);

            score += getMaceScore(
                    stack, isFalling);
        }

        if (otherSwapping.getValue()) {
            score += getImpalingScore(
                    stack, isAquatic);
        }

        return score;
    }

    private double getFireAspectScore(
            ItemStack stack,
            boolean isOnFire,
            boolean hasFireResistance) {

        if (!enchantFireAspect.getValue()
                || isOnFire
                || hasFireResistance) {
            return 0;
        }

        int level =
                EnchantmentUtil.getLevel(Enchantments.FIRE_ASPECT, stack);

        return level > 0 ? 30 : 0;
    }

    private double getLootingScore(
            ItemStack stack,
            boolean isLiving,
            boolean isPlayer,
            boolean isOnFire,
            float health) {

        if (!enchantLooting.getValue() || isPlayer) {
            return 0;
        }

        int level =
                EnchantmentUtil.getLevel(Enchantments.LOOTING, stack);

        if (level > 0) {
            boolean execute =
                    (isLiving && health < 20) || isOnFire;

            return level * (execute ? 10 : 5);
        }

        return 0;
    }

    private double getSharpnessScore(
            ItemStack stack,
            boolean isOnFire) {

        if (!enchantSharpness.getValue()) {
            return 0;
        }

        int level =
                EnchantmentUtil.getLevel(Enchantments.SHARPNESS, stack);

        if (level > 0) {
            double baseScore =
                    (1 + 0.5 * (level - 1)) * 3;

            return isOnFire
                    ? baseScore * 1.5
                    : baseScore;
        }

        return 0;
    }

    private double getSmiteScore(
            ItemStack stack,
            boolean isUndead,
            boolean isOnFire) {

        if (!enchantSmite.getValue() || !isUndead) {
            return 0;
        }

        int level =
                EnchantmentUtil.getLevel(Enchantments.SMITE, stack);

        if (level > 0) {
            double baseScore = level * 5;

            return isOnFire
                    ? baseScore * 1.5
                    : baseScore;
        }

        return 0;
    }

    private double getBaneOfArthropodsScore(
            ItemStack stack,
            boolean isArthropod,
            boolean isOnFire) {

        if (!enchantBaneOfArthropods.getValue()
                || !isArthropod) {
            return 0;
        }

        int level =
                EnchantmentUtil.getLevel(
                        Enchantments.BANE_OF_ARTHROPODS,
                        stack
                );

        if (level > 0) {
            double baseScore = level * 5;

            return isOnFire
                    ? baseScore * 1.5
                    : baseScore;
        }

        return 0;
    }

    private double getSweepingEdgeScore(ItemStack stack) {

        if (!enchantSweepingEdge.getValue()) {
            return 0;
        }

        int level =
                EnchantmentUtil.getLevel(
                        Enchantments.SWEEPING_EDGE,
                        stack
                );

        return level > 0 ? level * 3 : 0;
    }

    private double getImpalingScore(
            ItemStack stack,
            boolean isAquatic) {

        if (!enchantImpaling.getValue()
                || !isAquatic) {
            return 0;
        }

        int level =
                EnchantmentUtil.getLevel(
                        Enchantments.IMPALING,
                        stack
                );

        return level > 0 ? level * 5 : 0;
    }

    private double getBreachScore(
            ItemStack stack,
            boolean isLiving,
            double armor) {

        if (!enchantBreach.getValue()
                || !isLiving
                || armor <= 0) {
            return 0;
        }

        int level =
                EnchantmentUtil.getLevel(
                        Enchantments.BREACH,
                        stack
                );

        return level > 0
                ? level * armor * 0.3
                : 0;
    }

    private double getDensityScore(
            ItemStack stack,
            boolean isFalling) {

        if (!enchantDensity.getValue()
                || !isFalling) {
            return 0;
        }

        int level =
                EnchantmentUtil.getLevel(
                        Enchantments.DENSITY,
                        stack
                );

        return level > 0
                ? 50 + (level * mc.player.fallDistance * 2)
                : 0;
    }

    private double getWindBurstScore(
            ItemStack stack,
            boolean isFalling) {

        if (!enchantWindBurst.getValue()
                || !isFalling) {
            return 0;
        }

        int level =
                EnchantmentUtil.getLevel(
                        Enchantments.WIND_BURST,
                        stack
                );

        return level > 0 ? level * 20 : 0;
    }

    private double getMaceScore(
            ItemStack stack,
            boolean isFalling) {

        if (!regularMace.getValue()
                || !isFalling) {
            return 0;
        }

        return stack.getItem() instanceof MaceItem
                ? 40
                : 0;
    }

    private boolean hasFireProtectionArmor(
            LivingEntity entity) {

        for (var slot : EquipmentSlotGroup.ARMOR) {

            ItemStack stack =
                    entity.getItemBySlot(slot);

            if (stack.isEmpty()) {
                continue;
            }

            int fireProtection =
                    EnchantmentUtil.getLevel(
                            Enchantments.FIRE_PROTECTION,
                            stack
                    );

            if (fireProtection > 0) {
                return true;
            }
        }

        return false;
    }
            }
