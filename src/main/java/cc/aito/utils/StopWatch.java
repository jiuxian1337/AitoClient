package cc.aito.utils;

public class StopWatch {
    private long millis;

    public StopWatch() {
        reset();
    }

    public boolean finished(long delay) {
        return System.currentTimeMillis() - delay >= millis;
    }

    public void reset() {
        this.millis = System.currentTimeMillis();
    }
}
