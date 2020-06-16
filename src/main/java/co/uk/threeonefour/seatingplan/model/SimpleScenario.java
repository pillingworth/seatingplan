package co.uk.threeonefour.seatingplan.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleScenario implements Scenario {

    private final List<Course> courses;

    private final List<Table> tables;

    private final List<Person> people;

    public SimpleScenario() {
        this.courses = new ArrayList<>();
        this.tables = new ArrayList<>();
        this.people = new ArrayList<>();
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
        return Collections.unmodifiableList(people.stream().filter(p -> p.isHost() == host).collect(Collectors.toList()));
    }

    @Override
    public long countAllPeopleByHost(boolean host) {
        return people.stream().filter(p -> p.isHost() == host).count();
    }
}
