package com.tobyink.millionhorses.entity.mobs;

import com.tobyink.millionhorses.entity.variant.AlicornVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class AlicornEntity extends AbstractMillionHorseEntity {

    private static final EntityDataAccessor<Integer> ALICORN_VARIANT =
            SynchedEntityData.defineId(AlicornEntity.class, EntityDataSerializers.INT);

    public AlicornEntity(EntityType<? extends AbstractChestedHorse> type, Level level) {
        super(type, level);
    }

    // =========================================================================
    // Variant
    // =========================================================================

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ALICORN_VARIANT, 0);
    }

    public AlicornVariant getAlicornVariant() {
        return AlicornVariant.byId(this.entityData.get(ALICORN_VARIANT));
    }

    public void setAlicornVariant(AlicornVariant variant) {
        this.entityData.set(ALICORN_VARIANT, variant.getId());
    }

    @Override
    protected void randomizeVariant() {
        this.setAlicornVariant(randomAlicornVariant());
    }

    @Override
    protected void saveVariantData(CompoundTag tag) {
        tag.putInt("AlicornVariant", this.getAlicornVariant().getId());
    }

    @Override
    protected boolean loadVariantData(CompoundTag tag) {
        if (!tag.contains("AlicornVariant")) return false;
        this.setAlicornVariant(AlicornVariant.byId(tag.getInt("AlicornVariant")));
        return true;
    }

    private AlicornVariant randomAlicornVariant() {
        // Weights: 0→25, 1→25, 2→25, 3→6, 4→6, 5→6, 6→6, 7→1
        int roll = this.random.nextInt(100);
        if (roll < 25) return AlicornVariant.byId(0);
        if (roll < 50) return AlicornVariant.byId(1);
        if (roll < 75) return AlicornVariant.byId(2);
        if (roll < 81) return AlicornVariant.byId(3);
        if (roll < 87) return AlicornVariant.byId(4);
        if (roll < 93) return AlicornVariant.byId(5);
        if (roll < 99) return AlicornVariant.byId(6);
        return AlicornVariant.byId(7);
    }

    // =========================================================================
    // Attributes — Alicorn has higher jump cap than Pegasus
    // =========================================================================

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseMillionHorseAttributes()
                .add(Attributes.ATTACK_DAMAGE, 5.0); // Bedrock: melee_box_attack damage 5
    }

    // Alicorn jump: groups 0-9 → [0.70+N*0.06, 0.76+N*0.06], max [1.24, 1.30]
    @Override
    protected double jumpForGroup(RandomSource r, int g) {
        return randomInRange(r, 0.70 + g * 0.06, 0.76 + g * 0.06);
    }

    // =========================================================================
    // Breeding — alicorn x alicorn -> alicorn only
    // =========================================================================

    @Override
    public boolean canMate(Animal other) {
        if (!(other instanceof AlicornEntity partner)) return false;
        return this.isTamed() && partner.isTamed()
                && this.isInLove() && partner.isInLove();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        AlicornEntity foal = new AlicornEntity(
                com.tobyink.millionhorses.registry.EntityRegistry.ALICORN.get(), level);
        foal.setAlicornVariant(randomAlicornVariant());
        foal.variantSetByNbt = true;
        if (mate instanceof AbstractMillionHorseEntity horseMate) {
            foal.randomizeAttributesFromParents(this.random, this, horseMate);
        } else {
            foal.randomizeAttributes(this.random);
        }
        return foal;
    }

    // =========================================================================
    // Death drops
    // =========================================================================

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        int leather = 1 + this.random.nextInt(3) + looting;
        this.spawnAtLocation(new ItemStack(Items.LEATHER, leather));
        int bones = this.random.nextInt(2) + looting;
        if (bones > 0) this.spawnAtLocation(new ItemStack(Items.BONE, bones));
        if (this.random.nextFloat() < 0.15f + looting * 0.05f)
            this.spawnAtLocation(new ItemStack(Items.PHANTOM_MEMBRANE, 1));
    }

    // =========================================================================
    // Spawn rules (same as Pegasus — hay block Y>=175)
    // =========================================================================

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level,
                                   MobSpawnType reason) {
        if (this.blockPosition().getY() < 175) return false;
        return level.getBlockState(this.blockPosition().below())
                .is(net.minecraft.world.level.block.Blocks.HAY_BLOCK);
    }

    public static boolean checkAlicornSpawnRules(
            EntityType<AlicornEntity> type,
            ServerLevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random) {
        if (pos.getY() < 175) return false;
        return level.getBlockState(pos.below())
                .is(net.minecraft.world.level.block.Blocks.HAY_BLOCK);
    }
}