package com.tobyink.millionhorses.entity.mobs;

import com.tobyink.millionhorses.entity.constant.HorseAnimations;
import com.tobyink.millionhorses.entity.constant.MovementMode;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all Million Horses mod equines.
 * Contains all shared logic: flight, taming animation, movement modes,
 * attribute groups, inventory/chest/carpet/armor, GUI, goals, animations.
 *
 * Subclasses must implement:
 *  - randomizeVariant()      — assign a random species-specific variant on spawn
 *  - saveVariantData()       — write variant NBT key(s)
 *  - loadVariantData()       — read variant NBT key(s)
 *  - createAttributes()      — static AttributeSupplier.Builder (registered per entity type)
 */
public abstract class AbstractMillionHorseEntity extends AbstractChestedHorse {

    private static final UUID ARMOR_MODIFIER_UUID =
            UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F295");

    // -------------------------------------------------------------------------
    // Synced data
    // -------------------------------------------------------------------------
    private static final EntityDataAccessor<Boolean> HORSE_FLYING =
            SynchedEntityData.defineId(AbstractMillionHorseEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> CARPET_ITEM_DATA =
            SynchedEntityData.defineId(AbstractMillionHorseEntity.class, EntityDataSerializers.ITEM_STACK);

    // -------------------------------------------------------------------------
    // Attribute groups (0–9, mirroring Bedrock)
    // -------------------------------------------------------------------------
    protected int healthGroupId = 3;
    protected int speedGroupId  = 3;
    protected int jumpGroupId   = 3;

    // -------------------------------------------------------------------------
    // Animation state
    // -------------------------------------------------------------------------
    public HorseAnimations dispatcher;
    public final MoveAnalysis moveAnalysis;
    public HorseIdleController idleController;

    private enum BaseAnim { IDLE, WALK, RUN, FLY }
    private BaseAnim baseAnim = null;
    private boolean isRearing = false;

    // -------------------------------------------------------------------------
    // Taming animation state
    // -------------------------------------------------------------------------
    private enum TamingState { NONE, BUCKING, REARING }
    private TamingState tamingState = TamingState.NONE;
    private int         tamingTimer = 0;

    private int getBuckDuration() {
        float ratio = (float) this.getTemper() / this.getMaxTemper();
        return (int)(140 - ratio * 80) + this.random.nextInt(40);
    }
    private static final int REAR_DURATION_TAMING = 60;

    // -------------------------------------------------------------------------
    // Flight state
    // -------------------------------------------------------------------------
    private int     jumpCount        = 0;
    private boolean wasOnGround      = true;
    private boolean jumpKeyHeld      = false;
    private double  prevY            = 0.0;

    public static double FLY_ASCEND_SPEED   = 0.12;
    public static double FLY_DESCEND_SPEED  = -0.06;
    public static double FLY_FORWARD_SPEED  = 0.55;
    public static double GROUND_SPEED_BOOST = 1.6;
    public static int    CLIFF_MIN_BLOCKS   = 5;

    // -------------------------------------------------------------------------
    // Movement mode (whistle)
    // -------------------------------------------------------------------------
    private MovementMode movementMode = MovementMode.WANDERING;

    private FollowOwnerGoal followGoal;
    private SitGoal         sitGoal;

    // -------------------------------------------------------------------------
    // Misc
    // -------------------------------------------------------------------------
    protected boolean variantSetByNbt = false;
    private boolean babyBornPlayed   = false;
    private boolean wasBaby          = false;

    // =========================================================================
    // Constructor
    // =========================================================================
    protected AbstractMillionHorseEntity(EntityType<? extends AbstractChestedHorse> type, Level level) {
        super(type, level);
        this.moveAnalysis   = new MoveAnalysis(this);
        this.dispatcher     = new HorseAnimations(this);
        this.idleController = new HorseIdleController(this, this.dispatcher);
    }

    // =========================================================================
    // Abstract hooks — subclasses define variant behaviour
    // =========================================================================

    /** Assign a random variant to this entity on first spawn. */
    protected abstract void randomizeVariant();

    /** Write variant-specific NBT keys into the tag. */
    protected abstract void saveVariantData(CompoundTag tag);

    /**
     * Read variant-specific NBT keys from the tag.
     * @return true if a variant key was found and applied (sets variantSetByNbt).
     */
    protected abstract boolean loadVariantData(CompoundTag tag);

    // =========================================================================
    // Synced data
    // =========================================================================

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HORSE_FLYING, false);
        this.entityData.define(CARPET_ITEM_DATA, ItemStack.EMPTY);
    }

    // =========================================================================
    // Flying
    // =========================================================================

    public boolean isHorseFlying() { return this.entityData.get(HORSE_FLYING); }
    public void setHorseFlying(boolean v) { this.entityData.set(HORSE_FLYING, v); }

    // =========================================================================
    // Movement mode
    // =========================================================================

    public MovementMode getMovementMode() { return this.movementMode; }
    public void setMovementMode(MovementMode mode) {
        this.movementMode = mode;
        if (!level().isClientSide) {
            baseAnim = null;
            dispatcher.idle();
        }
    }
    public boolean isSitting() { return this.movementMode == MovementMode.SITTING; }

    // =========================================================================
    // Attribute groups
    // =========================================================================

    public int getHealthGroupId() { return healthGroupId; }
    public int getSpeedGroupId()  { return speedGroupId;  }
    public int getJumpGroupId()   { return jumpGroupId;   }

    public void setAttributeGroups(int hg, int sg, int jg) {
        this.healthGroupId = hg;
        this.speedGroupId  = sg;
        this.jumpGroupId   = jg;
    }

    protected static double randomInRange(RandomSource r, double min, double max) {
        return min + r.nextDouble() * (max - min);
    }

    // Default attribute group formulas — subclasses may override for higher caps
    protected double healthForGroup(RandomSource r, int g) {
        // grupo 0 → 15-18, grupo 9 → 42-45 (vanilla horse: 15-30)
        return randomInRange(r, 15.0 + g * 3.0, 18.0 + g * 3.0);
    }
    protected double speedForGroup(RandomSource r, int g) {
        // grupo 0 → 0.112-0.138, grupo 9 → 0.202-0.228 (vanilla: ~0.113-0.338 * 0.25 factor)
        return randomInRange(r, 0.112 + g * 0.01, 0.138 + g * 0.01);
    }
    protected double jumpForGroup(RandomSource r, int g) {
        // grupo 0 → 0.40-0.46, grupo 9 → 0.76-0.82
        return randomInRange(r, 0.40 + g * 0.04, 0.46 + g * 0.04);
    }

    public static AttributeSupplier.Builder createBaseMillionHorseAttributes() {
        return AbstractHorse.createBaseHorseAttributes()
                .add(Attributes.MAX_HEALTH,     99.0)
                .add(Attributes.MOVEMENT_SPEED, 10.0)
                .add(Attributes.JUMP_STRENGTH,   5.0)
                .add(Attributes.ATTACK_DAMAGE,   6.0);
    }

    @Override
    protected void randomizeAttributes(RandomSource random) {
        Objects.requireNonNull(random);
        this.healthGroupId = random.nextInt(6);
        this.speedGroupId  = random.nextInt(6);
        this.jumpGroupId   = random.nextInt(6);
        applyAttributeGroups(random, healthGroupId, speedGroupId, jumpGroupId);
    }

    public void randomizeAttributesFromParents(RandomSource random,
                                               AbstractMillionHorseEntity p1,
                                               AbstractMillionHorseEntity p2) {
        // Promedio de padres con leve varianza (±1) para evitar escalada infinita
        this.healthGroupId = clamp9(Math.round((p1.healthGroupId + p2.healthGroupId) / 2.0f) + random.nextInt(3) - 1);
        this.speedGroupId  = clamp9(Math.round((p1.speedGroupId  + p2.speedGroupId)  / 2.0f) + random.nextInt(3) - 1);
        this.jumpGroupId   = clamp9(Math.round((p1.jumpGroupId   + p2.jumpGroupId)   / 2.0f) + random.nextInt(3) - 1);
        applyAttributeGroups(random, healthGroupId, speedGroupId, jumpGroupId);
    }

    protected void applyAttributeGroups(RandomSource r, int hg, int sg, int jg) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(healthForGroup(r, hg));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speedForGroup(r, sg));
        this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(jumpForGroup(r, jg));
    }

    protected static int clamp9(long v) {
        return (int) Math.min(9, Math.max(0, v));
    }

    // =========================================================================
    // Inventory / chest
    // =========================================================================

    @Override
    public int getInventoryColumns() { return 5; }

    /** Subclasses override to change chest inventory size. Default: 15 (vanilla). */
    public int getChestSize() { return 15; }

    @Override
    public void createInventory() {
        SimpleContainer old = this.inventory;
        boolean chest = this.entityData != null && this.hasChest();
        int size = 3 + (chest ? getChestSize() : 0);
        this.inventory = new SimpleContainer(size) {
            @Override public void setChanged() {
                super.setChanged();
                AbstractMillionHorseEntity.this.containerChanged(
                        AbstractMillionHorseEntity.this.inventory);
            }
            @Override public void stopOpen(Player player) { /* keep inventory on close */ }
        };
        if (old != null) {
            int limit = Math.min(old.getContainerSize(), this.inventory.getContainerSize());
            for (int i = 0; i < limit; i++) this.inventory.setItem(i, old.getItem(i));
        }
    }

    // =========================================================================
    // Armor
    // =========================================================================

    @Override public boolean canWearArmor() { return true; }
    @Override public boolean isArmor(ItemStack stack) { return stack.getItem() instanceof HorseArmorItem; }
    public ItemStack getArmorItem() { return this.getItemBySlot(EquipmentSlot.CHEST); }
    public SimpleContainer getHorseInventory() { return this.inventory; }

    private void setArmor(ItemStack stack) {
        this.setItemSlot(EquipmentSlot.CHEST, stack);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
    }

    @Override
    protected void updateContainerEquipment() {
        if (!this.level().isClientSide) {
            super.updateContainerEquipment();
            setArmorEquipment(this.inventory.getItem(1));
            this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        }
    }

    private void setArmorEquipment(ItemStack stack) {
        setArmor(stack);
        if (!this.level().isClientSide) {
            this.getAttribute(Attributes.ARMOR).removeModifier(ARMOR_MODIFIER_UUID);
            if (this.isArmor(stack)) {
                int prot = ((HorseArmorItem) stack.getItem()).getProtection();
                if (prot != 0) {
                    this.getAttribute(Attributes.ARMOR).addTransientModifier(
                            new AttributeModifier(ARMOR_MODIFIER_UUID, "Horse armor bonus",
                                    prot, AttributeModifier.Operation.ADDITION));
                }
            }
        }
    }

    // =========================================================================
    // Carpet (slot 2)
    // =========================================================================

    @Override
    public void containerChanged(net.minecraft.world.Container container) {
        super.containerChanged(container);
        if (!level().isClientSide && this.inventory != null) {
            ItemStack inSlot   = this.inventory.getItem(2);
            ItemStack synced   = this.entityData.get(CARPET_ITEM_DATA);
            if (!ItemStack.matches(inSlot, synced)) {
                this.entityData.set(CARPET_ITEM_DATA,
                        inSlot.isEmpty() ? ItemStack.EMPTY : inSlot.copy());
            }
        }
    }

    public ItemStack getCarpetItem() { return this.entityData.get(CARPET_ITEM_DATA); }
    public void setCarpetItem(ItemStack stack) {
        if (this.inventory != null) this.inventory.setItem(2, stack);
        this.entityData.set(CARPET_ITEM_DATA, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
    }

    // =========================================================================
    // NBT
    // =========================================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveVariantData(tag);
        tag.putBoolean("BabyBornPlayed", this.babyBornPlayed);
        tag.putInt("HealthGroupId", this.healthGroupId);
        tag.putInt("SpeedGroupId",  this.speedGroupId);
        tag.putInt("JumpGroupId",   this.jumpGroupId);
        tag.putString("MovementMode", this.movementMode.name());
        if (!this.inventory.getItem(1).isEmpty())
            tag.put("ArmorItem",  this.inventory.getItem(1).save(new CompoundTag()));
        if (!this.inventory.getItem(2).isEmpty())
            tag.put("CarpetItem", this.inventory.getItem(2).save(new CompoundTag()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (loadVariantData(tag)) {
            variantSetByNbt = true;
            if (tag.contains("ArmorItem", 10)) {
                ItemStack s = ItemStack.of(tag.getCompound("ArmorItem"));
                if (!s.isEmpty() && this.isArmor(s)) this.inventory.setItem(1, s);
            }
            if (tag.contains("CarpetItem", 10)) {
                ItemStack s = ItemStack.of(tag.getCompound("CarpetItem"));
                if (!s.isEmpty()) {
                    this.inventory.setItem(2, s);
                    this.entityData.set(CARPET_ITEM_DATA, s.copy());
                }
            }
        } else if (!variantSetByNbt) {
            randomizeVariant();
            variantSetByNbt = true;
        }
        this.babyBornPlayed = tag.getBoolean("BabyBornPlayed");
        if (tag.contains("HealthGroupId")) {
            this.healthGroupId = tag.getInt("HealthGroupId");
            this.speedGroupId  = tag.getInt("SpeedGroupId");
            this.jumpGroupId   = tag.getInt("JumpGroupId");
        }
        if (tag.contains("MovementMode")) {
            try { this.movementMode = MovementMode.valueOf(tag.getString("MovementMode")); }
            catch (IllegalArgumentException e) { this.movementMode = MovementMode.WANDERING; }
        }
        this.updateContainerEquipment();
    }

    // =========================================================================
    // Spawn
    // =========================================================================

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
        if (!variantSetByNbt) {
            randomizeVariant();
            variantSetByNbt = true;
        }
        if (tag == null || tag.isEmpty()) {
            this.randomizeAttributes(this.random);
        }
        return data;
    }

    // =========================================================================
    // Food / breeding basics
    // =========================================================================

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.APPLE) || stack.is(Items.SUGAR)
                || stack.is(Items.HAY_BLOCK) || stack.is(Items.GOLDEN_CARROT)
                || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    public boolean isBreedingItem(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE)
                || stack.is(Items.ENCHANTED_GOLDEN_APPLE)
                || stack.is(Items.GOLDEN_CARROT);
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal other) {
        AgeableMob foal = this.getBreedOffspring(level, other);
        if (foal != null) {
            foal.setBaby(true);
            foal.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
            // La cría no despawnea naturalmente
            foal.setPersistenceRequired();
            this.finalizeSpawnChildFromBreeding(level, other, foal);
            level.addFreshEntityWithPassengers(foal);
        }
    }


    // =========================================================================
    // Goals
    // =========================================================================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new RunAroundLikeCrazyGoal(this, 1.2));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.2));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0) {
            // Override para buscar cualquier AbstractMillionHorseEntity, no solo la misma clase
            @Override
            protected void breed() {
                if (animal instanceof AbstractMillionHorseEntity horse
                        && partner instanceof AbstractMillionHorseEntity horseMate) {
                    horse.spawnChildFromBreeding((net.minecraft.server.level.ServerLevel) animal.level(), horseMate);
                    horse.setAge(6000);
                    horseMate.setAge(6000);
                    horse.resetLove();
                    horseMate.resetLove();
                } else {
                    super.breed();
                }
            }

            @Override
            public boolean canUse() {
                if (!animal.isInLove()) return false;
                // Buscar pareja entre todos los AbstractMillionHorseEntity cercanos
                partner = animal.level().getEntitiesOfClass(
                                AbstractMillionHorseEntity.class,
                                animal.getBoundingBox().inflate(8.0))
                        .stream()
                        .filter(e -> e != animal && e.isInLove() && animal.canMate(e))
                        .findFirst()
                        .orElse(null);
                return partner != null;
            }
        });

        this.followGoal = new FollowOwnerGoal(this, 1.1, 4.0F, 16.0F);
        this.sitGoal    = new SitGoal(this);
        this.goalSelector.addGoal(4, this.followGoal);
        this.goalSelector.addGoal(4, this.sitGoal);

        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    // =========================================================================
    // Rear animation
    // =========================================================================

    @Override
    public void setStanding(boolean standing) {
        super.setStanding(standing);
        if (standing) {
            isRearing = true;
            this.playSound(SoundEvents.HORSE_ANGRY, 1.0F, 1.0F);
            if (!this.isTamed() && this.isVehicle()) dispatcher.buck();
            else dispatcher.rear();
        } else {
            isRearing = false;
            dispatcher.idle();
        }
    }

    // =========================================================================
    // Tick
    // =========================================================================

    @Override
    public void tick() {
        super.tick();
        moveAnalysis.update();

        boolean onGround = this.onGround();

        if (this.isVehicle() && !this.isTamed()) {
            if (!level().isClientSide && tamingState == TamingState.NONE) {
                tamingState = TamingState.BUCKING;
                tamingTimer = getBuckDuration();
                dispatcher.buck();
                this.playSound(SoundEvents.HORSE_ANGRY, 1.0F, 1.0F);
            }
        } else if (this.isVehicle() && this.isTamed()) {
            Entity rider = this.getPassengers().get(0);
            if (rider instanceof Player player) {
                jumpKeyHeld = ((com.tobyink.millionhorses.mixin.LivingEntityMixin)(Object) player).isJumping();
                handleFlight(player);
            }
        } else {
            if (this.isHorseFlying()) {
                setHorseFlying(false);
                setNoGravity(false);
                jumpKeyHeld = false;
                jumpCount   = 0;
            }
            if (onGround && this.isNoGravity()) setNoGravity(false);
        }

        if (onGround && !wasOnGround) jumpCount = 0;
        wasOnGround = onGround;

        if (level().isClientSide) return;
        if (isRearing && tamingState == TamingState.NONE) return;

        // Taming animation cycle
        if (tamingState != TamingState.NONE) {
            // Si ya fue domado durante cualquier fase, cancelar limpiamente
            if (this.isTamed()) {
                tamingState = TamingState.NONE;
                dispatcher.idle();
            } else {
                tamingTimer--;
                if (tamingState == TamingState.BUCKING) {
                    dispatcher.buck();
                    if (tamingTimer <= 0) {
                        // Solo rear+eject si sigue sin domar
                        if (!this.isTamed()) {
                            this.ejectPassengers();
                            tamingState = TamingState.REARING;
                            tamingTimer = REAR_DURATION_TAMING;
                            dispatcher.rear();
                            this.playSound(SoundEvents.HORSE_ANGRY, 1.0F, 1.0F);
                        } else {
                            tamingState = TamingState.NONE;
                            dispatcher.idle();
                        }
                    }
                } else if (tamingState == TamingState.REARING && tamingTimer <= 0) {
                    tamingState = TamingState.NONE;
                    dispatcher.idle();
                }
            }
        }

        // Cliff detection → activate flight
        if (this.isVehicle() && this.isTamed() && !isHorseFlying() && canFly()) {
            double deltaY = this.getY() - prevY;
            if (!onGround && deltaY < -0.1) {
                boolean isCliff = true;
                for (int i = 1; i <= CLIFF_MIN_BLOCKS; i++) {
                    if (!level().isEmptyBlock(blockPosition().below(i))) { isCliff = false; break; }
                }
                if (isCliff) activateFlight();
            }
        }
        prevY = this.getY();

        // Baby animations
        if (this.isBaby()) {
            wasBaby = true;
            if (!babyBornPlayed) { dispatcher.babyBorn(); babyBornPlayed = true; }
            dispatcher.babyPose();
        }
        if (wasBaby && !this.isBaby()) {
            wasBaby = false;
            baseAnim = null;
            dispatcher.stopBaby();
            dispatcher.idle();
            this.refreshDimensions();
        }

        if (tamingState != TamingState.NONE) { prevY = this.getY(); return; }

        // Base animation state machine
        BaseAnim next;
        if (isHorseFlying()) {
            next = BaseAnim.FLY;
        } else if (this.isVehicle()) {
            Entity rider = this.getPassengers().get(0);
            if (!this.isTamed()) {
                next = BaseAnim.WALK;
            } else {
                boolean moving = rider.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6
                        || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
                next = moving ? BaseAnim.RUN : BaseAnim.IDLE;
            }
        } else {
            double sq = this.getDeltaMovement().horizontalDistanceSqr();
            if (sq <= 1.0E-4) {
                next = BaseAnim.IDLE;
            } else if (sq > 0.05 || isAggressive() || getTarget() != null) {
                next = BaseAnim.RUN;
            } else {
                next = BaseAnim.WALK;
            }
        }

        if (movementMode == MovementMode.SITTING && !this.isVehicle()) {
            if (baseAnim != BaseAnim.IDLE) {
                baseAnim = BaseAnim.IDLE;
                idleController.onStartMoving();
                dispatcher.idle();
            }
            idleController.tick();
            return;
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
        if (baseAnim == BaseAnim.IDLE && !this.isVehicle()) idleController.tick();
    }

    // =========================================================================
    // Flight system
    // =========================================================================

    /** Subclasses override to false to disable flight entirely. */
    protected boolean canFly() { return true; }

    @Override
    public void handleStartJump(int jumpPower) {
        if (!isHorseFlying()) {
            jumpCount++;
            if (jumpCount == 1) dispatcher.jump();
            else if (jumpCount >= 2 && canFly()) activateFlight();
        }
    }

    @Override
    public void handleStopJump() {}

    private void activateFlight() {
        setHorseFlying(true);
        setNoGravity(true);
        baseAnim = null;
        setDeltaMovement(getDeltaMovement().x, FLY_ASCEND_SPEED * 1.5, getDeltaMovement().z);
    }

    private void handleFlight(Player player) {
        if (!isHorseFlying()) return;
        setNoGravity(true);
        double newY = jumpKeyHeld ? FLY_ASCEND_SPEED : FLY_DESCEND_SPEED;
        float  yRot = player.getYRot();
        double dx   = -Math.sin(Math.toRadians(yRot)) * FLY_FORWARD_SPEED * player.zza;
        double dz   =  Math.cos(Math.toRadians(yRot)) * FLY_FORWARD_SPEED * player.zza;
        setDeltaMovement(dx, newY, dz);
        setYRot(player.getYRot());
        this.yRotO = getYRot();
        if (onGround() && newY <= 0) {
            setHorseFlying(false);
            setNoGravity(false);
            jumpCount = 0;
        }
    }

    // =========================================================================
    // Misc overrides
    // =========================================================================

    @Override
    public boolean isSaddleable() { return !this.isBaby() && super.isSaddleable(); }

    @Override
    public boolean causeFallDamage(float dist, float mul, DamageSource src) { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isVehicle() && !this.getPassengers().isEmpty()
                && this.getPassengers().get(0) instanceof Player) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void travel(Vec3 v) {
        if (this.isVehicle() && this.isTamed() && !isHorseFlying())
            super.travel(new Vec3(v.x * GROUND_SPEED_BOOST, v.y, v.z * GROUND_SPEED_BOOST));
        else
            super.travel(v);
    }

    @Override
    public void aiStep() { super.aiStep(); }

    // =========================================================================
    // Interaction
    // =========================================================================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Acciones propias — solo servidor
        if (!level().isClientSide) {
            if (stack.is(net.minecraft.world.item.Items.SHEARS) && this.hasChest()) {
                for (int i = 3; i < this.getHorseInventory().getContainerSize(); i++) {
                    ItemStack item = this.getHorseInventory().getItem(i);
                    if (!item.isEmpty()) {
                        this.spawnAtLocation(item);
                        this.getHorseInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
                this.setChest(false);
                this.spawnAtLocation(net.minecraft.world.level.block.Blocks.CHEST.asItem());
                this.createInventory();
                this.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild)
                    stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                return InteractionResult.SUCCESS;
            }

            boolean isOwner = this.getOwnerUUID() == null
                    || player.getUUID().equals(this.getOwnerUUID());

            if (isOwner) {
                if (com.tobyink.millionhorses.entity.client.renderer.layer.HorseCarpetLayer.isCarpet(stack)
                        && this.getCarpetItem().isEmpty()) {
                    ItemStack carpet = stack.copy();
                    carpet.setCount(1);
                    this.setCarpetItem(carpet);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    this.playSound(SoundEvents.LLAMA_SWAG, 1.0F, 1.0F);
                    return InteractionResult.SUCCESS;
                }
                if (stack.is(net.minecraft.world.level.block.Blocks.CHEST.asItem()) && !this.hasChest()) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    this.setChest(true);
                    this.createInventory();
                    return InteractionResult.SUCCESS;
                }
            }
        }

        // Doma, montaje, inventario — vanilla AbstractHorse lo maneja en ambos lados
        return super.mobInteract(player, hand);
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
        if (id == 7) {
            tamingState = TamingState.NONE;
            tamingTimer = 0;
            dispatcher.idle();
        }
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (!this.level().isClientSide
                && (!this.isVehicle() || this.hasPassenger(player))
                && this.isTamed()
                && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            dev.architectury.registry.menu.MenuRegistry.openExtendedMenu(
                    sp,
                    new net.minecraft.world.MenuProvider() {
                        @Override
                        public net.minecraft.network.chat.Component getDisplayName() {
                            return AbstractMillionHorseEntity.this.getDisplayName();
                        }
                        @Override
                        public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                                int id, net.minecraft.world.entity.player.Inventory inv, Player p) {
                            return new com.tobyink.millionhorses.entity.menu.mHorsesMenu(
                                    id, inv,
                                    AbstractMillionHorseEntity.this.getHorseInventory(),
                                    AbstractMillionHorseEntity.this);
                        }
                    },
                    buf -> {
                        buf.writeInt(AbstractMillionHorseEntity.this.getId());
                        buf.writeBoolean(AbstractMillionHorseEntity.this.hasChest());
                        buf.writeInt(AbstractMillionHorseEntity.this.getChestSize());
                    }
            );
        }
    }

    // =========================================================================
    // Sounds


    // =========================================================================

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.HORSE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.HORSE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.HORSE_DEATH; }
    @Override protected SoundEvent getEatingSound() { return SoundEvents.HORSE_EAT; }
    @Override protected void playStepSound(BlockPos pos, BlockState state) {
        if (!this.isInWater()) {
            this.playSound(
                    state.getSoundType().getStepSound(),
                    0.15F, 1.0F
            );
        }
    }
    @Override public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.8f * this.getEyeHeight(), this.getBbWidth() * 0.4f);
    }

    // =========================================================================
    // Inner goals
    // =========================================================================

    static class FollowOwnerGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final AbstractMillionHorseEntity horse;
        private final double speedModifier;
        private final float  stopDistance;
        private final float  startDistance;
        private Player owner;
        private int timeToRecalcPath;

        FollowOwnerGoal(AbstractMillionHorseEntity horse, double speed,
                        float stopDist, float startDist) {
            this.horse         = horse;
            this.speedModifier = speed;
            this.stopDistance  = stopDist;
            this.startDistance = startDist;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (horse.getMovementMode() != MovementMode.FOLLOWING) return false;
            if (!horse.isTamed() || horse.getOwnerUUID() == null) return false;
            net.minecraft.world.entity.LivingEntity le = horse.getOwner();
            if (!(le instanceof Player p)) return false;
            owner = p;
            return horse.distanceToSqr(owner) > (double)(startDistance * startDistance);
        }

        @Override public boolean canContinueToUse() {
            if (horse.getMovementMode() != MovementMode.FOLLOWING) return false;
            if (owner == null || !owner.isAlive()) return false;
            return horse.distanceToSqr(owner) > (double)(stopDistance * stopDistance);
        }

        @Override public void start() { timeToRecalcPath = 0; }
        @Override public void stop()  { owner = null; horse.getNavigation().stop(); }

        @Override public void tick() {
            if (owner == null) return;
            horse.getLookControl().setLookAt(owner, 10.0F, horse.getMaxHeadXRot());
            if (--timeToRecalcPath <= 0) {
                timeToRecalcPath = 10;
                if (!horse.isLeashed()) horse.getNavigation().moveTo(owner, speedModifier);
            }
        }
    }

    static class SitGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final AbstractMillionHorseEntity horse;

        SitGoal(AbstractMillionHorseEntity horse) {
            this.horse = horse;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override public boolean canUse() {
            return horse.isTamed()
                    && horse.getMovementMode() == MovementMode.SITTING
                    && !horse.isVehicle()
                    && horse.onGround();
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public void start() {
            horse.getNavigation().stop();
            horse.setDeltaMovement(0, horse.getDeltaMovement().y, 0);
        }

        @Override public void tick() { horse.getNavigation().stop(); }
    }
}