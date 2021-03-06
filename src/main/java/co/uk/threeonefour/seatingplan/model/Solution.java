package co.uk.threeonefour.seatingplan.model;

import java.util.Optional;

public interface Solution {

    Solution copy();
    
    void addSeating(Person host, Course course, Table table);

    Optional<Table> findTableByPersonAndCourse(Person person, Course course);

    long countAllPeopleByHostAndCourseAndTable(boolean host, Course course, Table table);

    long countAllDistinctTablesByPerson(Person person);

    Iterable<Person> findAllDistinctPeopleMetByPerson(Person person);
    
    long countAllDistinctPeopleMetByPerson(Person person);
    
    Iterable<Person> findAllPeopleByCourseAndTable(Course course, Table table);

    Optional<Person> findPersonByTableAndHost(Table table, boolean host);
    
    long countAllPeopleByCourseAndTable(Course course, Table table);

    Iterable<Table> findAllDistinctTablesByPerson(Person person);
    
    void swapPeopleOnCourse(Course course, Person person1, Person person2);

    void movePersonOnCourseToTable(Person person, Course course, Table table);
}
