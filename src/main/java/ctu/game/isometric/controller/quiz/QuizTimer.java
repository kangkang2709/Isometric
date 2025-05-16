package ctu.game.isometric.controller.quiz;

import com.badlogic.gdx.utils.Timer;

public class QuizTimer {
    private float timeLimit;
    private float timeRemaining;
    private boolean isRunning;
    private TimerCallback callback;

    public interface TimerCallback {
        void onTimerTick(float timeRemaining);
        void onTimerComplete();
    }

    public QuizTimer(float timeLimit, TimerCallback callback) {
        this.timeLimit = timeLimit;
        this.timeRemaining = timeLimit;
        this.callback = callback;
        this.isRunning = false;
    }

    public void start() {
        if (!isRunning) {
            isRunning = true;
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    updateTimer(0.1f);
                }
            }, 0, 0.1f);
        }
    }

    public void pause() {
        isRunning = false;
    }

    public void reset() {
        timeRemaining = timeLimit;
        isRunning = false;
    }

    private void updateTimer(float delta) {
        if (!isRunning) return;

        timeRemaining -= delta;
        if (timeRemaining <= 0) {
            timeRemaining = 0;
            isRunning = false;
            if (callback != null) {
                callback.onTimerComplete();
            }
        } else if (callback != null) {
            callback.onTimerTick(timeRemaining);
        }
    }

    public float getTimeRemaining() {
        return timeRemaining;
    }

    public float getElapsedTime() {
        return timeLimit - timeRemaining;
    }

    public float getTimeLimit() {
        return timeLimit;
    }
}