import java.util.ArrayList;
import java.util.List;

/**
 * @author csarasua
 *         Class to handle all the users who participated in the Wikidata editathon
 */
public class UsersManager {

    // Lists of user names
    List<String> users = new ArrayList<String>();

    // Initialization of a UsersManager object
    public UsersManager(List<String> participants) {

        this.users.addAll(participants);
    }

    // Obtains the list of user names
    public List<String> getUsers() {
        return users;
    }

    // Updates the list of user names
    public void setUsers(List<String> users) {
        this.users = users;
    }
}
