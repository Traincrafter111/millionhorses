package com.tobyink.millionhorses.entity.constant;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;

public class HorseAnimations {

    public static final String BASE_CONTROLLER   = "base_controller";
    public static final String TAIL_CONTROLLER   = "tail_controller";
    public static final String ACTION_CONTROLLER = "action_controller";
    public static final String SLEEP_CONTROLLER  = "sleep_controller";

    private static final AzCommand POSE_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.pose", AzPlayBehaviors.PLAY_ONCE
    );
    private static final AzCommand IDLE_BASE_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.idle_base", AzPlayBehaviors.LOOP
    );
    private static final AzCommand WALK_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.walk", AzPlayBehaviors.LOOP
    );
    private static final AzCommand RUN_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.run", AzPlayBehaviors.LOOP
    );
    private static final AzCommand FLY_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.fly", AzPlayBehaviors.LOOP
    );
    private static final AzCommand JUMP_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.jump", AzPlayBehaviors.PLAY_ONCE
    );
    private static final AzCommand IDLE_TAIL_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.idle_tail", AzPlayBehaviors.LOOP
    );
    private static final AzCommand IDLE_TAIL2_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.idle_tail2", AzPlayBehaviors.LOOP
    );
    private static final AzCommand IDLE_HEAD_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.idle_head", AzPlayBehaviors.LOOP
    );
    private static final AzCommand IDLE_REAROLD_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.idle_rearold", AzPlayBehaviors.LOOP
    );
    private static final AzCommand IDLE_ROLL_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.idle_roll", AzPlayBehaviors.PLAY_ONCE
    );
    private static final AzCommand IDLE_SIT_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.idle_sit", AzPlayBehaviors.LOOP
    );
    private static final AzCommand STANDUP_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.standup", AzPlayBehaviors.PLAY_ONCE
    );
    private static final AzCommand BUCK_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.buck", AzPlayBehaviors.PLAY_ONCE
    );
    private static final AzCommand REAR_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.rear", AzPlayBehaviors.LOOP
    );
    private static final AzCommand REAR_ENTRY_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.rear_entry", AzPlayBehaviors.PLAY_ONCE
    );
    private static final AzCommand SLEEP_STAND_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.sleep_stand", AzPlayBehaviors.LOOP
    );
    private static final AzCommand SLEEP_SIT_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.sleep_sit", AzPlayBehaviors.LOOP
    );
    private static final AzCommand SLEEP_LAY_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.sleep_lay", AzPlayBehaviors.LOOP
    );
    private static final AzCommand SLEEP_UNLAY_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.sleep_unlay", AzPlayBehaviors.PLAY_ONCE
    );
    private static final AzCommand BABY_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.baby", AzPlayBehaviors.LOOP
    );
    private static final AzCommand BABYBORN_COMMAND = AzCommand.create(
            BASE_CONTROLLER, "animation.cyn_horse.babyborn", AzPlayBehaviors.PLAY_ONCE
    );

    private final Entity animatedEntity;

    public HorseAnimations(Entity animatable) {
        this.animatedEntity = animatable;
    }

    public void pose()       { POSE_COMMAND.sendForEntity(animatedEntity); }
    public void idle()       { IDLE_BASE_COMMAND.sendForEntity(animatedEntity); }
    public void walk()       { WALK_COMMAND.sendForEntity(animatedEntity); }
    public void run()        { RUN_COMMAND.sendForEntity(animatedEntity); }
    public void fly()        { FLY_COMMAND.sendForEntity(animatedEntity); }
    public void jump()       { JUMP_COMMAND.sendForEntity(animatedEntity); }
    public void idleTail()   { IDLE_TAIL_COMMAND.sendForEntity(animatedEntity); }
    public void idleTail2()  { IDLE_TAIL2_COMMAND.sendForEntity(animatedEntity); }
    public void idleHead()   { IDLE_HEAD_COMMAND.sendForEntity(animatedEntity); }
    public void idleRearOld(){ IDLE_REAROLD_COMMAND.sendForEntity(animatedEntity); }
    public void idleRoll()   { IDLE_ROLL_COMMAND.sendForEntity(animatedEntity); }
    public void idleSit()    { IDLE_SIT_COMMAND.sendForEntity(animatedEntity); }
    public void standup()    { STANDUP_COMMAND.sendForEntity(animatedEntity); }
    public void buck()       { BUCK_COMMAND.sendForEntity(animatedEntity); }
    public void rear()       { REAR_COMMAND.sendForEntity(animatedEntity); }
    public void rearEntry()  { REAR_ENTRY_COMMAND.sendForEntity(animatedEntity); }
    public void sleepStand() { SLEEP_STAND_COMMAND.sendForEntity(animatedEntity); }
    public void sleepSit()   { SLEEP_SIT_COMMAND.sendForEntity(animatedEntity); }
    public void sleepLay()   { SLEEP_LAY_COMMAND.sendForEntity(animatedEntity); }
    public void sleepUnlay() { SLEEP_UNLAY_COMMAND.sendForEntity(animatedEntity); }
    public void baby()       { BABY_COMMAND.sendForEntity(animatedEntity); }
    public void babyBorn()   { BABYBORN_COMMAND.sendForEntity(animatedEntity); }
}