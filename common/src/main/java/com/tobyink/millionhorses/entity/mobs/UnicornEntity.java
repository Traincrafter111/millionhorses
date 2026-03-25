package com.tobyink.millionhorses.entity.mobs;

import com.tobyink.millionhorses.entity.variant.UnicornVariant;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.tags.BiomeTags;
import org.jetbrains.annotations.Nullable;

public class UnicornEntity extends AbstractMillionHorseEntity {

    // ── Synced data ──────────────────────────────────────────────────────────
    private static final EntityDataAccessor<Integer> BASE_ID =
            SynchedEntityData.defineId(UnicornEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEIGHT_SCALE =
            SynchedEntityData.defineId(UnicornEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DENSITY_SCALE =
            SynchedEntityData.defineId(UnicornEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TAIL_SCALE =
            SynchedEntityData.defineId(UnicornEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> HAS_NETHER_STAR =
            SynchedEntityData.defineId(UnicornEntity.class, EntityDataSerializers.BOOLEAN);

    // ── Constructor ──────────────────────────────────────────────────────────
    public UnicornEntity(EntityType<? extends UnicornEntity> type, Level level) {
        super(type, level);
    }

    // ── SynchedEntityData ────────────────────────────────────────────────────
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BASE_ID,         0);
        this.entityData.define(HEIGHT_SCALE,    0.0f);
        this.entityData.define(DENSITY_SCALE,   0.0f);
        this.entityData.define(TAIL_SCALE,      1.0f);
        this.entityData.define(HAS_NETHER_STAR, false);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public UnicornVariant getUnicornVariant() {
        return UnicornVariant.byId(this.entityData.get(BASE_ID));
    }

    public void setUnicornVariant(UnicornVariant variant) {
        this.entityData.set(BASE_ID, variant.getId());
    }

    public float getHeightScale()  { return this.entityData.get(HEIGHT_SCALE); }
    public float getDensityScale() { return this.entityData.get(DENSITY_SCALE); }
    public float getTailScale()    { return this.entityData.get(TAIL_SCALE); }

    // ── Attributes ───────────────────────────────────────────────────────────
    public static AttributeSupplier.Builder createAttributes() {
        return createBaseMillionHorseAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected boolean canFly() { return false; }

    // ── Variant randomization ────────────────────────────────────────────────
    // Weights from Bedrock: 25,25,25,6,6,6,6,1
    private static final int[] BASE_WEIGHTS = { 25, 25, 25, 6, 6, 6, 6, 1 };

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
        setUnicornVariant(UnicornVariant.byId(randomWeighted(rng, BASE_WEIGHTS)));
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
        tag.putInt("UnicornVariant", this.getUnicornVariant().getId());
        tag.putFloat("HeightScale",  getHeightScale());
        tag.putFloat("DensityScale", getDensityScale());
        tag.putFloat("TailScale",    getTailScale());
    }

    @Override
    protected boolean loadVariantData(CompoundTag tag) {
        if (!tag.contains("UnicornVariant")) return false;
        setUnicornVariant(UnicornVariant.byId(tag.getInt("UnicornVariant")));
        if (tag.contains("HeightScale"))
            this.entityData.set(HEIGHT_SCALE,  tag.getFloat("HeightScale"));
        if (tag.contains("DensityScale"))
            this.entityData.set(DENSITY_SCALE, tag.getFloat("DensityScale"));
        if (tag.contains("TailScale"))
            this.entityData.set(TAIL_SCALE,    tag.getFloat("TailScale"));
        return true;
    }

    public boolean hasNetherStar() { return this.entityData.get(HAS_NETHER_STAR); }
    public void setHasNetherStar(boolean v) { this.entityData.set(HAS_NETHER_STAR, v); }

    // ── Nether star interaction ───────────────────────────────────────────────
    // Nether star activa modo especial: Unicorn×Unicorn puede dar Pegasus
    // El breeding normal (golden carrot/apple) siempre funciona para Unicorn×Unicorn→Unicorn
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.NETHER_STAR) && this.isTamed()
                && !this.isBaby() && this.getAge() == 0
                && !level().isClientSide) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            this.setHasNetherStar(true);
            this.setInLove(player);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    // ── Breeding ─────────────────────────────────────────────────────────────
    @Override
    public boolean canMate(Animal other) {
        if (!this.isTamed() || !this.isInLove()) return false;
        if (other instanceof UnicornEntity u) return u.isTamed() && u.isInLove();
        if (other instanceof PegasusEntity p) return p.isTamed() && p.isInLove();
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        RandomSource rng = this.getRandom();

        // Unicorn × Pegasus → Alicorn (breeding normal, sin nether star)
        if (mate instanceof PegasusEntity pegasus) {
            AlicornEntity foal = EntityRegistry.ALICORN.get().create(level);
            if (foal == null) return null;
            foal.randomizeAttributesFromParents(rng, this, pegasus);
            foal.variantSetByNbt = true;
            this.setHasNetherStar(false);
            return foal;
        }

        // Unicorn × Unicorn
        if (mate instanceof UnicornEntity other) {
            boolean thisNether  = this.hasNetherStar();
            boolean otherNether = other.hasNetherStar();

            // Limpiar flags en ambos padres
            this.setHasNetherStar(false);
            other.setHasNetherStar(false);

            boolean spawnPegasus;
            if (thisNether && otherNether) {
                // Ambos con nether star → siempre Pegasus
                spawnPegasus = true;
            } else if (thisNether || otherNether) {
                // Solo uno con nether star → 50/50
                spawnPegasus = rng.nextBoolean();
            } else {
                // Ninguno con nether star → siempre Unicorn
                spawnPegasus = false;
            }

            if (spawnPegasus) {
                PegasusEntity foal = EntityRegistry.PEGASUS.get().create(level);
                if (foal == null) return null;
                foal.randomizeAttributesFromParents(rng, this, other);
                foal.variantSetByNbt = true;
                return foal;
            } else {
                UnicornEntity foal = EntityRegistry.UNICORN.get().create(level);
                if (foal == null) return null;
                foal.setUnicornVariant(
                        rng.nextBoolean() ? this.getUnicornVariant() : other.getUnicornVariant());
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
        }

        return null;
    }

    // ── Death drops ──────────────────────────────────────────────────────────
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        RandomSource rng = this.getRandom();

        // Leather: 0-2 + looting
        int leather = rng.nextInt(3) + looting;
        if (leather > 0) this.spawnAtLocation(new ItemStack(Items.LEATHER, leather));

        // Quartz: 50% chance, 0-2 + looting
        if (rng.nextInt(2) == 0) {
            int quartz = rng.nextInt(3) + looting;
            if (quartz > 0) this.spawnAtLocation(new ItemStack(Items.QUARTZ, quartz));
        }

        // Ghast tear: 20% chance, 0-1 + looting
        if (rng.nextFloat() < 0.20f) {
            int tears = rng.nextInt(2) + looting;
            if (tears > 0) this.spawnAtLocation(new ItemStack(Items.GHAST_TEAR, tears));
        }
    }

    // ── Spawn rules ───────────────────────────────────────────────────────────
    // Condition 1: Y >= 196, surface, light >= 7 (any biome)
    // Spawn: cherry_grove/flower_forest a cualquier altitud, otros biomas solo Y>=196
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level,
                                   MobSpawnType reason) {
        BlockPos pos = this.blockPosition();
        if (level.getRawBrightness(pos, 0) < 7) return false;
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome =
                level.getBiome(pos);
        if (biome.is(Biomes.FLOWER_FOREST) || biome.is(Biomes.CHERRY_GROVE)) return true;
        return pos.getY() >= 196;
    }

    public static boolean checkUnicornSpawnRules(
            EntityType<UnicornEntity> type,
            ServerLevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random) {
        if (level.getRawBrightness(pos, 0) < 7) return false;
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome =
                level.getBiome(pos);
        if (biome.is(Biomes.FLOWER_FOREST) || biome.is(Biomes.CHERRY_GROVE)) return true;
        return pos.getY() >= 196;
    }
}