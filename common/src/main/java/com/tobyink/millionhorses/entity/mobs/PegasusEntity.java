package com.tobyink.millionhorses.entity.mobs;

import com.tobyink.millionhorses.entity.variant.PegasusVariant;
import com.tobyink.millionhorses.entity.mobs.AlicornEntity;
import com.tobyink.millionhorses.entity.mobs.UnicornEntity;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class PegasusEntity extends AbstractMillionHorseEntity {

    private static final EntityDataAccessor<Integer> PEGASUS_VARIANT =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.INT);

    public PegasusEntity(EntityType<? extends AbstractChestedHorse> type, Level level) {
        super(type, level);
    }

    // =========================================================================
    // Variant
    // =========================================================================

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PEGASUS_VARIANT, 0);
    }

    public PegasusVariant getPegasusVariant() {
        return PegasusVariant.byId(this.entityData.get(PEGASUS_VARIANT));
    }

    public void setPegasusVariant(PegasusVariant variant) {
        this.entityData.set(PEGASUS_VARIANT, variant.getId());
    }

    @Override
    protected void randomizeVariant() {
        this.setPegasusVariant(randomPegasusVariant());
    }

    @Override
    protected void saveVariantData(CompoundTag tag) {
        tag.putInt("PegasusVariant", this.getPegasusVariant().getId());
    }

    @Override
    protected boolean loadVariantData(CompoundTag tag) {
        if (!tag.contains("PegasusVariant")) return false;
        this.setPegasusVariant(PegasusVariant.byId(tag.getInt("PegasusVariant")));
        return true;
    }

    private PegasusVariant randomPegasusVariant() {
        int roll = this.random.nextInt(100);
        if (roll < 25) return PegasusVariant.WHITE;
        if (roll < 50) return PegasusVariant.PURPLE;
        if (roll < 75) return PegasusVariant.RAINBOW;
        if (roll < 81) return PegasusVariant.DARK;
        if (roll < 87) return PegasusVariant.WHITE_BLUE;
        if (roll < 93) return PegasusVariant.PURPLE_BLUE;
        if (roll < 99) return PegasusVariant.RAINBOW_BLUE;
        return PegasusVariant.DARK_BLUE;
    }

    // =========================================================================
    // Attributes
    // =========================================================================

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseMillionHorseAttributes();
    }

    // Pegasus: mismos rangos que vanilla (abstract defaults)
    // No override — hereda healthForGroup/speedForGroup/jumpForGroup de la abstract

    // =========================================================================
    // Breeding — pegasus x pegasus -> pegasus
    // =========================================================================

    @Override
    public boolean canMate(Animal other) {
        if (other instanceof PegasusEntity partner)
            return this.isTamed() && partner.isTamed()
                    && this.isInLove() && partner.isInLove();
        if (other instanceof UnicornEntity unicorn)
            return this.isTamed() && unicorn.isTamed()
                    && this.isInLove() && unicorn.isInLove();
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        // Pegasus × Unicorn → Alicorn
        if (mate instanceof UnicornEntity unicorn) {
            AlicornEntity foal = com.tobyink.millionhorses.registry.EntityRegistry.ALICORN.get().create(level);
            if (foal == null) return null;
            foal.randomizeAttributesFromParents(this.random, this, unicorn);
            foal.variantSetByNbt = true;
            return foal;
        }

        // Pegasus × Pegasus → Pegasus
        if (!(mate instanceof PegasusEntity other)) return null;

        @SuppressWarnings("unchecked")
        PegasusEntity foal = new PegasusEntity(
                (EntityType<? extends AbstractChestedHorse>) this.getType(), level);

        boolean thisBlue  = this.getPegasusVariant().name().endsWith("_BLUE");
        boolean otherBlue = other.getPegasusVariant().name().endsWith("_BLUE");
        PegasusVariant parentBase = this.random.nextBoolean()
                ? this.getPegasusVariant() : other.getPegasusVariant();
        boolean foalBlue = this.random.nextBoolean() ? thisBlue : otherBlue;
        String baseName = parentBase.name().replace("_BLUE", "");
        PegasusVariant foalVariant;
        try {
            foalVariant = PegasusVariant.valueOf(foalBlue ? baseName + "_BLUE" : baseName);
        } catch (IllegalArgumentException e) {
            foalVariant = parentBase;
        }

        foal.setPegasusVariant(foalVariant);
        foal.variantSetByNbt = true;
        foal.randomizeAttributesFromParents(this.random, this, other);
        return foal;
    }

    // =========================================================================
    // Death drops
    // =========================================================================

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        int leather = this.random.nextInt(3) + looting;
        if (leather > 0) this.spawnAtLocation(new ItemStack(Items.LEATHER, leather));
        int feathers = 1 + this.random.nextInt(3) + looting;
        this.spawnAtLocation(new ItemStack(Items.FEATHER, feathers));
        if (this.random.nextBoolean())
            this.spawnAtLocation(new ItemStack(Items.QUARTZ, 1 + looting));
        if (this.random.nextFloat() < 0.1f + looting * 0.05f)
            this.spawnAtLocation(new ItemStack(Items.GHAST_TEAR, 1));
    }

    // =========================================================================
    // Spawn rules
    // =========================================================================

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level,
                                   MobSpawnType reason) {
        if (this.blockPosition().getY() < 175) return false;
        return level.getBlockState(this.blockPosition().below())
                .is(net.minecraft.world.level.block.Blocks.HAY_BLOCK);
    }

    public static boolean checkPegasusSpawnRules(
            EntityType<PegasusEntity> type,
            ServerLevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random) {
        if (pos.getY() < 175) return false;
        return level.getBlockState(pos.below())
                .is(net.minecraft.world.level.block.Blocks.HAY_BLOCK);
    }
}