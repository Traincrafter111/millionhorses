package com.tobyink.millionhorses.entity.mobs;

import com.tobyink.millionhorses.entity.constant.HorseAnimations;

public class PegasusIdleController {

    public enum IdleState {
        IDLE, SIT, SLEEP_STAND, SLEEP_SIT, SLEEP_LAY, TRANSITIONING, REARING
    }

    private static final int IDLE_ANIM_INTERVAL_MIN = 80;
    private static final int IDLE_ANIM_INTERVAL_MAX = 200;
    private static final int TIME_TO_SIT   = 600;
    private static final int TIME_TO_SLEEP = 1200;
    private static final int PLAYER_DETECT_RANGE = 6;
    private static final int STANDUP_DURATION = 40;
    private static final int UNLAY_DURATION   = 60;
    private static final int TRANSITION_TO_SIT_DURATION  = 60;
    private static final int TRANSITION_TO_LAY_DURATION  = 60;
    private static final int REAR_DURATION = 60; // duración del rear idle antes de volver

    private IdleState state = IdleState.IDLE;
    private int idleTimer       = 0;
    private int animTimer       = 0;
    private int transitionTimer = 0;
    private boolean inSit       = false;

    private final PegasusEntity pegasus;
    private final HorseAnimations dispatcher;

    public PegasusIdleController(PegasusEntity pegasus, HorseAnimations dispatcher) {
        this.pegasus    = pegasus;
        this.dispatcher = dispatcher;
        this.animTimer  = nextAnimInterval();
    }

    public void tick() {
        idleTimer++;
        animTimer--;
        if (transitionTimer > 0) transitionTimer--;

        boolean playerNear = isPlayerNear();

        switch (state) {

            case IDLE -> {
                // Jugador cerca y estamos en sit → levantarse
                if (inSit && playerNear) {
                    dispatcher.standup();
                    startTransition(STANDUP_DURATION);
                    inSit = false;
                    state = IdleState.TRANSITIONING;
                    break;
                }

                // Tiempo suficiente → ir a dormir
                if (idleTimer > TIME_TO_SLEEP && !playerNear) {
                    state = IdleState.SLEEP_STAND;
                    dispatcher.sleepStand();
                    startTransition(TRANSITION_TO_SIT_DURATION);
                    break;
                }

                // Tiempo medio → sentarse
                if (idleTimer > TIME_TO_SIT && !playerNear && !inSit) {
                    inSit = true;
                    dispatcher.idleSit();
                    break;
                }

                // Animación idle aleatoria periódica
                if (animTimer <= 0) {
                    animTimer = nextAnimInterval();
                    if (!inSit) playRandomIdleAnim();
                }
            }

            case REARING -> {
                // Si hay jugador cerca o termina el tiempo, volver a idle
                if (playerNear || transitionTimer <= 0) {
                    state = IdleState.IDLE;
                    animTimer = nextAnimInterval();
                    dispatcher.idle();
                }
            }

            case SLEEP_STAND -> {
                if (playerNear) { wakeUp(); break; }
                if (transitionTimer <= 0) {
                    state = IdleState.SLEEP_SIT;
                    dispatcher.sleepSit();
                    startTransition(TRANSITION_TO_LAY_DURATION);
                }
            }

            case SLEEP_SIT -> {
                if (playerNear) { wakeUp(); break; }
                if (transitionTimer <= 0) {
                    state = IdleState.SLEEP_LAY;
                    dispatcher.sleepLay();
                }
            }

            case SLEEP_LAY -> {
                if (playerNear) wakeUp();
            }

            case TRANSITIONING -> {
                if (transitionTimer <= 0) {
                    state    = IdleState.IDLE;
                    idleTimer = 0;
                    inSit    = false;
                    animTimer = nextAnimInterval();
                    dispatcher.idle();
                }
            }
        }
    }

    public void onStartMoving() {
        state         = IdleState.IDLE;
        idleTimer     = 0;
        animTimer     = nextAnimInterval();
        inSit         = false;
        transitionTimer = 0;
    }

    public boolean isSleeping() {
        return state == IdleState.SLEEP_LAY
                || state == IdleState.SLEEP_SIT
                || state == IdleState.SLEEP_STAND;
    }

    public IdleState getState() { return state; }

    // --- Privados ---

    private void wakeUp() {
        if (state == IdleState.SLEEP_LAY) {
            dispatcher.sleepUnlay();
            startTransition(UNLAY_DURATION);
        } else {
            startTransition(20);
        }
        state = IdleState.TRANSITIONING;
        inSit = false;
    }

    private void playRandomIdleAnim() {
        int roll = pegasus.getRandom().nextInt(7);
        switch (roll) {
            case 0 -> dispatcher.idleTail();
            case 1 -> dispatcher.idleTail2();
            case 2 -> dispatcher.idleRearOld();
            case 3 -> dispatcher.idleRoll();
            case 4 -> dispatcher.idleHead();
            case 5 -> {
                // Rear idle: entry + loop por REAR_DURATION ticks
                dispatcher.rearEntry();
                dispatcher.rear();
                state = IdleState.REARING;
                startTransition(REAR_DURATION);
            }
            case 6 -> dispatcher.idleTail(); // más probabilidad de tail para balancear
        }
    }

    private boolean isPlayerNear() {
        return pegasus.level().getNearestPlayer(pegasus, PLAYER_DETECT_RANGE) != null;
    }

    private void startTransition(int durationTicks) {
        transitionTimer = durationTicks;
    }

    private int nextAnimInterval() {
        return IDLE_ANIM_INTERVAL_MIN +
                pegasus.getRandom().nextInt(IDLE_ANIM_INTERVAL_MAX - IDLE_ANIM_INTERVAL_MIN);
    }
}