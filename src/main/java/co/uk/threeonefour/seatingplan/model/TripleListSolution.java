package co.uk.threeonefour.seatingplan.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

public class TripleListSolution implements Solution {

    private final List<Triple<Person, Course, Table>> triples;

    public TripleListSolution() {
        this.triples = new ArrayList<>();
    }

    private TripleListSolution(TripleListSolution orig) {
        this.triples = new ArrayList<>(orig.triples);
    }
    
    @Override
    public Solution copy() {
        return new TripleListSolution(this);
    }
    
    @Override
    public void addSeating(Person person, Course course, Table table) {
        triples.add(ImmutableTriple.of(person, course, table));
    }

    @Override
    public Optional<Table> findTableByPersonAndCourse(Person person, Course course) {
        return triples.stream().filter(triple -> triple.getLeft().equals(person) && triple.getMiddle().equals(course))
                .map(triple -> triple.getRight()).findFirst();
    }

    @Override
    public long countAllPeopleByHostAndCourseAndTable(boolean host, Course course, Table table) {
        return triples.stream().filter(triple -> triple.getLeft().isHost() == host && triple.getMiddle().equals(course)
                && triple.getRight().equals(table)).count();
    }

    @Override
    public long countAllDistinctTablesByPerson(Person person) {
        return triples.stream().filter(triple -> triple.getLeft().equals(person)).map(triple -> triple.getRight())
                .collect(Collectors.toSet()).size();
    }

    @Override
    public Iterable<Person> findAllDistinctPeopleMetByPerson(Person person) {
        Set<Person> peopleMet = new HashSet<>();
        triples.stream().filter(triple -> triple.getLeft().equals(person)).forEach(triple -> {
            peopleMet
                    .addAll(IterableUtils.toList(findAllPeopleByCourseAndTable(triple.getMiddle(), triple.getRight())));
        });
        peopleMet.remove(person);
        return peopleMet;
    }

    @Override
    public long countAllDistinctPeopleMetByPerson(Person person) {
        Set<Person> peopleMet = new HashSet<>();
        triples.stream().filter(triple -> triple.getLeft().equals(person)).forEach(triple -> {
            peopleMet
                    .addAll(IterableUtils.toList(findAllPeopleByCourseAndTable(triple.getMiddle(), triple.getRight())));
        });
        peopleMet.remove(person);
        return peopleMet.size();
    }

    @Override
    public Iterable<Person> findAllPeopleByCourseAndTable(Course course, Table table) {
        return triples.stream().filter(triple -> triple.getMiddle().equals(course) && triple.getRight().equals(table))
                .map(triple -> triple.getLeft()).collect(Collectors.toList());
    }

    @Override
    public Iterable<Table> findAllDistinctTablesByPerson(Person person) {
        Set<Table> tables = triples.stream().filter(triple -> triple.getLeft().equals(person))
                .map(triple -> triple.getRight()).collect(Collectors.toSet());
        return tables;
    }

    @Override
    public void swapPeopleOnCourse(Course course, Person person1, Person person2) {
        Triple<Person, Course, Table> triple1 = triples.stream()
                .filter(triple -> triple.getLeft().equals(person1) && triple.getMiddle().equals(course)).findFirst()
                .orElse(null);
        Triple<Person, Course, Table> triple2 = triples.stream()
                .filter(triple -> triple.getLeft().equals(person2) && triple.getMiddle().equals(course)).findFirst()
                .orElse(null);
        if (triple1 != null && triple2 != null) {
            /* remove old entries */
            triples.remove(triple1);
            triples.remove(triple2);
            /* add new entries with person swapped */
            triples.add(ImmutableTriple.of(triple2.getLeft(), triple1.getMiddle(), triple1.getRight()));
            triples.add(ImmutableTriple.of(triple1.getLeft(), triple2.getMiddle(), triple2.getRight()));
        }
    }
}
