/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.albasim.tools.jcrgc;

import ch.qos.logback.classic.Logger;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ReadConcern;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import org.apache.jackrabbit.oak.plugins.blob.MarkSweepGarbageCollector;
import org.apache.jackrabbit.oak.plugins.document.DocumentMK;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore;
import org.apache.jackrabbit.oak.plugins.document.VersionGarbageCollector;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxence
 */
public class GC {

    public static final Logger logger = (Logger) LoggerFactory.getLogger("JCR GC");
    private DB db;
    private DocumentNodeStore nodeStore;

    public void init(String strUri) throws URISyntaxException {
        final URI uri = new URI(strUri);
        if (uri.getScheme().equals("mongodb")) {
            // Remote
            String hostPort = uri.getHost();
            if (uri.getPort() > -1) {
                hostPort += ":" + uri.getPort();
            }
            String dbName = uri.getPath().replaceFirst("/", "");
            this.db = new MongoClient(hostPort, MongoClientOptions.builder()
                    .readConcern(ReadConcern.MAJORITY)
                    .build())
                    .getDB(dbName);
            this.nodeStore = new DocumentMK.Builder()
                    .setLeaseCheck(false)
                    .setMongoDB(db)
                    .getNodeStore();
        }
    }

    public void close() {
        nodeStore.dispose();
    }

    /**
     *
     * @param maxAge in seconds
     */
    public void revisionGC(Long maxAge) {
        logger.info("revisionGC(): OAK GarbageCollection");
        try {
            if (nodeStore != null) {
                long eMaxAge = (maxAge != null ? maxAge : 60 * 60 * 24);

                VersionGarbageCollector versionGc = nodeStore.getVersionGarbageCollector();
                logger.info("revisionGC(): start VersionGC");
                VersionGarbageCollector.VersionGCStats gc = versionGc.gc(eMaxAge, TimeUnit.SECONDS);
                logger.info("revisionGC(): versionGC done: {}", gc);


                MarkSweepGarbageCollector blobGC = nodeStore.createBlobGarbageCollector(eMaxAge, "oak");

                if (blobGC != null) {
                    try {
                        logger.info("check blob consistency");
                        long nbBlobs = blobGC.checkConsistency();
                        if (nbBlobs >= 0) {
                            logger.info("check blob consistency => {}", nbBlobs);
                        } else {
                            logger.error("check blob consistency => {}", nbBlobs);
                        }
                    } catch (Exception ex) {
                        logger.error("CheckConsistency failed with {}", ex);
                    }

                    try {
                        logger.info("Collect Blobs garbage");
                        blobGC.collectGarbage(false);
                        logger.info("Collect Blobs garbage done");
                    } catch (Exception ex) {
                        logger.error("collect blobs failed with {}", ex);
                    }
                } else {
                    logger.error("blobGC is null");
                }
            } else {
                logger.error("nodeStore is null");
            }

        } catch (Exception ex) {
            logger.error("Error while revisionGC: {}", ex);
        }
    }

}
