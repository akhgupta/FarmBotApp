package com.farm.bot;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

public class UsbUtil {
    public static UsbSerialDevice setupSerial(UsbSerialDevice serialPort) {
        serialPort.setBaudRate(9600);
        serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
        serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
        serialPort.setParity(UsbSerialInterface.PARITY_NONE);
        serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
        String string = "w";
        serialPort.write(string.getBytes());
        return serialPort;

    }
}
