package me.mdbell.noexs.io.usb;

import me.mdbell.noexs.core.IConnection;
import org.usb4java.*;

import javax.usb.UsbException;
import java.io.IOException;

public class UsbLowLevelConnection implements IConnection {

    private final int DEFAULT_INTERFACE = 0;
    private final short DEFAULT_VENDOR_ID = 0x057e;
    private final short DEFAULT_PRODUCT_ID = 0x2000;

    private Context nsContext;
    private DeviceHandle nsHandle;

    private String protocol;


    /**
     * NOT WORKING RIGHT NOW
     * @throws Exception
     */
    public UsbLowLevelConnection() throws Exception {
        init();
    }

    private void init() throws Exception {
        try {
            nsContext = new Context();
            int result = LibUsb.init(nsContext);
            if (result != LibUsb.SUCCESS) {
                System.out.println("libusb init failed");
                throw new UsbException("libusb init failed");
            }
            DeviceList deviceList = new DeviceList();
            result = LibUsb.getDeviceList(nsContext, deviceList);
            if (result < 0) {
                System.out.println("Failed to get device list");
                throw new UsbException("Failed to get device list");
            }
            DeviceDescriptor descriptor;
            Device nsDevice = null;
            for (Device device : deviceList) {
                descriptor = new DeviceDescriptor();                // mmm.. leave it as is.
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result != LibUsb.SUCCESS){
                    System.out.println("Could not get device descriptor");
                    LibUsb.freeDeviceList(deviceList, true);
                    throw new UsbException("Could not get device descriptor");
                }
                short id_vendor = descriptor.idVendor();
                short id_product = descriptor.idProduct();
                System.out.println(descriptor.toString());
                if ((id_vendor == DEFAULT_VENDOR_ID) && id_product == DEFAULT_PRODUCT_ID){
                    nsDevice = device;
                    System.out.println("Read file descriptors for USB devices");
                    break;
                }
            }

            if (nsDevice == null) {
                throw new UsbException("Could not pick up nintendo switch device.");
            }
            nsHandle = new DeviceHandle();
            result = LibUsb.open(nsDevice, nsHandle);
            if (result != LibUsb.SUCCESS) {
                if (result == LibUsb.ERROR_ACCESS) {
                    throw new UsbException("Could not get access to nintendo switch device.");
                }
                throw new UsbException("Could not handler for nintendo switch device.");
            }
            LibUsb.freeDeviceList(deviceList, true);

            boolean canDetach = LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER);
            if (canDetach) {
                int usedByKernel = LibUsb.kernelDriverActive(nsHandle, DEFAULT_INTERFACE);
                if (usedByKernel == LibUsb.SUCCESS) {
                    // ur gud
                } else if (usedByKernel == 1) {
                    result = LibUsb.detachKernelDriver(nsHandle, DEFAULT_INTERFACE);
                    if (result != 0) {
                        System.out.println("Could not detach kernel");
                        System.out.println("Error code: " + result);
                        throw new UsbException("Could not detach kernel");
                    }
                } else {
                    // cannot proceed
                    throw new UsbException("Cannot proceed with libusb driver");
                }
            } else {
                System.out.println("libusb does not support kernel detachment. might be okay?");
            }
            result = LibUsb.setConfiguration(nsHandle, 1);
            if (result != LibUsb.SUCCESS) {
                System.out.println("Could not set config");
                throw new UsbException("Could not set config");
            }
            result = LibUsb.claimInterface(nsHandle, DEFAULT_INTERFACE);
            if (result != LibUsb.SUCCESS) {
                System.out.println("Could not claim interface");
                throw new UsbException("Could not claim interface");
            }
            // TODO: other shit
            System.out.println("looks like we made it :)");
        } catch (Exception e) {
            close();
            System.out.println("Error in init");
            throw new Exception(e);
        }
    }

    @Override
    public boolean connected() {
        return false;
    }

    @Override
    public void write(byte[] data, int off, int len) {

    }

    @Override
    public void writeByte(int i) {

    }

    @Override
    public int readByte() {
        return 0;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        return 0;
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws IOException {
        try {
            // Close handler in the end
            if (nsHandle != null) {
                // Try to release interface
                int result = LibUsb.releaseInterface(nsHandle, DEFAULT_INTERFACE);

                if (result != LibUsb.SUCCESS)
                    System.out.println("Release interface\n  Returned: "+result+" (sometimes it's not an issue)");
                else
                    System.out.println("Release interface");

                LibUsb.close(nsHandle);
                System.out.println("Requested handler close");
            }
            // Close context in the end
            if (nsContext != null) {
                LibUsb.exit(nsContext);
                System.out.println("Requested context close");
            }
            System.out.println("Closed");
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
