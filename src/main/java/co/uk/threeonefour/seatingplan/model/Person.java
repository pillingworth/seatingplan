package co.uk.threeonefour.seatingplan.model;

import java.util.Objects;

public class Person {

    private final int id;
    private final String name;
    private final boolean host;

    public Person(int it, String name, boolean host) {
        this.id = it;
        this.name = name;
        this.host = host;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isHost() {
        return host;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Person other = (Person) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return "Person [id=" + id + ", name=" + name + ", host=" + host + "]";
    }
}
