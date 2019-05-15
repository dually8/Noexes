package me.mdbell.noexs.ui.services;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import me.mdbell.noexs.core.IConnection;
import me.mdbell.noexs.io.net.SocketConnection;
import me.mdbell.noexs.io.usb.UsbConnection;
import me.mdbell.noexs.io.usb.UsbLowLevelConnection;
import me.mdbell.noexs.io.usb.UsbUtils;
import me.mdbell.noexs.ui.models.ConnectionType;

import javax.usb.UsbDevice;
import java.net.InetSocketAddress;
import java.net.Socket;

public class DebuggerConnectionService extends ScheduledService<IConnection> {
    private String host;
    private int port;
    private int timeout = 1000;
    private ConnectionType type;

    public DebuggerConnectionService() {
        super();
        setMaximumFailureCount(10);
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    protected Task<IConnection> createTask() {
        switch (type) {
            case NETWORK:
                return new Task<>() {
                    @Override
                    protected IConnection call() throws Exception {
                        updateMessage("Connecting to:" + host + ":" + port + " (Attempt:" + (getCurrentFailureCount() + 1) + "/" + getMaximumFailureCount() + ")");
                        Socket s = new Socket();
                        InetSocketAddress addr = new InetSocketAddress(host, port);
                        s.connect(addr, timeout);
                        s.setTcpNoDelay(true);
                        return new SocketConnection(s);
                    }
                };
            case USB:
                return new Task<>() {
                    @Override
                    protected IConnection call() throws Exception {
                        updateMessage("Connecting via USB...");
                        UsbDevice nsDevice = UsbUtils.findDevice(0x057e, 0x2000);
                        if (nsDevice != null) {
                            System.out.println("Found nintendo switch!");
                            return new UsbConnection(nsDevice);
                        }
//                        return new UsbLowLevelConnection();
                        throw new UnsupportedOperationException("Couldn't find nintendo switch");
                    }
                };
            default:
                return new Task<>() {
                    @Override
                    protected IConnection call() {
                        throw new UnsupportedOperationException("Unsupported connection type:" + type);
                    }
                };
        }
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setType(ConnectionType type) {
        this.type = type;
    }
}
