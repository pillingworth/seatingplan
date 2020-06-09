package co.uk.threeonefour.seatingplan.model;

import java.util.Optional;

public interface Model {

    Model copy();

    void addCourse(Course course);

    long countAllCourses();

    Iterable<Course> findAllCourses();

    void addTable(Table table);

    long countAllTables();

    Iterable<Table> findAllTables();

    void addPerson(Person person);

    long countAllPeople();

    Iterable<Person> findAllPeople();

    Iterable<Person> findAllPeopleByHost(boolean host);

    long countPeopleByHost(boolean host);

    void addSeating(Person host, Course course, Table table);

    void deleteAllSeating();

    Optional<Table> findTableByPersonAndCourse(Person person, Course course);

    long countAllPeopleByHostAndCourseAndTable(boolean host, Course course, Table table);

    long countAllDistinctTablesByPerson(Person person);

    Iterable<Person> findAllDistinctPeopleMetByPerson(Person person);
    
    long countAllDistinctPeopleMetByPerson(Person person);
    
    Iterable<Person> findAllPeopleByCourseAndTable(Course course, Table table);

    Iterable<Table> findAllDistinctTablesByPerson(Person person);
    
    void swapPeopleOnCourse(Course course, Person person1, Person person2);
}
