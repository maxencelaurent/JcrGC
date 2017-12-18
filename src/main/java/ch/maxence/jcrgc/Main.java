/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.maxence.jcrgc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.net.URISyntaxException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxence
 */
public class Main {

    public static final Logger logger = (Logger) LoggerFactory.getLogger("JCR GC");

    public static void main(String... args) throws URISyntaxException {
        logger.setLevel(Level.INFO);

        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        String mongoUri;

        if (args.length >= 1) {
            mongoUri = args[0];
        } else {
            mongoUri = "mongodb://localhost/oak";
        }

        GC gc = new GC();
        gc.init(mongoUri);
        gc.revisionGC();
        gc.close();
    }
}
