package org.mlesyk.server.snmp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.snmp4j.PDU;
import org.snmp4j.smi.OID;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Maks on 21.08.2017.
 */
public class SnmpReaderWorkerThreadTest {
    SnmpReaderWorkerThread worker = new SnmpReaderWorkerThread("public",
            "10.21.172.21","161");
    @Before
    public void setUp() throws Exception {
        List<OID> oids = new ArrayList<OID>();
        oids.add(new OID("1.3.6.1.2.1.2.2.1.6"));
        worker.setOidStorage(oids);
    }

    @Test
    public void getNextOperationTest() {
        boolean result = this.invokeSnmpOperation(PDU.GETNEXT);
        Assert.assertEquals(true, result);
    }

    @Test
    public void bulkGetOperationTest() {
        boolean result = this.invokeSnmpOperation(PDU.GETBULK);
        Assert.assertEquals(true, result);
    }

    private boolean invokeSnmpOperation(int requestType) {
        try {
            worker.setRequestType(requestType);
            Thread workerThread = new Thread(worker);
            workerThread.setDaemon(true);
            workerThread.start();
            Thread.sleep(10000);
            System.out.println(worker.getResponseStorage().size());
            return (worker.getResponseStorage().size() > 0);
        } catch (InterruptedException e) {
            // ignore
        }
        return false;
    }

}