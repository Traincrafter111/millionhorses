package com.tobyink.millionhorses.entity.mobs;

import com.tobyink.millionhorses.entity.constant.HorseAnimations;
import com.tobyink.millionhorses.entity.variant.PegasusVariant;
import mod.azure.azurelib.util.MoveAnalysis;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class PegasusEntity extends AbstractChestedHorse {

    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F295");

    // --- Synced Data ---
    private static final EntityDataAccessor<Integer> PEGASUS_VARIANT =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PEGASUS_FLYING =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean variantSetByNbt = false;
    private boolean babyBornPlayed = false;
    private boolean wasBaby = false;

    // --- Animation State ---
    public HorseAnimations dispatcher;
    public final MoveAnalysis moveAnalysis;
    public PegasusIdleController idleController;

    private enum BaseAnim { IDLE, WALK, RUN, FLY, BABY }
    private BaseAnim baseAnim = null;
    private boolean isRearing = false;

    // --- Flight State ---
    private boolean riderJumpTriggered = false;
    private int jumpBoostTicks = 0;
    private static final int    JUMP_BOOST_DURATION = 20;
    private static final double FLY_ASCEND_SPEED  = 0.15;
    private static final double FLY_GLIDE_DESCENT = -0.02;
    private static final double FLY_FORWARD_SPEED = 0.3;

    // --- Constructor ---
    public PegasusEntity(EntityType<? extends AbstractChestedHorse> entityType, Level level) {
        super(entityType, level);
        this.moveAnalysis   = new MoveAnalysis(this);
        this.dispatcher     = new HorseAnimations(this);
        this.idleController = new PegasusIdleController(this, this.dispatcher);
    }

    // --- Synced Data ---
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PEGASUS_VARIANT, 0);
        this.entityData.define(PEGASUS_FLYING, false);
    }

    // --- Variant ---
    public PegasusVariant getPegasusVariant() {
        return PegasusVariant.byId(this.entityData.get(PEGASUS_VARIANT));
    }
    public void setPegasusVariant(PegasusVariant variant) {
        this.entityData.set(PEGASUS_VARIANT, variant.getId());
    }

    // --- Flying ---
    public boolean isPegasusFlying() { return this.entityData.get(PEGASUS_FLYING); }
    public void setPegasusFlying(boolean v) { this.entityData.set(PEGASUS_FLYING, v); }

    // --- Chest ---
    @Override
    public int getInventoryColumns() { return 5; }

    @Override
    public void createInventory() {
        SimpleContainer oldInventory = this.inventory;
        boolean chest = this.entityData != null && this.hasChest();
        int size = 2 + (chest ? 15 : 0);
        this.inventory = new SimpleContainer(size) {
            @Override
            public void setChanged() {
                super.setChanged();
                PegasusEntity.this.containerChanged(PegasusEntity.this.inventory);
            }
        };
        if (oldInventory != null) {
            int itemsToCopy = Math.min(oldInventory.getContainerSize(), this.inventory.getContainerSize());
            for (int i = 0; i < itemsToCopy; i++) {
                this.inventory.setItem(i, oldInventory.getItem(i));
            }
        }
    }

    // --- NBT ---
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PegasusVariant", this.getPegasusVariant().getId());
        tag.putBoolean("BabyBornPlayed", this.babyBornPlayed);
        if (!this.inventory.getItem(1).isEmpty()) {
            tag.put("ArmorItem", this.inventory.getItem(1).save(new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("PegasusVariant")) {
            this.setPegasusVariant(PegasusVariant.byId(tag.getInt("PegasusVariant")));
            if (tag.contains("ArmorItem", 10)) {
                ItemStack armorStack = ItemStack.of(tag.getCompound("ArmorItem"));
                if (!armorStack.isEmpty() && this.isArmor(armorStack)) {
                    this.inventory.setItem(1, armorStack);
                }
            }
            this.variantSetByNbt = true;
        } else if (!this.variantSetByNbt) {
            this.setPegasusVariant(PegasusVariant.byId(this.random.nextInt(PegasusVariant.values().length)));
            this.variantSetByNbt = true;
        }
        this.babyBornPlayed = tag.getBoolean("BabyBornPlayed");
        this.updateContainerEquipment();
    }

    // --- Spawn ---
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag tag) {
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
    }

    // --- Breeding ---
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.APPLE) || stack.is(Items.SUGAR)
                || stack.is(Items.HAY_BLOCK) || stack.is(Items.GOLDEN_CARROT)
                || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    @Override
    public boolean canMate(Animal other) {
        if (!(other instanceof PegasusEntity partner)) return false;
        return this.isTamed() && partner.isTamed()
                && this.isInLove() && partner.isInLove();
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal other) {
        if (!(other instanceof PegasusEntity partner)) return;
        AgeableMob foal = this.getBreedOffspring(level, partner);
        if (foal != null) {
            foal.setBaby(true);
            foal.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
            this.finalizeSpawnChildFromBreeding(level, other, foal);
            level.addFreshEntityWithPassengers(foal);
        }
    }

    public boolean isBreedingItem(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE)
                || stack.is(Items.ENCHANTED_GOLDEN_APPLE)
                || stack.is(Items.GOLDEN_CARROT);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        if (!(mate instanceof PegasusEntity other)) return null;

        @SuppressWarnings("unchecked")
        EntityType<? extends AbstractChestedHorse> type =
                (EntityType<? extends AbstractChestedHorse>) this.getType();
        PegasusEntity foal = new PegasusEntity(type, level);

        // Heredar textura base de un padre y ojos del otro (independientemente)
        boolean thisHasBlue  = this.getPegasusVariant().name().endsWith("_BLUE");
        boolean otherHasBlue = other.getPegasusVariant().name().endsWith("_BLUE");

        // Base: elegir textura de uno de los padres al azar (sin importar ojos)
        PegasusVariant parentBase = this.random.nextBoolean()
                ? this.getPegasusVariant()
                : other.getPegasusVariant();

        // Ojos: elegir de uno de los padres al azar (independiente de la base)
        boolean foalHasBlue = this.random.nextBoolean() ? thisHasBlue : otherHasBlue;

        // Obtener nombre base sin _BLUE
        String baseName = parentBase.name().replace("_BLUE", "");

        // Combinar base + ojos
        String foalVariantName = foalHasBlue ? baseName + "_BLUE" : baseName;
        PegasusVariant foalVariant;
        try {
            foalVariant = PegasusVariant.valueOf(foalVariantName);
        } catch (IllegalArgumentException e) {
            foalVariant = parentBase; // fallback
        }

        foal.setPegasusVariant(foalVariant);
        foal.variantSetByNbt = true;

        return foal;
    }

    // --- Armor ---
    @Override
    public boolean canWearArmor() { return true; }

    @Override
    public boolean isArmor(ItemStack stack) { return stack.getItem() instanceof HorseArmorItem; }

    public ItemStack getArmor() { return this.getItemBySlot(EquipmentSlot.CHEST); }

    private void setArmor(ItemStack stack) {
        this.setItemSlot(EquipmentSlot.CHEST, stack);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
    }

    @Override
    protected void updateContainerEquipment() {
        if (!this.level().isClientSide) {
            super.updateContainerEquipment();
            this.setArmorEquipment(this.inventory.getItem(1));
            this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        }
    }

    private void setArmorEquipment(ItemStack stack) {
        this.setArmor(stack);
        if (!this.level().isClientSide) {
            this.getAttribute(Attributes.ARMOR).removeModifier(ARMOR_MODIFIER_UUID);
            if (this.isArmor(stack)) {
                int protection = ((HorseArmorItem) stack.getItem()).getProtection();
                if (protection != 0) {
                    this.getAttribute(Attributes.ARMOR).addTransientModifier(
                            new AttributeModifier(ARMOR_MODIFIER_UUID, "Horse armor bonus",
                                    protection, AttributeModifier.Operation.ADDITION));
                }
            }
        }
    }

    /** Getter para el layer de armadura — lee del EquipmentSlot sincronizado con el cliente. */
    public ItemStack getArmorItem() {
        return this.getItemBySlot(EquipmentSlot.CHEST);
    }

    // --- Attributes ---
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractHorse.createBaseHorseAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.JUMP_STRENGTH, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    protected void randomizeAttributes(RandomSource random) {
        Objects.requireNonNull(random);
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(generateMaxHealth(random::nextInt));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(generateSpeed(random::nextDouble));
        this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateJumpStrength(random::nextDouble));
    }

    // --- Goals ---
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new RunAroundLikeCrazyGoal(this, 1.2));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.2));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    private int getFoodTemper(ItemStack stack) {
        if (stack.is(Items.SUGAR))                  return 3;
        if (stack.is(Items.WHEAT))                  return 3;
        if (stack.is(Items.APPLE))                  return 3;
        if (stack.is(Items.GOLDEN_CARROT))          return 5;
        if (stack.is(Items.HAY_BLOCK))              return 15;
        if (stack.is(Items.GOLDEN_APPLE))           return 10;
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return 10;
        return 0;
    }

    // --- Death Drops ---
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        int leather = this.random.nextInt(3) + looting;
        if (leather > 0) this.spawnAtLocation(new ItemStack(Items.LEATHER, leather));
        int feathers = 1 + this.random.nextInt(3) + looting;
        this.spawnAtLocation(new ItemStack(Items.FEATHER, feathers));
        if (this.random.nextBoolean())
            this.spawnAtLocation(new ItemStack(Items.QUARTZ, 1 + looting));
        if (this.random.nextFloat() < 0.1f + (looting * 0.05f))
            this.spawnAtLocation(new ItemStack(Items.GHAST_TEAR, 1));
    }

    // --- Rear Animation ---
    @Override
    public void setStanding(boolean standing) {
        super.setStanding(standing);
        if (standing) {
            isRearing = true;
            this.playSound(SoundEvents.HORSE_ANGRY, 1.0F, 1.0F);
            dispatcher.buck();
        } else {
            isRearing = false;
            dispatcher.idle();
        }
    }

    // --- Tick ---
    @Override
    public void tick() {
        super.tick();
        moveAnalysis.update();

        if (this.isVehicle() && this.isTamed()) {
            Entity rider = this.getPassengers().get(0);
            if (rider instanceof Player player) {
                handleFlight(player);
            }
        } else {
            if (this.isPegasusFlying() && this.onGround()) {
                this.setPegasusFlying(false);
                this.setNoGravity(false);
                jumpBoostTicks = 0;
            }
        }

        if (level().isClientSide) return;
        if (isRearing) return;

        // Animación de bebé
        if (this.isBaby()) {
            wasBaby = true;
            if (!babyBornPlayed) {
                dispatcher.babyBorn();
                babyBornPlayed = true;
                baseAnim = BaseAnim.BABY;
                return;
            }
            if (baseAnim != BaseAnim.BABY) {
                dispatcher.baby();
                baseAnim = BaseAnim.BABY;
            }
            return;
        }

        // Transición bebé → adulto
        if (wasBaby) {
            wasBaby = false;
            baseAnim = null; // forzar re-evaluación de animación
            dispatcher.idle();
        }

        BaseAnim next;
        if (isPegasusFlying()) {
            next = BaseAnim.FLY;
        } else if (this.isVehicle()) {
            Entity rider = this.getPassengers().get(0);
            if (!this.isTamed()) {
                next = BaseAnim.WALK;
            } else {
                double riderSpeed = rider.getDeltaMovement().horizontalDistanceSqr();
                boolean moving = riderSpeed > 1.0E-6
                        || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
                next = moving ? BaseAnim.RUN : BaseAnim.IDLE;
            }
        } else {
            double speedSq = this.getDeltaMovement().horizontalDistanceSqr();
            boolean moving = speedSq > 1.0E-4;
            if (!moving) {
                next = BaseAnim.IDLE;
            } else if (speedSq > 0.05 || isAggressive() || getTarget() != null) {
                next = BaseAnim.RUN;
            } else {
                next = BaseAnim.WALK;
            }
        }

        if (next != baseAnim) {
            if (next != BaseAnim.IDLE) idleController.onStartMoving();
            baseAnim = next;
            switch (baseAnim) {
                case FLY  -> dispatcher.fly();
                case RUN  -> dispatcher.run();
                case WALK -> dispatcher.walk();
                case IDLE -> dispatcher.idle();
            }
        }

        if (baseAnim == BaseAnim.IDLE && !this.isVehicle()) {
            idleController.tick();
        }
    }

    // --- Sistema de Vuelo ---
    @Override
    public void handleStartJump(int jumpPower) {
        if (!isPegasusFlying()) {
            dispatcher.jump();
            setPegasusFlying(true);
            setNoGravity(true);
            setDeltaMovement(getDeltaMovement().x, FLY_ASCEND_SPEED * 2, getDeltaMovement().z);
        } else {
            setDeltaMovement(getDeltaMovement().x, FLY_ASCEND_SPEED, getDeltaMovement().z);
        }
        riderJumpTriggered = true;
    }

    @Override
    public void handleStopJump() { riderJumpTriggered = false; }

    private void handleFlight(Player player) {
        if (!isPegasusFlying()) return;
        setNoGravity(true);
        Vec3 movement = getDeltaMovement();

        double newY;
        if (riderJumpTriggered) {
            newY = movement.y;
            riderJumpTriggered = false;
        } else {
            newY = Math.max(movement.y - 0.01, FLY_GLIDE_DESCENT);
        }

        float yRot = player.getYRot();
        double dx = -Math.sin(Math.toRadians(yRot)) * FLY_FORWARD_SPEED * player.zza;
        double dz =  Math.cos(Math.toRadians(yRot)) * FLY_FORWARD_SPEED * player.zza;

        setDeltaMovement(dx, newY, dz);
        setYRot(player.getYRot());
        this.yRotO = getYRot();

        if (onGround() && newY <= 0) {
            setPegasusFlying(false);
            setNoGravity(false);
        }
    }

    @Override
    public boolean isSaddleable() {
        return !this.isBaby() && super.isSaddleable();
    }

    @Override
    public void aiStep() { super.aiStep(); }

    // --- Interacción ---
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide) return InteractionResult.CONSUME;

        if (!this.isTamed()) {
            if (this.isFood(stack) && this.getTemper() < this.getMaxTemper()) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.modifyTemper(getFoodTemper(stack));
                this.heal(2.0F);
                this.playSound(SoundEvents.HORSE_EAT, 1.0F, 1.0F);
                this.level().broadcastEntityEvent(this, (byte) 6);
                return InteractionResult.SUCCESS;
            }
            if (!this.isBaby()) player.startRiding(this);
            return InteractionResult.SUCCESS;
        }

        boolean isOwner = this.getOwnerUUID() == null
                || player.getUUID().equals(this.getOwnerUUID());

        if (isOwner) {
            // Activar modo crianza
            if (this.isBreedingItem(stack) && this.isTamed() && !this.isBaby()) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.setInLove(player);
                this.playSound(SoundEvents.HORSE_EAT, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            if (this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.heal(4.0F);
                this.playSound(SoundEvents.HORSE_EAT, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            if (stack.is(Blocks.CHEST.asItem()) && !this.hasChest()) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.setChest(true);
                this.createInventory();
                return InteractionResult.SUCCESS;
            }
            if (stack.isEmpty() && this.hasChest() && player.isShiftKeyDown()) {
                this.setChest(false);
                this.spawnAtLocation(Blocks.CHEST.asItem());
                this.createInventory();
                return InteractionResult.SUCCESS;
            }
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (!this.level().isClientSide
                && (!this.isVehicle() || this.hasPassenger(player))
                && this.isTamed()) {
            player.openHorseInventory(this, this.inventory);
        }
    }

    // --- Sonidos ---
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.HORSE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.HORSE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.HORSE_DEATH; }
    @Override protected SoundEvent getEatingSound() { return SoundEvents.HORSE_EAT; }
    @Override protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.HORSE_STEP_WOOD, 1.0F, 1.0F);
    }
    @Override public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.8f * this.getEyeHeight(), this.getBbWidth() * 0.4f);
    }
}