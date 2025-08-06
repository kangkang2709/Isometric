package ctu.game.isometric.model.perform;

import com.badlogic.gdx.utils.Array;

public class CircularBuffer<T extends Number> {
    private final Array<T> items;
    private final int maxSize;
    private int currentIndex;
    private boolean filled;

    public CircularBuffer(int maxSize) {
        this.maxSize = maxSize;
        this.items = new Array<>(false, maxSize);
        this.currentIndex = 0;
        this.filled = false;
    }

    public void add(T item) {
        if (items.size < maxSize) {
            items.add(item);
        } else {
            items.set(currentIndex, item);
        }

        currentIndex = (currentIndex + 1) % maxSize;
        if (currentIndex == 0) filled = true;
    }

    public Array<T> getItems() {
        return items;
    }

    public T getLast() {
        if (items.size == 0) return null;
        int lastIndex = currentIndex - 1;
        if (lastIndex < 0) lastIndex = items.size - 1;
        return items.get(lastIndex);
    }

    public float getMin() {
        if (items.size == 0) return 0f;
        float min = items.get(0).floatValue();
        for (T item : items) {
            if (item.floatValue() < min) min = item.floatValue();
        }
        return min;
    }

    public float getMax() {
        if (items.size == 0) return 0f;
        float max = items.get(0).floatValue();
        for (T item : items) {
            if (item.floatValue() > max) max = item.floatValue();
        }
        return max;
    }

    public int size() {
        return items.size;
    }

    public boolean isEmpty() {
        return items.size == 0;
    }
}