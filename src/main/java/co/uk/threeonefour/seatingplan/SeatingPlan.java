package co.uk.threeonefour.seatingplan;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Option(names = { "-s",
            "--seed" }, description = "Seed for random number generation.", paramLabel = "<seed>", defaultValue = "0")
    private long seed;

    @Option(names = { "-pf",
            "--peoplefile" }, description = "File of people to include. One person per line. Use # to 'comment' someone out. Use ', host' to indicate they are a host.", paramLabel = "<peoplefile>", required = true)
    private Path peopleFilePath;

    @Option(names = { "-i",
            "--iterations" }, description = "How many iterations should be tried.", paramLabel = "<iterations>", defaultValue = "20000")
    private int iterations;

    @Option(names = { "-st",
            "--strategy" }, description = "Which strategies to use. Options are random|swap", paramLabel = "<strategies>", defaultValue = "swap")
    private List<String> strategies;

    @Option(names = { "-dpw",
            "--differentpeopleweighting" }, description = "Weighting to be applied to the number of different people score. Weightings should ideally add up to 1.0.", paramLabel = "<differentpeopleweighting>", defaultValue = "0.4")
    private double differentPeopleWeighting;

    @Option(names = { "-dtw",
            "--differenttablesweighting" }, description = "Weighting to be applied to the number of different tables score. Weightings should ideally add up to 1.0.", paramLabel = "<differenttablesweighting>", defaultValue = "0.6")
    private double differentTablesWeighting;

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

        /* random but repeatable */
        Random random = (seed == 0) ? new Random() : new Random(seed);

        Pair<Solution, Double> solutionScore;

        /* Solution strategy #1 */
        if (strategies.contains("random")) {
            solutionScore = bestRandomGuessStrategy(scenario, null, random);
            /* print the solution found */
            if (solutionScore.getLeft() != null) {
                printModel(scenario, solutionScore.getLeft(), solutionScore.getRight());
            } else {
                LOG.error("No valid solution found");
            }
        }

        /* Solution strategy #2 */
        if (strategies.contains("swap")) {
            solutionScore = swapAndRepeatStrategy(scenario, null, random);
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

        int numberOfHosts = 0;
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
                    if (host) {
                        numberOfHosts++;
                    }
                }
            }
        }

        /* create a list of Courses */
        for (int i = 0; i < numberOfCourses; i++) {
            int id = i + 1;
            scenario.addCourse(new Course(id, "Course " + id));
        }

        /* create a list of Tables */
        for (int i = 0; i < numberOfHosts; i++) {
            int id = i + 1;
            scenario.addTable(new Table(id, "Table " + id));
        }

        return scenario;
    }

    protected void printModel(Scenario scenario, Solution solution, double score) {

        try (PrintWriter pw = new PrintWriter(System.out)) {

            pw.println();
            pw.println(String.format("Solution score %f", score));

            /* first the header rows */
            int firstColWidth = 20;
            int otherColWidth = 12;
            pw.println("");
            for (int hdrRow = 0; hdrRow < 2; hdrRow++) {

                pw.print("| ");
                if (hdrRow == 0) {
                    pw.print(truncateAndPad("Name", firstColWidth));
                } else {
                    pw.print(StringUtils.repeat('-', firstColWidth));
                }
                pw.print(" | ");

                for (Iterator<Course> it = scenario.findAllCourses().iterator(); it.hasNext();) {
                    Course course = it.next();
                    if (hdrRow == 0) {
                        pw.print(truncateAndPad(course.getName(), otherColWidth));
                    } else {
                        pw.print(StringUtils.repeat('-', otherColWidth));
                    }
                    if (it.hasNext()) {
                        pw.print(" | ");
                    }
                }

                pw.print(" | ");
                if (hdrRow == 0) {
                    pw.print(truncateAndPad("# People", otherColWidth));
                } else {
                    pw.print(StringUtils.repeat('-', otherColWidth));
                }

                pw.print(" | ");
                if (hdrRow == 0) {
                    pw.print(truncateAndPad("# Tables", otherColWidth));
                } else {
                    pw.print(StringUtils.repeat('-', otherColWidth));
                }

                pw.println(" |");
            }

            /* then the rows */
            for (Iterator<Person> pit = scenario.findAllPeople().iterator(); pit.hasNext();) {
                Person person = pit.next();
                pw.print("| ");
                if (person.isHost()) {
                    pw.print("**");
                    pw.print("(h) ");
                    pw.print(truncateAndPad(person.getName(), firstColWidth - 8));
                    pw.print("**");
                } else {
                    pw.print(truncateAndPad(person.getName(), firstColWidth));
                }
                pw.print(" | ");
                for (Iterator<Course> cit = scenario.findAllCourses().iterator(); cit.hasNext();) {
                    Course course = cit.next();
                    // lpw.print(" ");
                    Optional<Table> table = solution.findTableByPersonAndCourse(person, course);
                    if (table.isPresent()) {
                        Optional<Person> host = solution.findPersonByTableAndHost(table.get(), true);
                        if (host.isPresent()) {
                            pw.print(truncateAndPad(host.get().getName(), otherColWidth));
                        } else {
                            pw.print(truncateAndPad(table.get().getName(), otherColWidth));
                        }
                    } else {
                        pw.print(StringUtils.repeat('-', otherColWidth));
                    }
                    pw.print(" | ");
                }
                long peopleMet = solution.countAllDistinctPeopleMetByPerson(person);
                pw.print(truncateAndPad(String.valueOf(peopleMet), otherColWidth));
                pw.print(" | ");
                long tablesSatOn = solution.countAllDistinctTablesByPerson(person);
                pw.print(truncateAndPad(String.valueOf(tablesSatOn), otherColWidth));
                pw.print(" |");
                pw.println();
            }

            /* and the footer */
            pw.println();

            /* and print all hosts and people they sit with */
            for (Person host : scenario.findAllPeopleByHost(true)) {
                pw.print("* ");
                pw.println(host.getName());
                for (Person person : solution.findAllDistinctPeopleMetByPerson(host)) {
                    pw.print("  * ");
                    pw.println(person.getName());
                }
            }
            pw.println();
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

        List<Table> tables = new ArrayList<>(IterableUtils.toList(scenario.findAllTables()));
        for (Course course : scenario.findAllCourses()) {

            /* add a host to each table */
            int tableNumber = 0;
            for (Person host : scenario.findAllPeopleByHost(true)) {
                solution.addSeating(host, course, tables.get(tableNumber++));
            }

            /* shuffle the remaining people */
            List<Person> shuffledNonHosts = new ArrayList<>(IterableUtils.toList(scenario.findAllPeopleByHost(false)));
            Collections.shuffle(shuffledNonHosts, random);

            /* and then add them to the tables one at a time */
            long numberOfTables = scenario.countAllTables();
            tableNumber = 0;
            for (Person person : shuffledNonHosts) {
                solution.addSeating(person, course, tables.get(tableNumber));
                tableNumber++;
                if (tableNumber >= numberOfTables) {
                    tableNumber = 0;
                }
            }
        }

        long endTime = System.nanoTime();
        LOG.trace("Solution generated in {} us", TimeUnit.NANOSECONDS.toMicros(endTime - startTime));

        return solution;
    }

    protected double scoreSolution(Scenario scenario, Solution solution) {

        long startTime = System.nanoTime();

        boolean valid = true;

        /* make sure one and only one host per table for each course */
        for (Table tables : scenario.findAllTables()) {
            for (Course courses : scenario.findAllCourses()) {
                long hosts = solution.countAllPeopleByHostAndCourseAndTable(true, courses, tables);
                if (hosts != 1) {
                    valid = false;
                    break;
                }
            }
        }

        /* make sure host only sits on one table */
        for (Person host : scenario.findAllPeopleByHost(true)) {
            long numTables = solution.countAllDistinctTablesByPerson(host);
            if (numTables != 1) {
                valid = false;
                break;
            }
        }

        long numberOfTables = scenario.countAllTables();
        long numberOfPeople = scenario.countAllPeople();
        long maxPeoplePerTable = (int) Math.ceil((double) numberOfPeople / numberOfTables);
        long mostPeopleAPersonCanMeet = numberOfCourses * (maxPeoplePerTable - 1);
        long mostNumberOfDifferentTablesAPersonCanSitAt = Math.min(numberOfTables, numberOfCourses);

        /* how many different people does each person get to sit with */
        Map<Person, Double> personToDifferentPeopleScore = new HashMap<>();
        for (Person person : scenario.findAllPeople()) {
            long peopleMet = solution.countAllDistinctPeopleMetByPerson(person);
            double score = peopleMet / (double) mostPeopleAPersonCanMeet;
            personToDifferentPeopleScore.put(person, score);
        }

        /* how many different tables does each person get to sit on */
        Map<Person, Double> personToDifferentTableScore = new HashMap<>();
        for (Person person : scenario.findAllPeopleByHost(false)) {
            long numTables = solution.countAllDistinctTablesByPerson(person);
            double score = numTables / (double) mostNumberOfDifferentTablesAPersonCanSitAt;
            personToDifferentTableScore.put(person, score);
        }

        double avgPersonToDifferentPeopleScore = personToDifferentPeopleScore.values().stream()
                .collect(Collectors.averagingDouble(v -> v));
        double avgPersonToDifferentTableScore = personToDifferentTableScore.values().stream()
                .collect(Collectors.averagingDouble(v -> v));
        /* combine scores with weightings */
        double solutionScore = valid
                ? avgPersonToDifferentPeopleScore * differentPeopleWeighting
                        + avgPersonToDifferentTableScore * differentTablesWeighting
                : 0.0;

        long endTime = System.nanoTime();

        LOG.trace("Solution scored in {} us", TimeUnit.NANOSECONDS.toMicros(endTime - startTime));

        return solutionScore;
    }

    /**
     * Repeatedly generate random solutions, score them and keep the best one
     * 
     * @param scenario
     *            the scenario to solve
     * @param initialSolution
     *            an initial solution to start with
     * @param random
     *            for repeatable random numbers
     * @return the best solution found or null if no valid solution found
     */
    public Pair<Solution, Double> bestRandomGuessStrategy(Scenario scenario, Solution initialSolution, Random random) {

        LOG.debug("Starting bestRandomGuessStrategy");

        long startTime = System.nanoTime();

        Solution bestSolution = initialSolution;
        double initialScore = (initialSolution == null) ? 0 : scoreSolution(scenario, initialSolution);
        double bestScore = initialScore;

        LOG.debug(String.format("Initial solution score %f", bestScore));

        for (int iteration = 0; iteration < iterations; iteration++) {

            Solution solution = createSolution(scenario, random);

            double score = scoreSolution(scenario, solution);

            LOG.debug(String.format("Iteration %d solution score %f", iteration, score));

            if (score > bestScore) {
                bestScore = score;
                bestSolution = solution;
            }
        }

        long endTime = System.nanoTime();
        LOG.debug(
                "bestRandomGuessStrategy generated {} solutions in {} ms from an initial score of {} producing a best score of {}",
                iterations, TimeUnit.NANOSECONDS.toMillis(endTime - startTime), initialScore, bestScore);

        return new ImmutablePair<Solution, Double>(bestSolution, bestScore);
    }

    /**
     * Generate a random solution, them tweak it, score it and repeat
     * 
     * @param scenario
     *            the scenario to solve
     * @param initialSolution
     *            a solution to start with to see if it can be improved
     * @param random
     *            for repeatable random numbers
     * @return the best solution found or null if no valid solution found
     */
    public Pair<Solution, Double> swapAndRepeatStrategy(Scenario scenario, Solution initialSolution, Random random) {

        LOG.debug("Starting swapAndRepeatStrategy");

        long startTime = System.nanoTime();

        Solution solution = (initialSolution == null) ? createSolution(scenario, random) : initialSolution;

        double score = scoreSolution(scenario, solution);

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

                Table table1 = solution.findTableByPersonAndCourse(person1, course).get();
                Table table2 = solution.findTableByPersonAndCourse(person2, course).get();
                boolean canSwap = !table1.equals(table2);

                if (canSwap) {

                    /* decide is we are going to swap two people or just move a single person */
                    long peopleOnTable1 = solution.countAllPeopleByCourseAndTable(course, table1);
                    long peopleOnTable2 = solution.countAllPeopleByCourseAndTable(course, table2);

                    boolean canMove = (peopleOnTable1 != peopleOnTable2);

                    if (canMove && random.nextBoolean()) {
                        LOG.debug("moving...");
                        if (peopleOnTable1 > peopleOnTable2) {
                            solution.movePersonOnCourseToTable(person1, course, table2);
                        } else {
                            solution.movePersonOnCourseToTable(person2, course, table1);
                        }
                    } else {
                        LOG.debug("swapping...");
                        solution.swapPeopleOnCourse(course, person1, person2);
                    }

                    score = scoreSolution(scenario, solution);

                    if (score <= prevScore) {
                        LOG.debug(String.format(
                                "Iteration %d solution score %f, worse than current score %f so reverting", iteration,
                                score, prevScore));
                        solution = prevSolution;
                        score = prevScore;
                    } else {
                        LOG.debug(
                                String.format("Iteration %d solution score %f better than current score %f so keeping",
                                        iteration, score, prevScore));
                    }
                } else {
                    LOG.debug(String.format("Skipping iteration %d as can't swap person to same table", iteration));
                }

            } else {
                LOG.debug(String.format("Skipping iteration %d as can't swap host or same person", iteration));
            }
        }

        long endTime = System.nanoTime();
        LOG.debug("swapAndRepeatStrategy generated {} solutions in {} ms with a best score of {}", iterations,
                TimeUnit.NANOSECONDS.toMillis(endTime - startTime), score);

        return new ImmutablePair<Solution, Double>(solution, score);
    }
}
