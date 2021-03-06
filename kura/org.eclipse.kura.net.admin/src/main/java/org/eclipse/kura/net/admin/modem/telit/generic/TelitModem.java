/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.telit.generic;

import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.net.modem.ModemDriver;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.modem.UsbModemDriver;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.net.modem.CellularModem.SerialPortType;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TelitModem {

    private static final Logger s_logger = LoggerFactory.getLogger(TelitModem.class);

    protected static Object s_atLock = new Object();

    protected String m_model;
    protected String m_manufacturer;
    protected String m_serialNumber;
    protected String m_revisionId;
    protected int m_rssi;
    protected Boolean m_gpsSupported;
    protected String m_imsi;
    protected String m_iccid;

    private boolean m_gpsEnabled;
    private ModemDevice m_device;
    private final String m_platform;
    private final ConnectionFactory m_connectionFactory;
    private List<NetConfig> m_netConfigs = null;

    public TelitModem(ModemDevice device, String platform, ConnectionFactory connectionFactory) {

        this.m_device = device;
        this.m_platform = platform;
        this.m_connectionFactory = connectionFactory;
        this.m_gpsEnabled = false;
    }

    public void reset() throws KuraException {

        boolean status = false;
        int offOnDelay = 1000;

        sleep(5000);
        while (true) {
            try {
                status = turnOff();
                if (status) {
                    sleep(offOnDelay);
                    status = turnOn();
                    if (!status && isOnGpio()) {
                        s_logger.info("reset() :: {} seconds delay, then turn OFF/ON", 35);
                        offOnDelay = 35000;
                        continue;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (status) {
                s_logger.info("reset() :: modem reset successful");
                break;
            } else {
                s_logger.info("reset() :: modem reset failed");
                sleep(1000);
            }
        }
    }

    public String getModel() throws KuraException {
        synchronized (s_atLock) {
            if (this.m_model == null) {
                s_logger.debug("sendCommand getModelNumber :: {}", TelitModemAtCommands.getModelNumber.getCommand());
                byte[] reply = null;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            "Modem not available for AT commands: " + TelitHe910.class.getName());
                }
                try {
                    reply = commAtConnection.sendCommand(TelitModemAtCommands.getModelNumber.getCommand().getBytes(),
                            1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    this.m_model = getResponseString(reply);
                    reply = null;
                }
            }
        }
        return this.m_model;
    }

    public String getManufacturer() throws KuraException {
        synchronized (s_atLock) {
            if (this.m_manufacturer == null) {
                s_logger.debug("sendCommand getManufacturer :: {}", TelitModemAtCommands.getManufacturer.getCommand());
                byte[] reply = null;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            "Modem not available for AT commands: " + TelitHe910.class.getName());
                }
                try {
                    reply = commAtConnection.sendCommand(TelitModemAtCommands.getManufacturer.getCommand().getBytes(),
                            1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    this.m_manufacturer = getResponseString(reply);
                    reply = null;
                }
            }
        }
        return this.m_manufacturer;
    }

    public String getSerialNumber() throws KuraException {
        synchronized (s_atLock) {
            if (this.m_serialNumber == null) {
                s_logger.debug("sendCommand getSerialNumber :: {}", TelitModemAtCommands.getSerialNumber.getCommand());
                byte[] reply = null;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            "Modem not available for AT commands: " + TelitHe910.class.getName());
                }
                try {
                    reply = commAtConnection.sendCommand(TelitModemAtCommands.getSerialNumber.getCommand().getBytes(),
                            1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    String serialNum = getResponseString(reply);
                    if (serialNum != null && !serialNum.isEmpty()) {
                        if (serialNum.startsWith("#CGSN:")) {
                            serialNum = serialNum.substring("#CGSN:".length()).trim();
                        }
                        this.m_serialNumber = serialNum;
                    }
                }
            }
        }
        return this.m_serialNumber;
    }

    public String getRevisionID() throws KuraException {
        synchronized (s_atLock) {
            if (this.m_revisionId == null) {
                s_logger.debug("sendCommand getRevision :: {}", TelitModemAtCommands.getRevision.getCommand());
                byte[] reply = null;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            "Modem not available for AT commands: " + TelitHe910.class.getName());
                }
                try {
                    reply = commAtConnection.sendCommand(TelitModemAtCommands.getRevision.getCommand().getBytes(), 1000,
                            100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    this.m_revisionId = getResponseString(reply);
                }
            }
        }
        return this.m_revisionId;
    }

    public boolean isReachable() throws KuraException {
        boolean ret = false;
        synchronized (s_atLock) {
            CommConnection commAtConnection = openSerialPort(getAtPort());
            ret = isAtReachable(commAtConnection);
            closeSerialPort(commAtConnection);
        }
        return ret;
    }

    public boolean isPortReachable(String port) {
        boolean ret = false;
        synchronized (s_atLock) {
            try {
                CommConnection commAtConnection = openSerialPort(port);
                closeSerialPort(commAtConnection);
                ret = true;
            } catch (KuraException e) {
                s_logger.warn("isPortReachable() :: The {} is not reachable", port);
            }
        }
        return ret;
    }

    public int getSignalStrength() throws KuraException {

        int signalStrength = -113;
        synchronized (s_atLock) {
            String atPort = getAtPort();
            String gpsPort = getGpsPort();
            if ((atPort.equals(getDataPort()) || atPort.equals(gpsPort) && this.m_gpsEnabled) && this.m_rssi < 0) {
                s_logger.trace(
                        "getSignalStrength() :: returning previously obtained RSSI={} :: m_gpsEnabled={}, m_rssi, m_gpsEnabled");
                return this.m_rssi;
            }

            s_logger.debug("sendCommand getSignalStrength :: {}", TelitModemAtCommands.getSignalStrength.getCommand());
            byte[] reply = null;
            CommConnection commAtConnection = openSerialPort(atPort);
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        "Modem not available for AT commands: " + TelitHe910.class.getName());
            }
            try {
                reply = commAtConnection.sendCommand(TelitModemAtCommands.getSignalStrength.getCommand().getBytes(),
                        1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String[] asCsq = null;
                String sCsq = this.getResponseString(reply);
                if (sCsq.startsWith("+CSQ:")) {
                    sCsq = sCsq.substring("+CSQ:".length()).trim();
                    s_logger.trace("getSignalStrength() :: +CSQ={}", sCsq);
                    asCsq = sCsq.split(",");
                    if (asCsq.length == 2) {
                        int rssi = Integer.parseInt(asCsq[0]);
                        if (rssi < 99) {
                            signalStrength = -113 + 2 * rssi;
                        }
                        s_logger.trace("getSignalStrength() :: signalStrength={}", signalStrength);
                    }
                }
                reply = null;
            }
        }
        this.m_rssi = signalStrength;
        return signalStrength;
    }

    public boolean isGpsSupported() throws KuraException {
        if (this.m_gpsSupported != null) {
            return this.m_gpsSupported;
        }
        if (getGpsPort() == null) {
            this.m_gpsSupported = false;
            return this.m_gpsSupported;
        }
        synchronized (s_atLock) {
            if (this.m_gpsSupported == null) {
                s_logger.debug("sendCommand isGpsSupported :: {}", TelitModemAtCommands.isGpsPowered.getCommand());
                byte[] reply = null;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            "Modem not available for AT commands: " + TelitHe910.class.getName());
                }

                try {
                    reply = commAtConnection.sendCommand(TelitModemAtCommands.isGpsPowered.getCommand().getBytes(),
                            1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    String sReply = getResponseString(reply);
                    if (sReply != null && !sReply.isEmpty()) {
                        if (sReply.startsWith("$GPSP:")) {
                            this.m_gpsSupported = true;
                        } else {
                            this.m_gpsSupported = false;
                        }
                    }
                }
            }
        }
        return this.m_gpsSupported;
    }

    public void enableGps() throws KuraException {
        enableGps(TelitModemAtCommands.gpsPowerUp.getCommand());
    }

    protected void enableGps(String gpsPowerupCommand) throws KuraException {
        if (this.m_gpsSupported == null || this.m_gpsSupported == false) {
            s_logger.warn("enableGps() :: GPS NOT SUPPORTED");
            this.m_gpsEnabled = false;
            return;
        }
        synchronized (s_atLock) {
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        "Modem not available for AT commands: " + TelitHe910.class.getName());
            }

            byte[] reply = null;
            int numAttempts = 3;
            while (numAttempts > 0) {
                String atPort = getAtPort();
                String gpsPort = getGpsPort();
                String gpsEnableNMEAcommand = formGpsEnableNMEACommand(atPort, gpsPort);
                try {
                    if (!isGpsPowered(commAtConnection)) {
                        s_logger.debug("enableGps() :: sendCommand gpsPowerUp :: {}", gpsPowerupCommand);
                        commAtConnection.sendCommand(gpsPowerupCommand.getBytes(), 1000, 100);
                    }
                    s_logger.debug("enableGps() :: sendCommand gpsEnableNMEA :: {}", gpsEnableNMEAcommand);
                    reply = commAtConnection.sendCommand(gpsEnableNMEAcommand.getBytes(), 3000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }

                if (reply != null && reply.length > 0) {
                    String sReply = getResponseString(reply);
                    if (sReply != null) {
                        if (atPort.equals(gpsPort)) {
                            s_logger.trace("enableGps() :: gpsEnableNMEA reply={}", sReply);
                            if (!sReply.isEmpty() && sReply.startsWith("CONNECT")) {
                                s_logger.info("enableGps() :: Modem replied to the {} command with 'CONNECT'",
                                        gpsEnableNMEAcommand);
                                s_logger.info("enableGps() :: !!! Modem GPS enabled !!!");
                                this.m_gpsEnabled = true;
                                break;
                            }
                        } else {
                            if (sReply.isEmpty()) {
                                s_logger.info("enableGps() :: Modem replied to the {} command with 'OK'",
                                        gpsEnableNMEAcommand);
                                s_logger.info("enableGps() :: !!! Modem GPS enabled !!!");
                                this.m_gpsEnabled = true;
                                break;
                            }
                        }
                    }
                }
                numAttempts--;
                sleep(2000);
            }

            closeSerialPort(commAtConnection);
        }
    }

    public void disableGps() throws KuraException {
        if (this.m_gpsSupported == null || this.m_gpsSupported == false) {
            s_logger.warn("disableGps() :: GPS NOT SUPPORTED");
            this.m_gpsEnabled = false;
            return;
        }
        synchronized (s_atLock) {
            CommConnection commAtConnection = openSerialPort(getAtPort());
            try {
                String atPort = getAtPort();
                String gpsPort = getGpsPort();
                if (atPort.equals(gpsPort) && !isAtReachable(commAtConnection)) {
                    int numAttempts = 3;
                    while (numAttempts > 0) {
                        s_logger.debug("disableGps() :: sendCommand gpsDisableNMEA {}",
                                TelitModemAtCommands.gpsDisableNMEA.getCommand());

                        byte[] reply = commAtConnection
                                .sendCommand(TelitModemAtCommands.gpsDisableNMEA.getCommand().getBytes(), 1000, 100);
                        if (reply != null && reply.length > 0) {
                            s_logger.trace("disableGps() :: reply={}", new String(reply));
                            String sReply = new String(reply);
                            if (sReply.contains("NO CARRIER")) {
                                s_logger.info(
                                        "disableGps() :: Modem replied with 'NO CARRIER' to the +++ escape sequence");
                                sleep(2000);
                                if (isAtReachable(commAtConnection)) {
                                    s_logger.info("disableGps() :: !!! Modem GPS disabled !!!, OK");
                                    this.m_gpsEnabled = false;
                                    break;
                                } else {
                                    s_logger.error("disableGps() :: [1] Failed to disable modem GPS");
                                    numAttempts--;
                                }
                            } else {
                                if (isAtReachable(commAtConnection)) {
                                    s_logger.warn(
                                            "disableGps() :: Modem didn't reply with 'NO CARRIER' to the +++ escape sequence but port is AT reachable");
                                    s_logger.info("disableGps() :: Will assume that GPS is disabled");
                                    this.m_gpsEnabled = false;
                                    break;
                                } else {
                                    s_logger.error("disableGps() :: [2] Failed to disable modem GPS");
                                    numAttempts--;
                                }
                            }
                        } else {
                            s_logger.error("disableGps() :: [3] Failed to disable modem GPS");
                            numAttempts--;
                        }
                        sleep(2000);
                    }
                } else {
                    s_logger.warn("disableGps() :: Modem GPS has already been disabled");
                    this.m_gpsEnabled = false;
                }

                s_logger.debug("disableGps() :: sendCommand gpsPowerDown :: {}",
                        TelitModemAtCommands.gpsPowerDown.getCommand());
                commAtConnection.sendCommand(TelitModemAtCommands.gpsPowerDown.getCommand().getBytes(), 1000, 100);

            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
            closeSerialPort(commAtConnection);
        }
    }

    public String getMobileSubscriberIdentity() throws KuraException {
        synchronized (s_atLock) {
            if (this.m_imsi == null) {
                if (isSimCardReady()) {
                    s_logger.debug("sendCommand getIMSI :: {}", TelitModemAtCommands.getIMSI.getCommand());
                    byte[] reply = null;
                    CommConnection commAtConnection = openSerialPort(getAtPort());
                    if (!isAtReachable(commAtConnection)) {
                        closeSerialPort(commAtConnection);
                        throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                                "Modem not available for AT commands: " + TelitHe910.class.getName());
                    }
                    try {
                        reply = commAtConnection.sendCommand(TelitModemAtCommands.getIMSI.getCommand().getBytes(), 1000,
                                100);
                    } catch (IOException e) {
                        closeSerialPort(commAtConnection);
                        throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                    }
                    closeSerialPort(commAtConnection);
                    if (reply != null) {
                        String imsi = getResponseString(reply);
                        if (imsi != null && !imsi.isEmpty()) {
                            if (imsi.startsWith("#CIMI:")) {
                                imsi = imsi.substring("#CIMI:".length()).trim();
                            }
                            this.m_imsi = imsi;
                        }
                    }
                }
            }
        }
        return this.m_imsi;
    }

    public String getIntegratedCirquitCardId() throws KuraException {
        synchronized (s_atLock) {
            if (this.m_iccid == null) {
                if (isSimCardReady()) {
                    s_logger.debug("sendCommand getICCID :: {}", TelitModemAtCommands.getICCID.getCommand());
                    byte[] reply = null;
                    CommConnection commAtConnection = openSerialPort(getAtPort());
                    if (!isAtReachable(commAtConnection)) {
                        closeSerialPort(commAtConnection);
                        throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                                "Modem not available for AT commands: " + TelitHe910.class.getName());
                    }
                    try {
                        reply = commAtConnection.sendCommand(TelitModemAtCommands.getICCID.getCommand().getBytes(),
                                1000, 100);
                    } catch (IOException e) {
                        closeSerialPort(commAtConnection);
                        throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                    }
                    closeSerialPort(commAtConnection);
                    if (reply != null) {
                        String iccid = getResponseString(reply);
                        if (iccid != null && !iccid.isEmpty()) {
                            if (iccid.startsWith("#CCID:")) {
                                iccid = iccid.substring("#CCID:".length()).trim();
                            }
                            this.m_iccid = iccid;
                        }
                    }
                }
            }
        }
        return this.m_iccid;
    }

    public String getDataPort() throws KuraException {
        String port = null;
        List<String> ports = this.m_device.getSerialPorts();
        if (ports != null && ports.size() > 0) {
            if (this.m_device instanceof UsbModemDevice) {
                SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.m_device);
                if (usbModemInfo != null) {
                    port = ports.get(usbModemInfo.getDataPort());
                } else {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No PPP serial port available");
                }
            } else if (this.m_device instanceof SerialModemDevice) {
                SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
                if (serialModemInfo != null) {
                    port = serialModemInfo.getDriver().getComm().getDataPort();
                } else {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No PPP serial port available");
                }
            } else {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Unsupported modem device");
            }
        } else {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No serial ports available");
        }
        return port;
    }

    public String getAtPort() throws KuraException {
        String port = null;
        List<String> ports = this.m_device.getSerialPorts();
        if (ports != null && ports.size() > 0) {
            if (this.m_device instanceof UsbModemDevice) {
                SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.m_device);
                if (usbModemInfo != null) {
                    port = ports.get(usbModemInfo.getAtPort());
                } else {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No AT serial port available");
                }
            } else if (this.m_device instanceof SerialModemDevice) {
                SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
                if (serialModemInfo != null) {
                    port = serialModemInfo.getDriver().getComm().getAtPort();
                } else {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No AT serial port available");
                }
            } else {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Unsupported modem device");
            }
        } else {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No serial ports available");
        }
        return port;
    }

    public String getGpsPort() throws KuraException {
        String port = null;
        List<String> ports = this.m_device.getSerialPorts();
        if (ports != null && ports.size() > 0) {
            if (this.m_device instanceof UsbModemDevice) {
                SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.m_device);
                if (usbModemInfo != null) {
                    int gpsPort = usbModemInfo.getGpsPort();
                    if (gpsPort >= 0) {
                        port = ports.get(gpsPort);
                    }
                } else {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No GPS serial port available");
                }
            } else if (this.m_device instanceof SerialModemDevice) {
                SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
                if (serialModemInfo != null) {
                    port = serialModemInfo.getDriver().getComm().getGpsPort();
                } else {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No GPS serial port available");
                }
            } else {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Unsupported modem device");
            }
        } else {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No serial ports available");
        }
        return port;
    }

    public ModemDevice getModemDevice() {
        return this.m_device;
    }

    public void setModemDevice(ModemDevice device) {
        this.m_device = device;
    }

    public List<NetConfig> getConfiguration() {
        return this.m_netConfigs;
    }

    public void setConfiguration(List<NetConfig> netConfigs) {
        this.m_netConfigs = netConfigs;
    }

    public CommURI getSerialConnectionProperties(SerialPortType portType) throws KuraException {
        CommURI commURI = null;
        try {
            String port;
            if (portType == SerialPortType.ATPORT) {
                port = getAtPort();
            } else if (portType == SerialPortType.DATAPORT) {
                port = getDataPort();
            } else if (portType == SerialPortType.GPSPORT) {
                port = getGpsPort();
            } else {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Invalid Port Type");
            }
            if (port != null) {
                StringBuffer sb = new StringBuffer();
                sb.append("comm:").append(port).append(";baudrate=115200;databits=8;stopbits=1;parity=0");
                commURI = CommURI.parseString(sb.toString());
            }
        } catch (URISyntaxException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "URI Syntax Exception");
        }
        return commURI;
    }

    public boolean isGpsEnabled() {
        return this.m_gpsEnabled;
    }

    public abstract boolean isSimCardReady() throws KuraException;

    protected CommConnection openSerialPort(String port) throws KuraException {

        CommConnection connection = null;
        if (this.m_connectionFactory != null) {
            String uri = new CommURI.Builder(port).withBaudRate(115200).withDataBits(8).withStopBits(1).withParity(0)
                    .withTimeout(2000).build().toString();

            try {
                connection = (CommConnection) this.m_connectionFactory.createConnection(uri, 1, false);
            } catch (Exception e) {
                s_logger.debug("Exception creating connection: " + e);
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e, "Connection Failed");
            }
        }
        return connection;
    }

    protected void closeSerialPort(CommConnection connection) throws KuraException {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    protected boolean isAtReachable(CommConnection connection) {

        boolean status = false;
        int attemptNo = 0;
        do {
            try {
                s_logger.trace("isAtReachable() :: sending AT commnd to modem on port {}",
                        connection.getURI().getPort());
                byte[] reply = connection.sendCommand(TelitModemAtCommands.at.getCommand().getBytes(), 1000, 100);
                if (reply.length > 0) {
                    String sReply = new String(reply);
                    if (sReply.contains("OK")) {
                        status = true;
                    }
                }
            } catch (Exception e) {
                sleep(2000);
            } finally {
                attemptNo++;
            }
        } while (status == false && attemptNo < 3);

        s_logger.trace("isAtReachable() :: port={}, status={}", connection.getURI().getPort(), status);
        return status;
    }

    // Parse the AT command response for the relevant info
    protected String getResponseString(String resp) {
        if (resp == null) {
            return "";
        }

        // remove the command and space at the beginning, and the 'OK' and spaces at the end
        return resp.replaceFirst("^\\S*\\s*", "").replaceFirst("\\s*(OK)?\\s*$", "");
    }

    protected String getResponseString(byte[] resp) {
        if (resp == null) {
            return "";
        }

        return getResponseString(new String(resp));
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private boolean isGpsPowered(CommConnection commAtConnection) throws KuraException {

        boolean gpsPowered = false;
        if (this.m_gpsSupported == null || this.m_gpsSupported == false) {
            return false;
        }

        s_logger.debug("sendCommand isGpsPowered :: {}", TelitModemAtCommands.isGpsPowered.getCommand());
        byte[] reply = null;
        try {
            reply = commAtConnection.sendCommand(TelitModemAtCommands.isGpsPowered.getCommand().getBytes(), 1000, 100);
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
        if (reply != null) {
            String sReply = getResponseString(reply);
            if (sReply != null && !sReply.isEmpty()) {
                if (sReply.startsWith("$GPSP:")) {
                    sReply = sReply.substring("$GPSP:".length()).trim();
                    gpsPowered = sReply.equals("1") ? true : false;
                }
            }
        }

        return gpsPowered;
    }

    private boolean isOnGpio() throws Exception {

        boolean gpioOn = false;
        if (this.m_platform.equals("reliagate-10-20")) {
            FileReader fr = new FileReader("/sys/class/gpio/usb-rear-pwr/value");
            int data = fr.read();
            fr.close();
            s_logger.debug("isGpioOn() :: data={}", data);
            gpioOn = data == 48 ? false : true;
            s_logger.info("isGpioOn()? {}", gpioOn);
        }
        return gpioOn;
    }

    private boolean turnOff() throws Exception {

        boolean retVal = true;
        ModemDriver modemDriver = getModemDriver();
        if (modemDriver != null) {
            retVal = modemDriver.turnModemOff();
        }
        return retVal;
    }

    private boolean turnOn() throws Exception {

        boolean retVal = true;
        ModemDriver modemDriver = getModemDriver();
        if (modemDriver != null) {
            retVal = modemDriver.turnModemOn();
        }
        return retVal;
    }

    private ModemDriver getModemDriver() {

        if (this.m_device == null) {
            return null;
        }
        ModemDriver modemDriver = null;
        if (this.m_device instanceof UsbModemDevice) {
            SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.m_device);
            if (usbModemInfo != null) {
                List<? extends UsbModemDriver> usbDeviceDrivers = usbModemInfo.getDeviceDrivers();
                if (usbDeviceDrivers != null && usbDeviceDrivers.size() > 0) {
                    modemDriver = usbDeviceDrivers.get(0);
                }
            }
        } else if (this.m_device instanceof SerialModemDevice) {
            SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
            if (serialModemInfo != null) {
                modemDriver = serialModemInfo.getDriver();
            }
        }
        return modemDriver;
    }

    private String formGpsEnableNMEACommand(String atPort, String gpsPort) throws KuraException {

        StringBuilder sbCommand = new StringBuilder(TelitModemAtCommands.gpsEnableNMEA.getCommand());
        if (atPort.equals(gpsPort)) {
            sbCommand.append("3");
        } else {
            sbCommand.append("2");
        }
        sbCommand.append(",1,1,1,1,1,1\r\n");
        return sbCommand.toString();
    }
}
