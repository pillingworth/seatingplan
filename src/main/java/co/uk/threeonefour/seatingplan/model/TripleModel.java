package co.uk.threeonefour.seatingplan.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public class TripleModel implements Model {

    private final List<Course> courses;

    private final List<Table> tables;

    private final List<Person> people;

    private final List<Triple<Person, Course, Table>> seating;

    public TripleModel() {
        this.courses = new ArrayList<>();
        this.tables = new ArrayList<>();
        this.people = new ArrayList<>();
        this.seating = new ArrayList<>();
    }

    protected TripleModel(TripleModel orig) {
        this.courses = new ArrayList<>(orig.courses);
        this.tables = new ArrayList<>(orig.tables);
        this.people = new ArrayList<>(orig.people);
        this.seating = new ArrayList<>(orig.seating);
    }

    @Override
    public TripleModel copy() {
        return new TripleModel(this);
    }

    @Override
    public void addCourse(Course course) {
        courses.add(course);
    }

    @Override
    public long countAllCourses() {
        return courses.size();
    }

    @Override
    public Iterable<Course> findAllCourses() {
        return courses;
    }

    @Override
    public void addTable(Table table) {
        tables.add(table);
    }

    @Override
    public long countAllTables() {
        return tables.size();
    }

    @Override
    public Iterable<Table> findAllTables() {
        return tables;
    }

    @Override
    public void addPerson(Person person) {
        people.add(person);
    }

    @Override
    public long countAllPeople() {
        return people.size();
    }

    @Override
    public Iterable<Person> findAllPeople() {
        return people;
    }

    @Override
    public Iterable<Person> findAllPeopleByHost(boolean host) {
        return people.stream().filter(p -> p.isHost() == host).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public long countPeopleByHost(boolean host) {
        return people.stream().filter(p -> p.isHost() == host).count();
    }

    @Override
    public void addSeating(Person person, Course course, Table table) {
        seating.add(ImmutableTriple.of(person, course, table));
    }

    @Override
    public void deleteAllSeating() {
        seating.clear();
    }

    @Override
    public Optional<Table> findTableByPersonAndCourse(Person person, Course course) {
        return seating.stream().filter(triple -> triple.getLeft().equals(person) && triple.getMiddle().equals(course))
                .map(triple -> triple.getRight()).findFirst();
    }

    @Override
    public long countAllPeopleByHostAndCourseAndTable(boolean host, Course course, Table table) {
        return seating.stream().filter(triple -> triple.getLeft().isHost() == host && triple.getMiddle().equals(course)
                && triple.getRight().equals(table)).count();
    }

    @Override
    public long countAllDistinctTablesByPerson(Person person) {
        return seating.stream().filter(triple -> triple.getLeft().equals(person)).map(triple -> triple.getRight())
                .collect(Collectors.toSet()).size();
    }

    @Override
    public Iterable<Person> findAllDistinctPeopleMetByPerson(Person person) {
        Set<Person> peopleMet = new HashSet<>();
        findAllCourseTablePairsByPerson(person).forEach(courseTable -> {
            peopleMet.addAll(IterableUtils
                    .toList(findAllPeopleByCourseAndTable(courseTable.getLeft(), courseTable.getRight())));
        });
        return peopleMet;
    }

    @Override
    public long countAllDistinctPeopleMetByPerson(Person person) {
        Set<Person> peopleMet = new HashSet<>();
        findAllCourseTablePairsByPerson(person).forEach(courseTable -> {
            peopleMet.addAll(IterableUtils
                    .toList(findAllPeopleByCourseAndTable(courseTable.getLeft(), courseTable.getRight())));
        });
        return peopleMet.size();
    }

    private Stream<Pair<Course, Table>> findAllCourseTablePairsByPerson(Person person) {
        return seating.stream().filter(triple -> triple.getLeft().equals(person))
                .map(triple -> ImmutablePair.of(triple.getMiddle(), triple.getRight()));
    }

    @Override
    public Iterable<Person> findAllPeopleByCourseAndTable(Course course, Table table) {
        return seating.stream().filter(triple -> triple.getMiddle().equals(course) && triple.getRight().equals(table))
                .map(triple -> triple.getLeft()).collect(Collectors.toList());
    }

    @Override
    public Iterable<Table> findAllDistinctTablesByPerson(Person person) {
        Set<Table> tables = seating.stream().filter(triple -> triple.getLeft().equals(person))
                .map(triple -> triple.getRight()).collect(Collectors.toSet());
        return tables;
    }

    @Override
    public void swapPeopleOnCourse(Course course, Person person1, Person person2) {
        Triple<Person, Course, Table> triple1 = seating.stream()
                .filter(triple -> triple.getLeft().equals(person1) && triple.getMiddle().equals(course)).findFirst()
                .orElse(null);
        Triple<Person, Course, Table> triple2 = seating.stream()
                .filter(triple -> triple.getLeft().equals(person2) && triple.getMiddle().equals(course)).findFirst()
                .orElse(null);
        if (triple1 != null && triple2 != null) {
            /* remove old entries */
            seating.remove(triple1);
            seating.remove(triple2);
            /* add new entries with person swapped */
            seating.add(ImmutableTriple.of(triple2.getLeft(), triple1.getMiddle(), triple1.getRight()));
            seating.add(ImmutableTriple.of(triple1.getLeft(), triple2.getMiddle(), triple2.getRight()));
        }
    }
}
