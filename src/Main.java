import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author csarasua
 * Main class to start the process.
 */
public class Main {


    // Main directory of the code.
    static String workingDir = System.getProperty("user.dir");
    static String workingDirForFileName = workingDir.replace("\\", "/");

    public static void main(String args[]) {

        // Analyzes all the contributions of the participants *during* the Wikidata Editathon
        runParticipantsAnalyzer();
        // Analyzes all the contributions of the participants *during the month after* the Wikidata Editathon
        runParticipantsAposterioriAnalyzer();


        /*
        // Saves a local copy of all  the contributions of the participants of the editathon during the event
        saveAllContributions();
        */
    }


    /**
     * Method to analyze the activity of the participants during the Wikidata Editathon.
     */
    private static void runParticipantsAnalyzer()
    {
        // Get the list of all participants.
        List<String> participants = getDSSWEparticipants();
        // Create and initialize an analyzer object (start and end of the Wikidata editathon).
        DSSWEAnalyzer partAnalyzer = new DSSWEAnalyzer("2015-07-03T07:00:00Z", "2015-07-03T18:00:00Z", participants, "/reports/edits_byusers_editathon.txt", "/reports/report_global_edits.txt");
        // Run the analysis by analyzing the contributions of participants.
        partAnalyzer.processByUsers();

    }

    /**
     * Method to analyze the activity of the participants during the Wikidata Editathon.
     * Timezone: CEST time, was GTM+2.
     * The event took place between 9:00 and 19:00 aprox. We extend the period of time to be analyzed further just in case.
     */
    private static void runParticipantsAposterioriAnalyzer()
    {
        // Get the list of all participants.
        List<String> participants = getDSSWEparticipants();
        // Create and initialize an analyzer object (start and end of the a posteriori period of time to track). Split the time (e.g. 1 month) in two periods of time due to the max. number of updates to
        DSSWEAnalyzer partAnalyzer = new DSSWEAnalyzer("2015-07-03T18:00:00Z", "2015-07-24T18:00:00Z", participants, "/reports/edits_byusers_editathon_aposteriori.txt", "/reports/report_global_edits_aposteriori.txt");
        // Run the analysis by analyzing the contributions of participants.
        partAnalyzer.processByUsers();

        //Repeat for the second period of time, after the Wikidata editathon.
        DSSWEAnalyzer partAnalyzer2 = new DSSWEAnalyzer("2015-07-24T18:00:00Z", "2015-08-03T18:00:00Z", participants, "/reports/edits_byusers_editathon_aposteriori2.txt", "/reports/report_global_edits_aposteriori2.txt");
        partAnalyzer2.processByUsers();

    }

    /**
     * Method to download the complete set of contributions of a particular user.
     */
    private static void saveAllContributions()
    {
        // Gets the list of all participants.
        List<String> participants = getDSSWEparticipants();

        // For each participant download all his/her contributions (API's response is limited to 500).
        for (int i=0; i<participants.size();i++)
        {
            String userName = participants.get(i);
            forceDownloadUserContributions(userName);
        }
    }

    /**
     * Method to get the contributions of a particular user.
     * @param userName name of the Wikidata user whose contributions have to bee queried.
     */
    private static void forceDownloadUserContributions(String userName)
    {

        // Create the HTTP client.
        HttpClient client = new DefaultHttpClient();

        // Create the HTTP GET request. Use the action:query / list:usercontribs option of the Wikidata API. default 10, max. 500 contributions.
        HttpGet getUserContributions = new HttpGet("https://www.wikidata.org/w/api.php?action=query&list=usercontribs&ucuser="+userName+"&uclimit=500");

        //Set the header of the HTTP GET call.
        getUserContributions.setHeader("Accept", "application/json");

        HttpResponse response = null;
        try {
            //Execute the HTTP GET request and obtain the response.
            response = client.execute(getUserContributions);

            // Get the status of the response.
            int statusCode = response.getStatusLine().getStatusCode();

            // If erverything went OK (status code == 200).
            if (statusCode == 200) {

                // Gets the response body
                InputStream in = response.getEntity().getContent();

                // Prepares the file where to write all the contributions.
                File f = new File(workingDirForFileName+"/reports/contributions_"+userName+".txt");

                //Inputstream to String.
                String inString = CharStreams.toString((new InputStreamReader(in, "UTF-8")));
                String ls = System.getProperty("line.separator");

                // Writes header of the file content.
                Files.write("***** contributions of " + userName + " *****", f, Charset.defaultCharset());
                // Writes the contributions in the file.
                Files.append(inString, f, Charset.defaultCharset());
                Files.append(ls, f, Charset.defaultCharset());


            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the list of participants from an input file in the /data directory of the project.
     * @return list of user names of the participants of the DSS Wikidata Editathon.
     */
    private static List<String> getDSSWEparticipants()
    {
        // Prepares the list of user names.
        List<String> users = new ArrayList<String>();

        // Reads the input file with all the user names (one per line).
        File fileUsers = new File(workingDirForFileName+"/data/usersdsswikidataeditathon.txt");
        try {
            // Uses guava to parse the file and each of its lines.
            List<String> listOfUsers = Files.readLines(fileUsers, Charset.defaultCharset());
            // Loads the read user names into the local list of user names.
            users.addAll(listOfUsers);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Returns the loaded list of names as the result of the method.
        return users;
    }
}
