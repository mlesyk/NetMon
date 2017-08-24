package org.mlesyk.server.snmp;

import org.mlesyk.Loggers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Maks on 20.08.2017.
 */
public class SnmpReaderWorkerThread implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Loggers.SNMP);

    private CommunityTarget target;
    private Snmp snmp;
    private TransportMapping transport;
    private boolean connectionEstablished = false;
    private String community;
    private final String host;
    private String hostPort;
    private InetAddress hostAddress;
    private int retries = 10;
    private int timeout = 5000;
    private int version = SnmpConstants.version2c;
    private int requestType = PDU.GETNEXT;
    private List<OID> oidStorage = new ArrayList<OID>();
    private List<VariableBinding> responseStorage = new ArrayList<VariableBinding>();

    public SnmpReaderWorkerThread(String community, String host, String hostPort) {
        this.community = community;
        this.host = host;
        this.hostPort = hostPort;
    }

    public void run() {
        try {
            List<VariableBinding> response = new ArrayList<>();
            this.prepareSnmpConnection();
            switch (this.requestType) {
                case PDU.GETNEXT:
                    response = this.doGetNext(oidStorage);
                    break;
                case PDU.GETBULK:
                    response = this.doBulkGet(oidStorage);
                    break;
                case PDU.GET:
                    break;
            }
            for(VariableBinding vb: response) {
                System.out.println(vb.getOid() + "\t" + vb.getVariable().toString());
            }
            responseStorage.addAll(response);
            this.closeSnmpSession();
        } catch (IOException e) {
            LOGGER.debug("Failed reading from device" + hostAddress + ":" + hostPort
                    + "; SNMP operation " + PDU.getTypeString(requestType), e);
        }
    }

    public List<OID> getOidStorage() {
        return oidStorage;
    }

    public void setOidStorage(List<OID> oidStorage) {
        this.oidStorage = oidStorage;
    }

    public List<VariableBinding> getResponseStorage() {
        return responseStorage;
    }

    public int getRequestType() {
        return requestType;
    }

    public void setRequestType(int requestType) {
        this.requestType = requestType;
    }

    private boolean prepareSnmpConnection() {
        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            hostAddress = InetAddress.getByName(host);
            target = new CommunityTarget();
            target.setCommunity(new OctetString(community));
            Address targetAddress = GenericAddress.parse(hostAddress + "/" + hostPort);
            target.setAddress(targetAddress);
            target.setRetries(retries);
            target.setTimeout(timeout);
            target.setVersion(version);
            snmp.listen();
            connectionEstablished = true;
        } catch (UnknownHostException e) {
            LOGGER.error("Can not get the host name, " + host + e.getMessage());
            connectionEstablished = false;
        } catch (IOException e) {
            LOGGER.error("Connection error " + e.getMessage());
            connectionEstablished = false;
        } catch (Exception e) {
            LOGGER.error("Can not connect to the host, " + host + e.getMessage());
            connectionEstablished = false;
        }
        return connectionEstablished;
    }

    private void closeSnmpSession() {
        try {
            if (snmp != null) {
                snmp.close();
                snmp = null;
            }
            if (transport != null) {
                transport.close();
                transport = null;
            }
        } catch (IOException e) {
            LOGGER.debug(e.getMessage());
        }
    }

    private List<VariableBinding> doGetNext(List<OID> getNextOids) throws IOException {
            PDU request = new PDU();
            request.setType(PDU.GETNEXT);
            request.addAll(getNextOids.stream()
                    .map(oid -> new VariableBinding(oid))
                    .collect(Collectors.toList()));
            request.setMaxRepetitions(1);
            request.setNonRepeaters(0);
            ResponseEvent responseEvent = snmp.send(request, target);
            PDU response = responseEvent.getResponse();
            return new ArrayList<VariableBinding>(response.getVariableBindings());
    }

    private List<VariableBinding> doBulkGet(List<OID> bulkGetOids) {
        int maxRows = 5;
        int maxRepetition = 3;
        OID[] oids = bulkGetOids.toArray(new OID[bulkGetOids.size()]);
        List<VariableBinding> response = new ArrayList<>();
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        try {
            List<TreeEvent> treeEventList = treeUtils.walk(target, oids);
            if (treeEventList == null || treeEventList.size() == 0) {
                System.out.println("Error: Unable to read table...");
                return response;
            }
            for (TreeEvent event : treeEventList) {
                VariableBinding[] varsBinds = event.getVariableBindings();
                if (varsBinds != null) {
                    response.addAll(Arrays.asList(varsBinds));
                }
            }
        } catch(Exception e) {
            System.out.println("error");
        }
        return response;
    }

    private boolean isConnectionEstablished() {
        return connectionEstablished;
    }
}
