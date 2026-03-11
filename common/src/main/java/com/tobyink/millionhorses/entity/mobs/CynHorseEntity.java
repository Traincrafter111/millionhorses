package com.tobyink.millionhorses.entity.mobs;

import com.tobyink.millionhorses.registry.EntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class CynHorseEntity extends AbstractMillionHorseEntity {

    // ── Synced data ──────────────────────────────────────────────────────────
    private static final EntityDataAccessor<Integer> BASE_ID =
            SynchedEntityData.defineId(CynHorseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PATTERN_ID =
            SynchedEntityData.defineId(CynHorseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HEAD_ID =
            SynchedEntityData.defineId(CynHorseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SOCK_ID =
            SynchedEntityData.defineId(CynHorseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MANE_ID =
            SynchedEntityData.defineId(CynHorseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEIGHT_SCALE =
            SynchedEntityData.defineId(CynHorseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DENSITY_SCALE =
            SynchedEntityData.defineId(CynHorseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TAIL_SCALE =
            SynchedEntityData.defineId(CynHorseEntity.class, EntityDataSerializers.FLOAT);

    // ── Constructor ──────────────────────────────────────────────────────────
    public CynHorseEntity(EntityType<? extends CynHorseEntity> type, Level level) {
        super(type, level);
    }

    // ── SynchedEntityData ────────────────────────────────────────────────────
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BASE_ID,       0);
        this.entityData.define(PATTERN_ID,    0);
        this.entityData.define(HEAD_ID,       0);
        this.entityData.define(SOCK_ID,       0);
        this.entityData.define(MANE_ID,       0);
        this.entityData.define(HEIGHT_SCALE,  0.0f);
        this.entityData.define(DENSITY_SCALE, 0.0f);
        this.entityData.define(TAIL_SCALE,    1.0f);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public int   getBaseId()       { return this.entityData.get(BASE_ID); }
    public int   getPatternId()    { return this.entityData.get(PATTERN_ID); }
    public int   getHeadId()       { return this.entityData.get(HEAD_ID); }
    public int   getSockId()       { return this.entityData.get(SOCK_ID); }
    public int   getManeId()       { return this.entityData.get(MANE_ID); }
    public float getHeightScale()  { return this.entityData.get(HEIGHT_SCALE); }
    public float getDensityScale() { return this.entityData.get(DENSITY_SCALE); }
    public float getTailScale()    { return this.entityData.get(TAIL_SCALE); }

    public void setBaseId(int v)    { this.entityData.set(BASE_ID,      clamp(v, 0, 21)); }
    public void setPatternId(int v) { this.entityData.set(PATTERN_ID,   clamp(v, 0, 17)); }
    public void setHeadId(int v)    { this.entityData.set(HEAD_ID,      clamp(v, 0, 16)); }
    public void setSockId(int v)    { this.entityData.set(SOCK_ID,      clamp(v, 0, 13)); }
    public void setManeId(int v)    { this.entityData.set(MANE_ID,      clamp(v, 0, 13)); }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    // ── Attributes ───────────────────────────────────────────────────────────
    public static AttributeSupplier.Builder createAttributes() {
        return createBaseMillionHorseAttributes();
    }

    // Atributos: usa los valores de AbstractMillionHorseEntity (equivalentes a vanilla)

    @Override
    protected boolean canFly() { return false; }

    // ── Variant randomization ────────────────────────────────────────────────
    private static final int[] BASE_WEIGHTS =
            { 10,10,10,10,10,10,10, 8,10,10,10,10,10, 7,10,10,10,10,10,10, 8,10 };
    private static final int[] PATTERN_WEIGHTS =
            { 5,20, 8,10,10, 8,10,10,10,10, 6, 8,10, 8, 6, 8,10,10 };
    private static final int[] HEAD_WEIGHTS =
            { 8,40,10,10,10,10,10, 8, 9,10, 8,10,10,10,10,10,10 };
    private static final int[] MANE_WEIGHTS =
            { 8,30,10,10,10,10,10,10,10,10,10,10,10,10 };

    private static int randomWeighted(RandomSource rng, int[] weights) {
        int total = 0;
        for (int w : weights) total += w;
        int roll = rng.nextInt(total);
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0) return i;
        }
        return weights.length - 1;
    }

    @Override
    protected void randomizeVariant() {
        RandomSource rng = this.getRandom();
        setBaseId(randomWeighted(rng, BASE_WEIGHTS));
        setPatternId(randomWeighted(rng, PATTERN_WEIGHTS));
        setHeadId(randomWeighted(rng, HEAD_WEIGHTS));
        setManeId(randomWeighted(rng, MANE_WEIGHTS));
        setSockId(rng.nextInt(14));
        this.entityData.set(HEIGHT_SCALE,  lerp(rng, -0.125f, 0.1875f));
        this.entityData.set(DENSITY_SCALE, lerp(rng, -0.1875f, 0.0875f));
        this.entityData.set(TAIL_SCALE,    lerp(rng, 0.6f, 1.5f));
        // Grupos 0-5 en spawn wild (igual que Bedrock)
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
        tag.putInt("BaseId",       getBaseId());
        tag.putInt("PatternId",    getPatternId());
        tag.putInt("HeadId",       getHeadId());
        tag.putInt("SockId",       getSockId());
        tag.putInt("ManeId",       getManeId());
        tag.putFloat("HeightScale",  getHeightScale());
        tag.putFloat("DensityScale", getDensityScale());
        tag.putFloat("TailScale",    getTailScale());
    }

    @Override
    protected boolean loadVariantData(CompoundTag tag) {
        if (!tag.contains("BaseId")) return false;
        setBaseId(tag.getInt("BaseId"));
        setPatternId(tag.getInt("PatternId"));
        setHeadId(tag.getInt("HeadId"));
        setSockId(tag.getInt("SockId"));
        setManeId(tag.getInt("ManeId"));
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
        return other instanceof CynHorseEntity && this.isTamed() && ((CynHorseEntity) other).isTamed();
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(
            net.minecraft.server.level.ServerLevel level, AgeableMob mate) {
        CynHorseEntity foal = EntityRegistry.CYN_HORSE.get().create(level);
        if (foal == null) return null;

        if (!(mate instanceof CynHorseEntity other)) return foal;

        RandomSource rng = foal.getRandom();

        // Cada capa se hereda de uno de los dos padres (50/50)
        foal.setBaseId(    rng.nextBoolean() ? this.getBaseId()    : other.getBaseId());
        foal.setPatternId( rng.nextBoolean() ? this.getPatternId() : other.getPatternId());
        foal.setHeadId(    rng.nextBoolean() ? this.getHeadId()    : other.getHeadId());
        foal.setSockId(    rng.nextBoolean() ? this.getSockId()    : other.getSockId());
        foal.setManeId(    rng.nextBoolean() ? this.getManeId()    : other.getManeId());

        // Escalas cosméticas: promedio de los padres con leve varianza
        foal.entityData.set(HEIGHT_SCALE,  lerp(rng,
                (this.getHeightScale()  + other.getHeightScale())  / 2f - 0.02f,
                (this.getHeightScale()  + other.getHeightScale())  / 2f + 0.02f));
        foal.entityData.set(DENSITY_SCALE, lerp(rng,
                (this.getDensityScale() + other.getDensityScale()) / 2f - 0.02f,
                (this.getDensityScale() + other.getDensityScale()) / 2f + 0.02f));
        foal.entityData.set(TAIL_SCALE,    lerp(rng,
                (this.getTailScale()    + other.getTailScale())    / 2f - 0.05f,
                (this.getTailScale()    + other.getTailScale())    / 2f + 0.05f));

        // Atributos heredados de padres
        foal.randomizeAttributesFromParents(rng, this, other);

        return foal;
    }

    // ── Death drops ──────────────────────────────────────────────────────────
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        int leather = this.getRandom().nextInt(3) + looting;
        if (leather > 0) this.spawnAtLocation(new ItemStack(Items.LEATHER, leather));
    }
}