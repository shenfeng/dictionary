package me.shenfeng;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MakeupIdelConnection {

    public static void main(String[] args) throws IOException {

        int totalIdleConnection = 5000;
        final Selector selector = Selector.open();

        final AtomicBoolean running = new AtomicBoolean(true);

        InetSocketAddress addrs[] = {
                new InetSocketAddress("127.0.0.1", 9090),
                new InetSocketAddress("192.168.1.114", 9090) };

        final List<SocketChannel> all = new ArrayList<SocketChannel>(
                totalIdleConnection * addrs.length);

        // sudo echo 1025 65535 > /proc/sys/net/ipv4/ip_local_port_range
        // sudo sysctl -w net.ipv4.ip_local_port_range="1025 65535"
        // cat /proc/net/sockstat

        for (int i = 0; i < totalIdleConnection; ++i) {
            for (InetSocketAddress addr : addrs) {
                SocketChannel ch = SocketChannel.open();
                ch.socket().setReuseAddress(true);
                ch.configureBlocking(false);
                ch.register(selector, SelectionKey.OP_CONNECT,
                        SelectAttachment.next());
                ch.connect(addr);
                all.add(ch);
            }
        }

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                running.set(false);
                selector.wakeup();
            }
        }, TimeUnit.SECONDS.toMillis(15));

        int connected = 0;

        while (running.get()) {
            int select = selector.select();
            if (select > 0) {
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectedKeys.iterator();

                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    if (key.isConnectable()) {
                        SocketChannel ch = (SocketChannel) key.channel();
                        if (ch.finishConnect()) {
                            ++connected;
                            if (connected
                                    % (totalIdleConnection * addrs.length / 10) == 0) {
                                System.out.println("connected: " + connected);
                            }
                        }
                    }
                }
                selectedKeys.clear();
            }
        }
        System.out.println("close all connection");
        for (SocketChannel ch : all) {
            try {
                ch.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("close all connection ok");
        timer.cancel(); // shutdown jvm
    }
}
