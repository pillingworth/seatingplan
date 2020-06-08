package co.uk.threeonefour.seatingplan;

import picocli.CommandLine;
import picocli.CommandLine.HelpCommand;

public class Application {

    public static final void main(String[] args) {

        // create picocli
        CommandLine cl = new CommandLine(new SeatingPlan());

        // add std help sub command
        cl.addSubcommand(new HelpCommand());

        // be flexible with enum casting
        cl.setCaseInsensitiveEnumValuesAllowed(true);

        // and execute
        cl.execute(args);
    }
}
