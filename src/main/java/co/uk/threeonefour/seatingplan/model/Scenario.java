package co.uk.threeonefour.seatingplan.model;

public interface Scenario {

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

    long countAllPeopleByHost(boolean host);
}
