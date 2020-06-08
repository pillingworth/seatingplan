package co.uk.threeonefour.seatingplan.model;

import java.util.Objects;

public class Table {

    private final int id;
    private final String name;

    public Table(int it, String name) {
        this.id = it;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
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
        Table other = (Table) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return "Table [id=" + id + ", name=" + name + "]";
    }
}
