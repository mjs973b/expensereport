package name.mjs001.expensereport;

import java.io.Serializable;

/**
 * User model. id is immutable.
 */
public class User implements Serializable{
    // data fields; these match the columns in DB
    private UserId id;
    private String name;

    public User(UserId id, String name) {
        this.id = id;
        this.name = name.replace('"', '_');
    }
    // getters and setters
    public UserId getId() {
        return id;
    }

//    public void setId(UserId id) {
//        this.id = id;
//    }

    public String getName() {
        return name;
    }

    /** silently replace double-quote by underscore */
    public void setName(String name) {
        this.name = name.replace('"', '_');
    }

    @Override
    public String toString() {
        return name;
    }
}
