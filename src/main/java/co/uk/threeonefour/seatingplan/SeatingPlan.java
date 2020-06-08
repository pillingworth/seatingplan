package co.uk.threeonefour.seatingplan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.uk.threeonefour.seatingplan.model.Course;
import co.uk.threeonefour.seatingplan.model.Model;
import co.uk.threeonefour.seatingplan.model.Person;
import co.uk.threeonefour.seatingplan.model.Table;
import co.uk.threeonefour.seatingplan.model.TripleModel;
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
            "--iterations" }, description = "How many iterations should be tried.", paramLabel = "<iterations>", defaultValue = "500")
    private int iterations;

    @Override
    public void run() {

        Model model = new TripleModel();

        /* read in the list of people */
        if (!Files.exists(peopleFilePath) || Files.isDirectory(peopleFilePath)) {
            LOG.error("Unable to open {} as it either could not be found or is not a file.", peopleFilePath);
            return;
        }

        try (Scanner scanner = new Scanner(peopleFilePath)) {
            int i = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.startsWith("#")) {
                    int id = (i + 1);
                    String name = StringUtils.substringBeforeLast(line, ",").trim();
                    boolean host = "host".equalsIgnoreCase(StringUtils.substringAfterLast(line, ",").trim());
                    model.addPerson(new Person(id, name, host));
                    i++;
                }
            }
        } catch (IOException e) {
            LOG.error("Unable to read the people file {}", peopleFilePath, e);
            return;
        }

        /* create a list of Courses */
        for (var i = 0; i < numberOfCourses; i++) {
            int id = i + 1;
            model.addCourse(new Course(id, "Course " + id));
        }

        /* create a list of Tables */
        for (var i = 0; i < numberOfTables; i++) {
            int id = i + 1;
            model.addTable(new Table(id, "Table " + id));
        }

        /* use repeatable random numbers if required */
        var random = (seed == 0) ? new Random() : new Random(seed);

        long numberOfPeople = model.countPeople();
        long numberOfHosts = model.countPeopleByHost(true);
        long maxPeoplePerTable = (int) Math.ceil((double) numberOfPeople / numberOfTables);
        long mostPeopleAPersonCanMeet = numberOfCourses * (maxPeoplePerTable - 1);
        long mostNumberOfDifferentTablesAPersonCanSitAt = Math.min(numberOfTables, numberOfCourses);

        System.out.println(
                String.format("There are %d people on %d tables, so that's upto %d people per table with %d hosts",
                        numberOfPeople, numberOfTables, maxPeoplePerTable, numberOfHosts));

        Model bestModel = null;
        double bestScore = 0;
        int bestIteration = -1;

        for (int iteration = 0; iteration < iterations; iteration++) {

            model.clearSeating();

            /*
             * method 1:
             * 
             * + put one host on each table
             * 
             * + fill up the remaining spaces from a shuffled list of people
             */
            {
                var tables = new ArrayList<>(IterableUtils.toList(model.findAllTables()));
                for (var course : model.findAllCourses()) {

                    /* add a host to each table */
                    int tableNumber = 0;
                    for (var host : model.findAllPeopleByHost(true)) {
                        model.addSeating(host, course, tables.get(tableNumber++));
                    }

                    // shuffle the remaining people
                    var shuffledNonHosts = new ArrayList<>(IterableUtils.toList(model.findAllPeopleByHost(false)));
                    Collections.shuffle(shuffledNonHosts, random);

                    /* and then add them to the tables one at a time */
                    tableNumber = 0;
                    for (var person : shuffledNonHosts) {
                        model.addSeating(person, course, tables.get(tableNumber));
                        tableNumber++;
                        if (tableNumber >= numberOfTables) {
                            tableNumber = 0;
                        }
                    }
                }
            }

            boolean valid = true;

            /* make sure one and only one host per table for each course */
            for (var tables : model.findAllTables()) {
                for (var courses : model.findAllCourses()) {
                    long hosts = model.seatingCountPeopleByHostAndCourseAndTable(true, courses, tables);
                    if (hosts != 1) {
                        valid = false;
                        break;
                    }
                }
            }

            /* make sure host only sits on one table */
            for (var host : model.findAllPeopleByHost(true)) {
                var numTables = model.countDistinctTablesByPerson(host);
                if (numTables != 1) {
                    valid = false;
                    break;
                }
            }

            /* how many different people does each person get to sit with */
            var personToDifferentPeopleScore = new HashMap<Person, Double>();
            for (var person : model.findAllPeople()) {
                var peopleMet = model.seatingCountAllDistinctPeopleMetByPerson(person);
                double score = peopleMet / (double) mostPeopleAPersonCanMeet;
                personToDifferentPeopleScore.put(person, score);
            }

            /* how many different tables does each person get to sit on */
            var personToDifferentTableScore = new HashMap<Person, Double>();
            for (var person : model.findAllPeopleByHost(false)) {
                var numTables = model.seatingCountAllDistinctTablesByPerson(person);
                double score = numTables / (double) mostNumberOfDifferentTablesAPersonCanSitAt;
                personToDifferentTableScore.put(person, score);
//                if (numTables != mostNumberOfDifferentTablesAPersonCanSitAt) {
//                    valid = false;
//                }
            }

            double avgPersonToDifferentPeopleScore = personToDifferentPeopleScore.values().stream()
                    .collect(Collectors.averagingDouble(v -> v));
            double avgPersonToDifferentTableScore = personToDifferentTableScore.values().stream()
                    .collect(Collectors.averagingDouble(v -> v));
            double solutionScore = valid ? (avgPersonToDifferentPeopleScore + avgPersonToDifferentTableScore) / 2 : 0.0;

            System.out.println(String.format("Iteration %d solution score %f", iteration, solutionScore));

            if (solutionScore > bestScore) {
                if (bestIteration >= 0) {
                    System.out.println(String.format("  better than previous of %f from iteration %d so updating",
                            bestScore, bestIteration));
                }
                bestIteration = iteration;
                bestScore = solutionScore;
                bestModel = model.copy();
            }
        }

        /*
         * Print out the seating plan by person and table
         */

        if (bestModel != null) {

            System.out.println(String.format("Best solution from iteration %d with a solution score %f", bestIteration,
                    bestScore));

            /* first the header rows */
            int colWidth = 12;
            System.out.println("");
            for (int hdrRow = 0; hdrRow < 2; hdrRow++) {

                System.out.print("| ");
                if (hdrRow == 0) {
                    System.out.print(truncateAndPad("Name", colWidth));
                } else {
                    System.out.print(repeat('-', colWidth));
                }
                System.out.print(" | ");

                for (Iterator<Course> it = bestModel.findAllCourses().iterator(); it.hasNext();) {
                    Course course = it.next();
                    if (hdrRow == 0) {
                        System.out.print(truncateAndPad(course.getName(), colWidth));
                    } else {
                        System.out.print(repeat('-', colWidth));
                    }
                    if (it.hasNext()) {
                        System.out.print(" | ");
                    }
                }

                System.out.print(" | ");
                if (hdrRow == 0) {
                    System.out.print(truncateAndPad("# People", colWidth));
                } else {
                    System.out.print(repeat('-', colWidth));
                }

                System.out.print(" | ");
                if (hdrRow == 0) {
                    System.out.print(truncateAndPad("# Tables", colWidth));
                } else {
                    System.out.print(repeat('-', colWidth));
                }

                System.out.println(" |");
            }

            /* then the rows */
            for (Iterator<Person> pit = bestModel.findAllPeople().iterator(); pit.hasNext();) {
                Person person = pit.next();
                System.out.print("| ");
                System.out.print(truncateAndPad(person.getName(), colWidth));
                System.out.print(" | ");
                for (Iterator<Course> cit = bestModel.findAllCourses().iterator(); cit.hasNext();) {
                    Course course = cit.next();
                    // System.out.print(" ");
                    bestModel.findSeatingTableByPersonAndCourse(person, course).ifPresentOrElse(table -> {
                        System.out.print(truncateAndPad(table.getName(), colWidth));
                    }, () -> {
                        System.out.print(repeat(' ', colWidth));
                    });
                    System.out.print(" | ");
                }
                var peopleMet = bestModel.seatingCountAllDistinctPeopleMetByPerson(person);
                System.out.print(truncateAndPad(String.valueOf(peopleMet), colWidth));
                System.out.print(" | ");
                var tablesSatOn = bestModel.seatingCountAllDistinctTablesByPerson(person);
                System.out.print(truncateAndPad(String.valueOf(tablesSatOn), colWidth));
                System.out.print(" |");
                System.out.println();
            }

            /* and the footer */
            System.out.print("|");
            System.out.print(repeat('-', (colWidth + 2) + (numberOfCourses + 2) * (colWidth + 3)));
            System.out.println("|");
        }

        else {
            System.out.println("No valid solution found");
        }

    }

    private static final String truncateAndPad(String str, int width) {
        return StringUtils.rightPad(StringUtils.truncate(str, width), width);
    }

    private static final String repeat(char ch, int times) {
        return StringUtils.repeat(ch, times);
    }
}
