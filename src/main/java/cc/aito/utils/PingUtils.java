package cc.aito.utils;

import net.minecraft.client.multiplayer.ServerAddress;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PingUtils implements Wrapper {

    private static final long TIMEOUT_AUTO_DISABLE = 120000;
    private static final long DELAY = 10000;
    private static final long DEFAULT_PING = 250;
    private static long ping = DEFAULT_PING;
    private static final ExecutorService thread = Executors.newFixedThreadPool(1);
    private static final StopWatch lastPing = new StopWatch();
    private static final StopWatch lastGrab = new StopWatch();

    private PingUtils() {
    }

    public static long getPing() {
        if (lastGrab.finished(TIMEOUT_AUTO_DISABLE)) {
            ping();
            lastGrab.reset();
            return DEFAULT_PING;
        } else {
            lastGrab.reset();
            return ping;
        }
    }

    public static void tick() {
        if (lastPing.finished(DELAY) && !lastGrab.finished(TIMEOUT_AUTO_DISABLE)) {
            ping();
            lastPing.reset();
        }
    }

    private static void ping() {
        if (mc.isIntegratedServerRunning()) {
            ping = 0;
            return;
        }
        thread.execute(() -> {
            lastPing.reset();
            ping = measurePing(getServerIp());
        });
    }

    private static long measurePing(String address) {
        try {
            ServerAddress serverAddress = ServerAddress.fromString(address);
            SocketAddress socketAddress = new InetSocketAddress(serverAddress.getIP(), serverAddress.getPort());
            Socket socket = new Socket();
            long time = System.currentTimeMillis();
            socket.connect(socketAddress);
            socket.close();
            return System.currentTimeMillis() - time;
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String getServerIp() {
        return mc.getCurrentServerData() == null ? "" : mc.getCurrentServerData().serverIP;
    }
}
