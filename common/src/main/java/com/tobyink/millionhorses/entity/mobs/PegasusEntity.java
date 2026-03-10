package com.tobyink.millionhorses.entity.mobs;

import com.tobyink.millionhorses.entity.constant.HorseAnimations;
import com.tobyink.millionhorses.entity.constant.MovementMode;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PegasusEntity extends AbstractChestedHorse {

    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F295");

    // --- Synced Data ---
    private static final EntityDataAccessor<Integer> PEGASUS_VARIANT =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PEGASUS_FLYING =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> CARPET_ITEM_DATA =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.ITEM_STACK);

    protected boolean variantSetByNbt = false;
    private boolean babyBornPlayed  = false;
    private boolean wasBaby         = false;

    // Grupos de atributos (0-9, igual que Bedrock)
    // Wild: 0-5. Bred: puede llegar a 9 por herencia.
    private int healthGroupId = 3;
    private int speedGroupId  = 3;
    private int jumpGroupId   = 3;

    // --- Animation State ---
    public HorseAnimations dispatcher;
    public final MoveAnalysis moveAnalysis;
    public HorseIdleController idleController;

    private enum BaseAnim { IDLE, WALK, RUN, FLY }
    private BaseAnim baseAnim = null;
    private boolean isRearing = false;

    // --- Movement Mode (silbato) ---
    private MovementMode movementMode = MovementMode.WANDERING;

    // Goals que se activan/desactivan al cambiar de modo
    private FollowOwnerGoal followGoal;
    private SitGoal        sitGoal;

    // --- Taming Animation State ---
    private enum TamingState { NONE, BUCKING, REARING }
    private TamingState tamingState = TamingState.NONE;
    private int         tamingTimer = 0;

    private int getBuckDuration() {
        int temper = this.getTemper();
        int maxTemper = this.getMaxTemper();
        float ratio = (float) temper / maxTemper;
        return (int)(140 - ratio * 80) + this.random.nextInt(40);
    }
    private static final int REAR_DURATION_TAMING = 60;

    // --- Flight State ---
    private int     jumpCount    = 0;
    private boolean wasOnGround  = true;
    private boolean jumpKeyHeld  = false;
    private double  prevY        = 0.0;

    public static double FLY_ASCEND_SPEED   = 0.12;
    public static double FLY_DESCEND_SPEED  = -0.06;
    public static double FLY_FORWARD_SPEED  = 0.55;
    public static double GROUND_SPEED_BOOST = 1.6;
    public static int    CLIFF_MIN_BLOCKS   = 5;

    // --- Constructor ---
    public PegasusEntity(EntityType<? extends AbstractChestedHorse> entityType, Level level) {
        super(entityType, level);
        this.moveAnalysis   = new MoveAnalysis(this);
        this.dispatcher     = new HorseAnimations(this);
        this.idleController = new HorseIdleController(this, this.dispatcher);
    }

    // --- Synced Data ---
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PEGASUS_VARIANT, 0);
        this.entityData.define(PEGASUS_FLYING, false);
        this.entityData.define(CARPET_ITEM_DATA, ItemStack.EMPTY);
    }

    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
    }

    // --- Variant ---
    public PegasusVariant getPegasusVariant() {
        return PegasusVariant.byId(this.entityData.get(PEGASUS_VARIANT));
    }
    public void setPegasusVariant(PegasusVariant variant) {
        this.entityData.set(PEGASUS_VARIANT, variant.getId());
    }

    // --- Movement Mode ---
    public MovementMode getMovementMode() { return this.movementMode; }
    public void setMovementMode(MovementMode mode) {
        this.movementMode = mode;
        // Al cambiar de modo siempre volvemos a idle —
        // el tick se encargará de actualizar la animación correcta
        if (!level().isClientSide) {
            baseAnim = null;
            dispatcher.idle();
        }
    }
    public boolean isSitting() { return this.movementMode == MovementMode.SITTING; }

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
        int size = 3 + (chest ? 15 : 0);
        this.inventory = new SimpleContainer(size) {
            @Override
            public void setChanged() {
                super.setChanged();
                PegasusEntity.this.containerChanged(PegasusEntity.this.inventory);
            }
            @Override
            public void stopOpen(net.minecraft.world.entity.player.Player player) {
                // No limpiar el inventario al cerrar la GUI
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
        tag.putInt("HealthGroupId", this.healthGroupId);
        tag.putInt("SpeedGroupId",  this.speedGroupId);
        tag.putInt("JumpGroupId",   this.jumpGroupId);
        tag.putString("MovementMode", this.movementMode.name());
        if (!this.inventory.getItem(1).isEmpty()) {
            tag.put("ArmorItem", this.inventory.getItem(1).save(new CompoundTag()));
        }
        if (!this.inventory.getItem(2).isEmpty()) {
            tag.put("CarpetItem", this.inventory.getItem(2).save(new CompoundTag()));
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
            if (tag.contains("CarpetItem", 10)) {
                ItemStack carpetStack = ItemStack.of(tag.getCompound("CarpetItem"));
                if (!carpetStack.isEmpty()) {
                    this.inventory.setItem(2, carpetStack);
                    this.entityData.set(CARPET_ITEM_DATA, carpetStack.copy());
                }
            }
            this.variantSetByNbt = true;
        } else if (!this.variantSetByNbt) {
            this.setPegasusVariant(randomVariant());
            this.variantSetByNbt = true;
        }
        this.babyBornPlayed = tag.getBoolean("BabyBornPlayed");
        if (tag.contains("HealthGroupId")) {
            this.healthGroupId = tag.getInt("HealthGroupId");
            this.speedGroupId  = tag.getInt("SpeedGroupId");
            this.jumpGroupId   = tag.getInt("JumpGroupId");
        }
        if (tag.contains("MovementMode")) {
            try {
                this.movementMode = MovementMode.valueOf(tag.getString("MovementMode"));
            } catch (IllegalArgumentException e) {
                this.movementMode = MovementMode.WANDERING;
            }
        }
        this.updateContainerEquipment();
    }

    // --- Spawn ---
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
        if (!variantSetByNbt) {
            this.setPegasusVariant(randomVariant());
            variantSetByNbt = true;
        }
        if (tag == null || tag.isEmpty()) {
            this.randomizeAttributes(this.random);
        }
        return data;
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

        boolean thisHasBlue  = this.getPegasusVariant().name().endsWith("_BLUE");
        boolean otherHasBlue = other.getPegasusVariant().name().endsWith("_BLUE");

        PegasusVariant parentBase = this.random.nextBoolean()
                ? this.getPegasusVariant()
                : other.getPegasusVariant();

        boolean foalHasBlue = this.random.nextBoolean() ? thisHasBlue : otherHasBlue;
        String baseName = parentBase.name().replace("_BLUE", "");
        String foalVariantName = foalHasBlue ? baseName + "_BLUE" : baseName;
        PegasusVariant foalVariant;
        try {
            foalVariant = PegasusVariant.valueOf(foalVariantName);
        } catch (IllegalArgumentException e) {
            foalVariant = parentBase;
        }

        foal.setPegasusVariant(foalVariant);
        foal.variantSetByNbt = true;
        foal.randomizeAttributesFromParents(this.random, this, other);
        return foal;
    }

    // --- Armor ---
    @Override public boolean canWearArmor() { return true; }
    @Override public boolean isArmor(ItemStack stack) { return stack.getItem() instanceof HorseArmorItem; }
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

    public ItemStack getArmorItem() { return this.getItemBySlot(EquipmentSlot.CHEST); }
    public net.minecraft.world.SimpleContainer getHorseInventory() { return this.inventory; }

    // --- Carpet (slot 2) ---
    @Override
    public void containerChanged(net.minecraft.world.Container container) {
        super.containerChanged(container);
        if (!level().isClientSide && this.inventory != null) {
            ItemStack carpetInSlot = this.inventory.getItem(2);
            ItemStack currentSync = this.entityData.get(CARPET_ITEM_DATA);
            if (!ItemStack.matches(carpetInSlot, currentSync)) {
                this.entityData.set(CARPET_ITEM_DATA, carpetInSlot.isEmpty() ? ItemStack.EMPTY : carpetInSlot.copy());
            }
        }
    }
    public ItemStack getCarpetItem() { return this.entityData.get(CARPET_ITEM_DATA); }
    public void setCarpetItem(ItemStack stack) {
        if (this.inventory != null) this.inventory.setItem(2, stack);
        this.entityData.set(CARPET_ITEM_DATA, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
    }

    // --- Spawn rules ---
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType reason) {
        if (this.blockPosition().getY() < 175) return false;
        net.minecraft.core.BlockPos below = this.blockPosition().below();
        return level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.HAY_BLOCK);
    }

    public static boolean checkPegasusSpawnRules(
            EntityType<PegasusEntity> type,
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.entity.MobSpawnType reason,
            net.minecraft.core.BlockPos pos,
            net.minecraft.util.RandomSource random) {
        if (pos.getY() < 175) return false;
        net.minecraft.core.BlockPos below = pos.below();
        return level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.HAY_BLOCK);
    }

    // --- Variante aleatoria con rareza ---
    private PegasusVariant randomVariant() {
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

    // --- Attributes ---
    private static double randomInRange(RandomSource random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }
    private static double healthForGroup(RandomSource random, int group) {
        return randomInRange(random, 20.0 + group * 3.0, 23.0 + group * 3.0);
    }
    private static double speedForGroup(RandomSource random, int group) {
        return randomInRange(random, 0.230 + group * 0.032, 0.262 + group * 0.032);
    }
    private static double jumpForGroup(RandomSource random, int group) {
        return randomInRange(random, 0.70 + group * 0.06, 0.76 + group * 0.06);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractHorse.createBaseHorseAttributes()
                .add(Attributes.MAX_HEALTH, 99.0)
                .add(Attributes.MOVEMENT_SPEED, 10.0)
                .add(Attributes.JUMP_STRENGTH, 5.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    protected void randomizeAttributes(RandomSource random) {
        Objects.requireNonNull(random);
        this.healthGroupId = random.nextInt(6);
        this.speedGroupId  = random.nextInt(6);
        this.jumpGroupId   = random.nextInt(6);
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(healthForGroup(random, this.healthGroupId));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speedForGroup(random, this.speedGroupId));
        this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(jumpForGroup(random, this.jumpGroupId));
    }

    public void randomizeAttributesFromParents(RandomSource random, PegasusEntity parent1, PegasusEntity parent2) {
        this.healthGroupId = Math.min(9, Math.max(0, Math.round((parent1.healthGroupId + parent2.healthGroupId) / 2.0f)));
        this.speedGroupId  = Math.min(9, Math.max(0, Math.round((parent1.speedGroupId  + parent2.speedGroupId)  / 2.0f)));
        this.jumpGroupId   = Math.min(9, Math.max(0, Math.round((parent1.jumpGroupId   + parent2.jumpGroupId)   / 2.0f)));
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(healthForGroup(random, this.healthGroupId));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speedForGroup(random, this.speedGroupId));
        this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(jumpForGroup(random, this.jumpGroupId));
    }

    // --- Goals ---
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new RunAroundLikeCrazyGoal(this, 1.2));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.2));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));

        this.followGoal = new FollowOwnerGoal(this, 1.1, 4.0F, 16.0F);
        this.sitGoal    = new SitGoal(this);
        this.goalSelector.addGoal(4, this.followGoal);
        this.goalSelector.addGoal(4, this.sitGoal);

        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
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
            if (!this.isTamed() && this.isVehicle()) {
                dispatcher.buck();
            } else {
                dispatcher.rear();
            }
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

        boolean currentlyOnGround = this.onGround();

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
            if (this.isPegasusFlying()) {
                setPegasusFlying(false);
                setNoGravity(false);
                jumpKeyHeld = false;
                jumpCount = 0;
            }
            if (currentlyOnGround && this.isNoGravity()) {
                setNoGravity(false);
            }
        }

        if (currentlyOnGround && !wasOnGround) jumpCount = 0;
        wasOnGround = currentlyOnGround;

        if (level().isClientSide) return;
        if (isRearing && tamingState == TamingState.NONE) return;

        // --- Taming animation cycle ---
        if (tamingState != TamingState.NONE) {
            tamingTimer--;
            if (tamingState == TamingState.BUCKING) {
                dispatcher.buck();
                if (tamingTimer <= 0) {
                    this.ejectPassengers();
                    tamingState = TamingState.REARING;
                    tamingTimer = REAR_DURATION_TAMING;
                    dispatcher.rear();
                    this.playSound(SoundEvents.HORSE_ANGRY, 1.0F, 1.0F);
                }
            } else if (tamingState == TamingState.REARING) {
                if (tamingTimer <= 0) {
                    tamingState = TamingState.NONE;
                    dispatcher.idle();
                }
            }
        }

        // Detectar precipicio
        if (this.isVehicle() && this.isTamed() && !isPegasusFlying()) {
            double deltaY = this.getY() - prevY;
            if (!onGround() && deltaY < -0.1) {
                boolean isCliff = true;
                for (int i = 1; i <= CLIFF_MIN_BLOCKS; i++) {
                    BlockPos below = blockPosition().below(i);
                    if (!level().isEmptyBlock(below)) { isCliff = false; break; }
                }
                if (isCliff) activateFlight();
            }
        }
        prevY = this.getY();

        // Animación baby
        if (this.isBaby()) {
            wasBaby = true;
            if (!babyBornPlayed) {
                dispatcher.babyBorn();
                babyBornPlayed = true;
            }
            dispatcher.babyPose();
        }
        if (wasBaby && !this.isBaby()) {
            wasBaby = false;
            baseAnim = null;
            dispatcher.idle();
        }

        if (tamingState != TamingState.NONE) {
            prevY = this.getY();
            return;
        }

        // --- Animación base ---
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

        // Modo SITTING: forzar IDLE y dejar al idleController hacer sus animaciones normales
        // El SitGoal ya bloquea el movimiento físico, aquí solo manejamos la animación
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

        if (baseAnim == BaseAnim.IDLE && !this.isVehicle()) {
            idleController.tick();
        }
    }

    // --- Sistema de Vuelo ---
    @Override
    public void handleStartJump(int jumpPower) {
        if (!isPegasusFlying()) {
            jumpCount++;
            if (jumpCount == 1) {
                dispatcher.jump();
            } else if (jumpCount >= 2) {
                activateFlight();
            }
        }
    }

    @Override
    public void handleStopJump() {}

    private void activateFlight() {
        setPegasusFlying(true);
        setNoGravity(true);
        baseAnim = null;
        setDeltaMovement(getDeltaMovement().x, FLY_ASCEND_SPEED * 1.5, getDeltaMovement().z);
    }

    private void handleFlight(Player player) {
        if (!isPegasusFlying()) return;
        setNoGravity(true);
        double newY = jumpKeyHeld ? FLY_ASCEND_SPEED : FLY_DESCEND_SPEED;
        float yRot = player.getYRot();
        double dx = -Math.sin(Math.toRadians(yRot)) * FLY_FORWARD_SPEED * player.zza;
        double dz =  Math.cos(Math.toRadians(yRot)) * FLY_FORWARD_SPEED * player.zza;
        setDeltaMovement(dx, newY, dz);
        setYRot(player.getYRot());
        this.yRotO = getYRot();
        if (onGround() && newY <= 0) {
            setPegasusFlying(false);
            setNoGravity(false);
            jumpCount = 0;
        }
    }

    @Override
    public boolean isSaddleable() { return !this.isBaby() && super.isSaddleable(); }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isVehicle() && !this.getPassengers().isEmpty()) {
            Entity rider = this.getPassengers().get(0);
            if (rider instanceof Player) return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void travel(Vec3 travelVec) {
        if (this.isVehicle() && this.isTamed() && !isPegasusFlying()) {
            super.travel(new Vec3(travelVec.x * GROUND_SPEED_BOOST, travelVec.y, travelVec.z * GROUND_SPEED_BOOST));
        } else {
            super.travel(travelVec);
        }
    }

    @Override
    public void aiStep() { super.aiStep(); }

    // --- Interacción ---
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;

        if (stack.is(net.minecraft.world.item.Items.SHEARS)) {
            if (this.hasChest()) {
                for (int i = 3; i < this.getHorseInventory().getContainerSize(); i++) {
                    ItemStack chestItem = this.getHorseInventory().getItem(i);
                    if (!chestItem.isEmpty()) {
                        this.spawnAtLocation(chestItem);
                        this.getHorseInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
                this.setChest(false);
                this.spawnAtLocation(net.minecraft.world.level.block.Blocks.CHEST.asItem());
                this.createInventory();
                this.playSound(net.minecraft.sounds.SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) stack.hurtAndBreak(1, player,
                        p -> p.broadcastBreakEvent(hand));
                return InteractionResult.SUCCESS;
            }
        }

        boolean isOwner = this.getOwnerUUID() == null
                || player.getUUID().equals(this.getOwnerUUID());

        if (isOwner) {
            if (com.tobyink.millionhorses.entity.client.renderer.layer.PegasusCarpetLayer.isCarpet(stack)
                    && this.getCarpetItem().isEmpty()) {
                ItemStack carpet = stack.copy();
                carpet.setCount(1);
                this.setCarpetItem(carpet);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.playSound(net.minecraft.sounds.SoundEvents.LLAMA_SWAG, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            if (stack.is(net.minecraft.world.level.block.Blocks.CHEST.asItem()) && !this.hasChest()) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.setChest(true);
                this.createInventory();
                return InteractionResult.SUCCESS;
            }
        }

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
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            dev.architectury.registry.menu.MenuRegistry.openExtendedMenu(
                    serverPlayer,
                    new net.minecraft.world.MenuProvider() {
                        @Override
                        public net.minecraft.network.chat.Component getDisplayName() {
                            return net.minecraft.network.chat.Component.translatable("entity.millionhorses.pegasus");
                        }
                        @Override
                        public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                                int id, net.minecraft.world.entity.player.Inventory inv, Player p) {
                            return new com.tobyink.millionhorses.entity.menu.mHorsesMenu(
                                    id, inv, PegasusEntity.this.getHorseInventory(), PegasusEntity.this);
                        }
                    },
                    buf -> { buf.writeInt(PegasusEntity.this.getId()); buf.writeBoolean(PegasusEntity.this.hasChest()); }
            );
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

    // -------------------------------------------------------------------------
    // Goal: seguir al dueño (modo FOLLOWING)
    // -------------------------------------------------------------------------
    static class FollowOwnerGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final PegasusEntity pegasus;
        private final double speedModifier;
        private final float stopDistance;
        private final float startDistance;
        private Player owner;
        private int timeToRecalcPath;

        FollowOwnerGoal(PegasusEntity pegasus, double speed, float stopDist, float startDist) {
            this.pegasus       = pegasus;
            this.speedModifier = speed;
            this.stopDistance  = stopDist;
            this.startDistance = startDist;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (pegasus.getMovementMode() != MovementMode.FOLLOWING) return false;
            if (!pegasus.isTamed()) return false;
            if (pegasus.getOwnerUUID() == null) return false;
            net.minecraft.world.entity.LivingEntity le = pegasus.getOwner();
            if (!(le instanceof Player p)) return false;
            owner = p;
            return pegasus.distanceToSqr(owner) > (double)(startDistance * startDistance);
        }

        @Override
        public boolean canContinueToUse() {
            if (pegasus.getMovementMode() != MovementMode.FOLLOWING) return false;
            if (owner == null || !owner.isAlive()) return false;
            return pegasus.distanceToSqr(owner) > (double)(stopDistance * stopDistance);
        }

        @Override public void start() { timeToRecalcPath = 0; }

        @Override
        public void stop() {
            owner = null;
            pegasus.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (owner == null) return;
            pegasus.getLookControl().setLookAt(owner, 10.0F, pegasus.getMaxHeadXRot());
            if (--timeToRecalcPath <= 0) {
                timeToRecalcPath = 10;
                if (!pegasus.isLeashed()) {
                    pegasus.getNavigation().moveTo(owner, speedModifier);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Goal: quedarse quieto con idle (modo SITTING)
    // Bloquea MOVE y JUMP para que el pegaso no se mueva por sus propios goals
    // -------------------------------------------------------------------------
    static class SitGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final PegasusEntity pegasus;

        SitGoal(PegasusEntity pegasus) {
            this.pegasus = pegasus;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return pegasus.isTamed()
                    && pegasus.getMovementMode() == MovementMode.SITTING
                    && !pegasus.isVehicle()
                    && pegasus.onGround();
        }

        @Override
        public boolean canContinueToUse() { return canUse(); }

        @Override
        public void start() {
            pegasus.getNavigation().stop();
            pegasus.setDeltaMovement(0, pegasus.getDeltaMovement().y, 0);
        }

        @Override
        public void tick() {
            pegasus.getNavigation().stop();
        }
    }
}