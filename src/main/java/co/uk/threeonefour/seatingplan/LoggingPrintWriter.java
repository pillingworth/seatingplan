package co.uk.threeonefour.seatingplan;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;

/**
 * A print writer that writes to an slf4j logger
 */
public class LoggingPrintWriter extends PrintWriter {

    private final Logger logger;
    
    public LoggingPrintWriter(Logger logger) {
        super(new StringWriter());
        this.logger = logger;
    }

    public void println(String str) {
        super.print(str);
        println();
    }

    public void println() {
        flush();
        logger.info(out.toString());
        ((StringWriter) out).getBuffer().setLength(0);
    }
}