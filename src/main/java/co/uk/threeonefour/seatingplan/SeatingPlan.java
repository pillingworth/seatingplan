package co.uk.threeonefour.seatingplan;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.uk.threeonefour.seatingplan.model.Course;
import co.uk.threeonefour.seatingplan.model.Person;
import co.uk.threeonefour.seatingplan.model.Scenario;
import co.uk.threeonefour.seatingplan.model.SimpleScenario;
import co.uk.threeonefour.seatingplan.model.Solution;
import co.uk.threeonefour.seatingplan.model.Table;
import co.uk.threeonefour.seatingplan.model.TripleListSolution;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "seatingplan", description = "Seating plan using simple in-memory list of triples model", version = "1.0")
public class SeatingPlan implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SeatingPlan.class);

    @Option(names = { "-c",
            "--courses" }, description = "Number of courses.", paramLabel = "<courses>", defaultValue = "4")
    private int numberOfCourses;

    @Option(names = { "-t",
            "--tables" }, description = "Number of tables.", paramLabel = "<tables>", defaultValue = "5")
    private int numberOfTables;

    @Option(names = { "-s",
            "--seed" }, description = "Seed for random number generation.", paramLabel = "<seed>", defaultValue = "0")
    private long seed;

    @Option(names = { "-pf",
            "--peoplefile" }, description = "File of people to include. One person per line. Use # to 'comment' someone out.", paramLabel = "<peoplefile>", required = true)
    private Path peopleFilePath;

    @Option(names = { "-i",
            "--iterations" }, description = "How many iterations should be tried.", paramLabel = "<iterations>", defaultValue = "1000")
    private int iterations;

    @Option(names = { "-st",
            "--strategy" }, description = "Which strategies to use. Options are random|walk", paramLabel = "<strategies>", defaultValue = "swap")
    private List<String> strategies;

    @Override
    public void run() {

        /* build the scenario */
        Scenario scenario;
        try {
            scenario = createScenario();
        } catch (IOException e) {
            LOG.error("Failed to create scenario.", e);
            return;
        }

        Pair<Solution, Double> solutionScore;

        /* Solution strategy #1 */
        if (strategies.contains("random")) {
            solutionScore = bestRandomGuessStrategy(scenario, null);
            /* print the solution found */
            if (solutionScore.getLeft() != null) {
                printModel(scenario, solutionScore.getLeft(), solutionScore.getRight());
            } else {
                LOG.error("No valid solution found");
            }
        }

        /* Solution strategy #2 */
        if (strategies.contains("swap")) {
            solutionScore = tweakAndRepeatStrategy(scenario, null /* solutionScore.getLeft() */);
            /* print the solution found */
            if (solutionScore.getLeft() != null) {
                printModel(scenario, solutionScore.getLeft(), solutionScore.getRight());
            } else {
                LOG.error("No valid solution found");
            }
        }
    }

    protected final Scenario createScenario() throws IOException {

        SimpleScenario scenario = new SimpleScenario();

        /* read in the list of people */
        if (!Files.exists(peopleFilePath) || Files.isDirectory(peopleFilePath)) {
            throw new FileNotFoundException(
                    "Unable to open " + peopleFilePath + " as it either could not be found or is not a file.");
        }

        try (Scanner scanner = new Scanner(peopleFilePath)) {
            int i = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.startsWith("#")) {
                    int id = (i + 1);
                    String name = StringUtils.substringBeforeLast(line, ",").trim();
                    boolean host = "host".equalsIgnoreCase(StringUtils.substringAfterLast(line, ",").trim());
                    scenario.addPerson(new Person(id, name, host));
                    i++;
                }
            }
        }

        /* create a list of Courses */
        for (var i = 0; i < numberOfCourses; i++) {
            int id = i + 1;
            scenario.addCourse(new Course(id, "Course " + id));
        }

        /* create a list of Tables */
        for (var i = 0; i < numberOfTables; i++) {
            int id = i + 1;
            scenario.addTable(new Table(id, "Table " + id));
        }

        return scenario;
    }

    protected void printModel(Scenario scenario, Solution solution, double score) {

        try (LoggingPrintWriter lpw = new LoggingPrintWriter()) {

            lpw.println();
            lpw.println(String.format("Solution score %f", score));

            /* first the header rows */
            int colWidth = 12;
            lpw.println("");
            for (int hdrRow = 0; hdrRow < 2; hdrRow++) {

                lpw.print("| ");
                if (hdrRow == 0) {
                    lpw.print(truncateAndPad("Name", colWidth));
                } else {
                    lpw.print("-".repeat(colWidth));
                }
                lpw.print(" | ");

                for (Iterator<Course> it = scenario.findAllCourses().iterator(); it.hasNext();) {
                    Course course = it.next();
                    if (hdrRow == 0) {
                        lpw.print(truncateAndPad(course.getName(), colWidth));
                    } else {
                        lpw.print("-".repeat(colWidth));
                    }
                    if (it.hasNext()) {
                        lpw.print(" | ");
                    }
                }

                lpw.print(" | ");
                if (hdrRow == 0) {
                    lpw.print(truncateAndPad("# People", colWidth));
                } else {
                    lpw.print("-".repeat(colWidth));
                }

                lpw.print(" | ");
                if (hdrRow == 0) {
                    lpw.print(truncateAndPad("# Tables", colWidth));
                } else {
                    lpw.print("-".repeat(colWidth));
                }

                lpw.println(" |");
            }

            /* then the rows */
            for (Iterator<Person> pit = scenario.findAllPeople().iterator(); pit.hasNext();) {
                Person person = pit.next();
                lpw.print("| ");
                lpw.print(truncateAndPad(person.getName(), colWidth));
                lpw.print(" | ");
                for (Iterator<Course> cit = scenario.findAllCourses().iterator(); cit.hasNext();) {
                    Course course = cit.next();
                    // lpw.print(" ");
                    solution.findTableByPersonAndCourse(person, course).ifPresentOrElse(table -> {
                        lpw.print(truncateAndPad(table.getName(), colWidth));
                    }, () -> {
                        lpw.print("-".repeat(colWidth));
                    });
                    lpw.print(" | ");
                }
                var peopleMet = solution.countAllDistinctPeopleMetByPerson(person);
                lpw.print(truncateAndPad(String.valueOf(peopleMet), colWidth));
                lpw.print(" | ");
                var tablesSatOn = solution.countAllDistinctTablesByPerson(person);
                lpw.print(truncateAndPad(String.valueOf(tablesSatOn), colWidth));
                lpw.print(" |");
                lpw.println();
            }

            /* and the footer */
            lpw.print("|");
            lpw.print("-".repeat((colWidth + 2) + (numberOfCourses + 2) * (colWidth + 3)));

            lpw.println("|");
        }
    }

    private static final class LoggingPrintWriter extends PrintWriter {
        LoggingPrintWriter() {
            super(new StringWriter());
        }

        public void println(String str) {
            super.print(str);
            println();
        }

        public void println() {
            flush();
            LOG.info(out.toString());
            ((StringWriter) out).getBuffer().setLength(0);
        }
    }

    private static final String truncateAndPad(String str, int width) {
        return StringUtils.rightPad(StringUtils.truncate(str, width), width);
    }

    /*
     * Simple random filling of tables.
     * 
     * 1. put one host on each table
     * 
     * 2. fill up the remaining spaces from a shuffled list of people
     */
    protected Solution createSolution(Scenario scenario, Random random) {

        long startTime = System.nanoTime();

        Solution solution = new TripleListSolution();

        var tables = new ArrayList<>(IterableUtils.toList(scenario.findAllTables()));
        for (var course : scenario.findAllCourses()) {

            /* add a host to each table */
            int tableNumber = 0;
            for (var host : scenario.findAllPeopleByHost(true)) {
                solution.addSeating(host, course, tables.get(tableNumber++));
            }

            /* shuffle the remaining people */
            var shuffledNonHosts = new ArrayList<>(IterableUtils.toList(scenario.findAllPeopleByHost(false)));
            Collections.shuffle(shuffledNonHosts, random);

            /* and then add them to the tables one at a time */
            tableNumber = 0;
            for (var person : shuffledNonHosts) {
                solution.addSeating(person, course, tables.get(tableNumber));
                tableNumber++;
                if (tableNumber >= numberOfTables) {
                    tableNumber = 0;
                }
            }
        }

        long endTime = System.nanoTime();
        LOG.debug("Solution generated in {} us", TimeUnit.NANOSECONDS.toMicros(endTime - startTime));

        return solution;
    }

    protected double scoreSolution(Scenario scenario, Solution solution) {

        long startTime = System.nanoTime();

        boolean valid = true;

        /* make sure one and only one host per table for each course */
        for (var tables : scenario.findAllTables()) {
            for (var courses : scenario.findAllCourses()) {
                long hosts = solution.countAllPeopleByHostAndCourseAndTable(true, courses, tables);
                if (hosts != 1) {
                    valid = false;
                    break;
                }
            }
        }

        /* make sure host only sits on one table */
        for (var host : scenario.findAllPeopleByHost(true)) {
            var numTables = solution.countAllDistinctTablesByPerson(host);
            if (numTables != 1) {
                valid = false;
                break;
            }
        }

        long numberOfPeople = scenario.countAllPeople();
        long maxPeoplePerTable = (int) Math.ceil((double) numberOfPeople / numberOfTables);
        long mostPeopleAPersonCanMeet = numberOfCourses * (maxPeoplePerTable - 1);
        long mostNumberOfDifferentTablesAPersonCanSitAt = Math.min(numberOfTables, numberOfCourses);

        /* how many different people does each person get to sit with */
        var personToDifferentPeopleScore = new HashMap<Person, Double>();
        for (var person : scenario.findAllPeople()) {
            var peopleMet = solution.countAllDistinctPeopleMetByPerson(person);
            double score = peopleMet / (double) mostPeopleAPersonCanMeet;
            personToDifferentPeopleScore.put(person, score);
        }

        /* how many different tables does each person get to sit on */
        var personToDifferentTableScore = new HashMap<Person, Double>();
        for (var person : scenario.findAllPeopleByHost(false)) {
            var numTables = solution.countAllDistinctTablesByPerson(person);
            double score = numTables / (double) mostNumberOfDifferentTablesAPersonCanSitAt;
            personToDifferentTableScore.put(person, score);
        }

        double avgPersonToDifferentPeopleScore = personToDifferentPeopleScore.values().stream()
                .collect(Collectors.averagingDouble(v -> v));
        double avgPersonToDifferentTableScore = personToDifferentTableScore.values().stream()
                .collect(Collectors.averagingDouble(v -> v));
        /* combine scores with weightings */
        double solutionScore = valid ? avgPersonToDifferentPeopleScore * 0.5D + avgPersonToDifferentTableScore * 0.5D
                : 0.0;

        long endTime = System.nanoTime();

        LOG.debug("Solution scored in {} us", TimeUnit.NANOSECONDS.toMicros(endTime - startTime));

        return solutionScore;
    }

    /**
     * Repeatedly generate random solutions, score them and keep the best one
     * 
     * @param scenario
     *            the scenario to solve
     * @param initialSolution
     *            an initial solution to start with
     * @return the best solution found or null if no valid solution found
     */
    public Pair<Solution, Double> bestRandomGuessStrategy(Scenario scenario, Solution initialSolution) {

        long startTime = System.nanoTime();

        /* random but repeatable */
        var random = (seed == 0) ? new Random() : new Random(seed);

        Solution bestSolution = initialSolution;
        double bestScore = (initialSolution == null) ? 0 : scoreSolution(scenario, initialSolution);

        LOG.info(String.format("Initial solution score %f", bestScore));

        for (int iteration = 0; iteration < iterations; iteration++) {

            Solution solution = createSolution(scenario, random);

            double score = scoreSolution(scenario, solution);

            LOG.info(String.format("Iteration %d solution score %f", iteration, score));

            if (score > bestScore) {
                bestScore = score;
                bestSolution = solution;
            }
        }

        long endTime = System.nanoTime();
        LOG.info("bestRandomGuessStrategy generated {} solutions in {} ms with a best score of {}", iterations,
                TimeUnit.NANOSECONDS.toMillis(endTime - startTime), bestScore);

        return new ImmutablePair<Solution, Double>(bestSolution, bestScore);
    }

    /**
     * Generate a random solution, them tweak it, score it and repeat
     * 
     * @param scenario
     *            the scenario to solve
     * @param initialSolution
     *            a solution to start with to see if it can be improved
     * @return the best solution found or null if no valid solution found
     */
    public Pair<Solution, Double> tweakAndRepeatStrategy(Scenario scenario, Solution initialSolution) {

        long startTime = System.nanoTime();

        /* random but repeatable */
        var random = (seed == 0) ? new Random() : new Random(seed);

        Solution solution = (initialSolution == null) ? createSolution(scenario, random) : initialSolution;

        double score = scoreSolution(scenario, solution);
        LOG.info(String.format("Initial solution score %f", score));

        List<Course> courses = IterableUtils.toList(scenario.findAllCourses());
        List<Person> people = IterableUtils.toList(scenario.findAllPeople());

        for (int iteration = 0; iteration < iterations; iteration++) {

            Solution prevSolution = solution;
            solution = solution.copy();
            double prevScore = score;

            Course course = courses.get(random.nextInt(courses.size()));
            Person person1 = people.get(random.nextInt(people.size()));
            Person person2 = people.get(random.nextInt(people.size()));

            if (!person1.isHost() && !person2.isHost() && !person1.equals(person2)) {
                solution.swapPeopleOnCourse(course, person1, person2);
                score = scoreSolution(scenario, solution);

                if (score <= prevScore) {
                    LOG.debug(String.format("Iteration %d solution score %f, worse than current score %f so reverting",
                            iteration, score, prevScore));
                    solution = prevSolution;
                    score = prevScore;
                } else {
                    LOG.debug(String.format("Iteration %d solution score %f better than current score %f so keeping",
                            iteration, score, prevScore));
                }

            } else {
                LOG.debug(String.format("Skipping iteration %d as can't swap host or same person", iteration));
            }
        }

        long endTime = System.nanoTime();
        LOG.info("tweakAndRepeatStrategy generated {} solutions in {} ms with a best score of {}", iterations,
                TimeUnit.NANOSECONDS.toMillis(endTime - startTime), score);

        return new ImmutablePair<Solution, Double>(solution, score);
    }

}
