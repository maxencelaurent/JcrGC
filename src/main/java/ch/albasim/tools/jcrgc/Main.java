/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.albasim.tools.jcrgc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.net.URISyntaxException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxence
 */
public class Main {

    public static final Logger logger = (Logger) LoggerFactory.getLogger("JCR GC");

    public static void main(String... args) throws URISyntaxException {

        Option maxAgeOption = Option.builder("m").longOpt("maxAge")
                .required(false)
                .hasArg(true).numberOfArgs(1)
                .desc("remove garbage older than the given number of seconds (default 86400 => 1 days)")
                .build();

        Option uriOption = Option.builder("d").longOpt("db")
                .required(false)
                .hasArg(true).numberOfArgs(1)
                .desc("database url (default is mongo://localhost/oak)")
                .build();

        Option logLevelOption = Option.builder("v").longOpt("verbose")
                .required(false)
                .hasArg(true).numberOfArgs(1)
                .desc("logger level [TRACE, DEBUG, INFO, WARN, ERROR] (default is INFO)")
                .build();

        Options options = new Options();
        options.addOption(maxAgeOption);
        options.addOption(uriOption);
        options.addOption(logLevelOption);

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine commandLine = parser.parse(options, args);

            Level level = Level.INFO;
            if (commandLine.hasOption("v")) {
                level = Level.valueOf(commandLine.getOptionValue("v"));
            }
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(level);

            Long maxAge = null;
            if (commandLine.hasOption("m")) {
                maxAge = Long.parseLong(commandLine.getOptionValue("m"));
                logger.trace("MAX AGE: {}", maxAge);
            }

            String mongoUri;
            if (commandLine.hasOption("d")) {
                mongoUri = commandLine.getOptionValue("d");
            } else {
                mongoUri = "mongodb://localhost/oak";
            }

            GC gc = new GC();
            gc.init(mongoUri);
            gc.revisionGC(maxAge);
            gc.close();

        } catch (ParseException ex) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("java -jar JcrGC-0.0.1-SNAPSHOT-jar-with-dependencies.jar", options);
        }
    }
}
