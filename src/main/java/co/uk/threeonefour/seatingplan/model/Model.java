package co.uk.threeonefour.seatingplan.model;

import java.util.Optional;

public interface Model {

    Model copy();

    void addCourse(Course course);

    long countCourses();

    Iterable<Course> findAllCourses();

    void addTable(Table table);

    long countTables();

    Iterable<Table> findAllTables();

    void addPerson(Person person);

    long countPeople();

    Iterable<Person> findAllPeople();

    Iterable<Person> findAllPeopleByHost(boolean host);

    long countPeopleByHost(boolean host);

    void addSeating(Person host, Course course, Table table);

    void clearSeating();

    Optional<Table> findSeatingTableByPersonAndCourse(Person person, Course course);

    long seatingCountPeopleByHostAndCourseAndTable(boolean host, Course course, Table table);

    long countDistinctTablesByPerson(Person host);

    Iterable<Person> seatingFindAllDistinctPeopleMetByPerson(Person person);
    
    long seatingCountAllDistinctPeopleMetByPerson(Person person);
    
    Iterable<Person> seatingFindAllPeopleByCourseAndTable(Course course, Table table);

    Iterable<Table> seatingFindAllDistinctTablesByPerson(Person person);
    
    long seatingCountAllDistinctTablesByPerson(Person person);
    
    void swap(Course course, Person person1, Person person2);
}
