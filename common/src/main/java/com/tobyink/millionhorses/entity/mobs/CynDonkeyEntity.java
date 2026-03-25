package com.tobyink.millionhorses.entity.mobs;

import com.tobyink.millionhorses.entity.variant.CynDonkeyVariant;
import com.tobyink.millionhorses.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.tags.BiomeTags;
import org.jetbrains.annotations.Nullable;

public class CynDonkeyEntity extends AbstractMillionHorseEntity {

    // ── Synced data ──────────────────────────────────────────────────────────
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(CynDonkeyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEIGHT_SCALE =
            SynchedEntityData.defineId(CynDonkeyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DENSITY_SCALE =
            SynchedEntityData.defineId(CynDonkeyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TAIL_SCALE =
            SynchedEntityData.defineId(CynDonkeyEntity.class, EntityDataSerializers.FLOAT);

    // ── Constructor ──────────────────────────────────────────────────────────
    public CynDonkeyEntity(EntityType<? extends CynDonkeyEntity> type, Level level) {
        super(type, level);
    }

    // ── SynchedEntityData ────────────────────────────────────────────────────
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT,       0);
        this.entityData.define(HEIGHT_SCALE,  0.0f);
        this.entityData.define(DENSITY_SCALE, 0.0f);
        this.entityData.define(TAIL_SCALE,    1.0f);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public CynDonkeyVariant getCynDonkeyVariant() {
        return CynDonkeyVariant.byId(this.entityData.get(VARIANT));
    }

    public void setCynDonkeyVariant(CynDonkeyVariant variant) {
        this.entityData.set(VARIANT, variant.getId());
    }

    public float getHeightScale()  { return this.entityData.get(HEIGHT_SCALE); }
    public float getDensityScale() { return this.entityData.get(DENSITY_SCALE); }
    public float getTailScale()    { return this.entityData.get(TAIL_SCALE); }

    // ── Attributes ───────────────────────────────────────────────────────────
    public static AttributeSupplier.Builder createAttributes() {
        return createBaseMillionHorseAttributes()
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected double healthForGroup(RandomSource r, int g) {
        return randomInRange(r, 15.0 + g * 2.5, 17.5 + g * 2.5);
    }

    @Override
    protected double speedForGroup(RandomSource r, int g) {
        return randomInRange(r, 0.100 + g * 0.018, 0.118 + g * 0.018);
    }

    @Override
    protected double jumpForGroup(RandomSource r, int g) {
        return randomInRange(r, 0.40 + g * 0.04, 0.44 + g * 0.04);
    }

    @Override
    protected boolean canFly() { return false; }

    @Override
    public int getChestSize() { return 9; }

    // ── Variant randomization ────────────────────────────────────────────────
    @Override
    protected void randomizeVariant() {
        RandomSource rng = this.getRandom();
        setCynDonkeyVariant(CynDonkeyVariant.byId(rng.nextInt(3)));
        this.entityData.set(HEIGHT_SCALE,  lerp(rng, -0.04f,  0.075f));
        this.entityData.set(DENSITY_SCALE, lerp(rng, -0.075f, 0.03f));
        this.entityData.set(TAIL_SCALE,    lerp(rng,  0.75f,  1.25f));
        healthGroupId = rng.nextInt(6);
        speedGroupId  = rng.nextInt(6);
        jumpGroupId   = rng.nextInt(6);
    }

    private static float lerp(RandomSource rng, float min, float max) {
        return min + rng.nextFloat() * (max - min);
    }

    // ── NBT ──────────────────────────────────────────────────────────────────
    @Override
    protected void saveVariantData(CompoundTag tag) {
        tag.putInt("CynDonkeyVariant", getCynDonkeyVariant().getId());
        tag.putFloat("HeightScale",    getHeightScale());
        tag.putFloat("DensityScale",   getDensityScale());
        tag.putFloat("TailScale",      getTailScale());
    }

    @Override
    protected boolean loadVariantData(CompoundTag tag) {
        if (!tag.contains("CynDonkeyVariant")) return false;
        setCynDonkeyVariant(CynDonkeyVariant.byId(tag.getInt("CynDonkeyVariant")));
        if (tag.contains("HeightScale"))
            this.entityData.set(HEIGHT_SCALE,  tag.getFloat("HeightScale"));
        if (tag.contains("DensityScale"))
            this.entityData.set(DENSITY_SCALE, tag.getFloat("DensityScale"));
        if (tag.contains("TailScale"))
            this.entityData.set(TAIL_SCALE,    tag.getFloat("TailScale"));
        return true;
    }

    // ── Breeding ─────────────────────────────────────────────────────────────
    @Override
    public boolean canMate(Animal other) {
        if (!this.isTamed() || !this.isInLove()) return false;
        if (other instanceof CynDonkeyEntity d) return d.isTamed() && d.isInLove();
        // Donkey × CynHorse → Mule (futuro)
        // Donkey × Zebra   → Zonkey (futuro)
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        if (!(mate instanceof CynDonkeyEntity other)) return null;

        RandomSource rng = this.getRandom();
        CynDonkeyEntity foal = EntityRegistry.CYN_DONKEY.get().create(level);
        if (foal == null) return null;

        foal.setCynDonkeyVariant(
                rng.nextBoolean() ? this.getCynDonkeyVariant() : other.getCynDonkeyVariant());
        foal.entityData.set(HEIGHT_SCALE,  lerp(rng,
                (this.getHeightScale()  + other.getHeightScale())  / 2f - 0.02f,
                (this.getHeightScale()  + other.getHeightScale())  / 2f + 0.02f));
        foal.entityData.set(DENSITY_SCALE, lerp(rng,
                (this.getDensityScale() + other.getDensityScale()) / 2f - 0.02f,
                (this.getDensityScale() + other.getDensityScale()) / 2f + 0.02f));
        foal.entityData.set(TAIL_SCALE,    lerp(rng,
                (this.getTailScale()    + other.getTailScale())    / 2f - 0.05f,
                (this.getTailScale()    + other.getTailScale())    / 2f + 0.05f));
        foal.randomizeAttributesFromParents(rng, this, other);
        foal.variantSetByNbt = true;
        return foal;
    }

    // ── Death drops ──────────────────────────────────────────────────────────
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        int leather = this.getRandom().nextInt(3) + looting;
        if (leather > 0) this.spawnAtLocation(new ItemStack(Items.LEATHER, leather));
    }

    // ── Spawn rules ───────────────────────────────────────────────────────────
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level,
                                   MobSpawnType reason) {
        BlockPos pos = this.blockPosition();
        if (level.getRawBrightness(pos, 0) < 7) return false;
        if (!level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK))
            return false;
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome =
                level.getBiome(pos);
        // is_plains cubre plains, sunflower_plains y biomas de plains de otros mods
        // meadow no tiene tag en 1.20.1 así que lo chequeamos directo
        return biome.is(BiomeTags.IS_OVERWORLD) && (
                biome.is(net.minecraft.world.level.biome.Biomes.PLAINS)
                        || biome.is(net.minecraft.world.level.biome.Biomes.SUNFLOWER_PLAINS)
                        || biome.is(net.minecraft.world.level.biome.Biomes.MEADOW));
    }

    public static boolean checkCynDonkeySpawnRules(
            EntityType<CynDonkeyEntity> type,
            ServerLevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random) {
        if (level.getRawBrightness(pos, 0) < 7) return false;
        if (!level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK))
            return false;
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome =
                level.getBiome(pos);
        return biome.is(BiomeTags.IS_OVERWORLD) && (
                biome.is(net.minecraft.world.level.biome.Biomes.PLAINS)
                        || biome.is(net.minecraft.world.level.biome.Biomes.SUNFLOWER_PLAINS)
                        || biome.is(net.minecraft.world.level.biome.Biomes.MEADOW));
    }
}