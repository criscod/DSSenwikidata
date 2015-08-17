import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author csarasua
 * Class to analyze the contributions of the Wikidata Editathon, by looking at the contributions of the participants.
 */
public class DSSWEAnalyzer {

    /*
     * File to report on the results of the analysis (reportFilePath is the path of the file
     * containing the concrete contributions of participants;
     * globalReportFilePath is the path of the file containing the global overview of the edited items and  the count of
     * Wikidata and non-Wikidata edited items.
     *
     */
    private String reportFilePath;
    private String globalReportFilePath;

    // When the event (period of time) started and ended.
    private String start;
    private String end;
    // List of user names of the participants of the event.
    private List<String> users = new ArrayList<String>();

    // Number of edits related to Wikidata items (identified by identifiers of the shape Qxxx).
    int wikidataEditsCount = 0;

    // All registered edits (not necessarily related to Wikidata items). Properties may also be edited, and also help pages etc.
    int allEditsCount = 0;


	// Number of edits on Wikidata Items DSS
    int dssEditsCount = 0;
    int dssNewItems = 0;

	// Number of new Wikidata items created
    int newItems = 0;

	// Types of edits --- defined by the Wikidata API: https://www.wikidata.org/w/api.php
    int wbeditentityCreateEdits = 0;
    int wbcreateclaimCreateEdits =0;
    int wbsetclaimCreateEdits = 0;
    int wbsetclaimUpdateEdits = 0;
    int wbsetreferenceAddEdits = 0;
    int wbsetreferenceSetEdits = 0;
    int wbsetqualifierEdits = 0;
    int wbsetlabelAddEdits = 0;
    int wbsetlabelSetEdits = 0;
    int wbsetdescriptionAddEdits=0;
    int wbsetdescriptionSetEdits=0;
    int wbsetdescriptionRemoveEdits=0;
    int wbremovereferencesRemoveEdits =0;
    int wbremoveclaimsRemoveEdits =0;
    int wbsetaliasesAddEdits =0;
    int wbsetaliasesSetEdits=0;
    int wbsetsitelinkAddEdits=0;
    int wbmergeitemsFromEdits=0;
    int clientsitelinkUpdateEdits=0;

    int euThings=0; //basque
    int esThings=0; //spanish
    int enThings=0; //english
    int deThings=0; //german
    int glThings=0; //galician
    int otherThings=0; //other


    // Current code directory.
    String workingDir = System.getProperty("user.dir");
    String workingDirForFileName = workingDir.replace("\\", "/");

    // Set of identifiers (title property in contributions) of all edited Wikidata items (Qxxx).
    Set<String> setOfEditedItems = new HashSet<String>();
    // Set of identifiers (title property in contributions) of all edited non-Wikidata items (properties, help pages etc. in Wikidata, but no Qxxx).
    Set<String> setOfEditedNonWikidataItems = new HashSet<String>();

    int numberOfEditedItems = 0;
    int numberOfCreatedItems = 0;

    int numberOfEditedItemsConnectedDSS = 0;


    // Map containing <key,value> pairs, where the key is the user name and the value is the count of edits of the user name used as key.
    Map<String, Integer> mapEditsOfUsers = new HashMap<String, Integer>();




    /**
     * Constructor method to create and initialize an analyzer
     *
     * @param eventStart  the date/time when the event started
     * @param eventEnd    the date/time when the event finished
     * @param listOfUsers the set of user names that participated in the event
     */
    public DSSWEAnalyzer(String eventStart, String eventEnd, List<String> listOfUsers, String report, String globalReport) {
        this.start = eventStart;
        this.end = eventEnd;
        this.users.addAll(listOfUsers);

        this.reportFilePath = report;
        this.globalReportFilePath = globalReport;

    }

    /**
     * Method to analyze and report on the contributions of the participantes / users.
     */
    public void processByUsers() {

        // Obtain the iterator of the list of user names.
        Iterator<String> usersIT = this.users.iterator();

        // Line separation used to write in the next line in the file.
        String ls = System.getProperty("line.separator");

        try {

            // Prepare the file where the contributions will be reported.
            File reportFile = new File(workingDirForFileName + this.reportFilePath);
            Files.write("**** TRACKING THE EDITS DONE BY REGISTERED PARTICIPANTS ****", reportFile, Charset.defaultCharset());
            Files.append(ls, reportFile, Charset.defaultCharset());

            // Prepares the file where the global overview of the contributions will be reported.
            File globalReportFile = new File(workingDirForFileName + this.globalReportFilePath);
            Files.write("**** GLOBAL RESULTS OF THE EDITATHON ****", globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());


            // Goes through all the participants / users.
            while (usersIT.hasNext()) {

                // Gets the participant to analyze in the current iteration.
                String user = usersIT.next();

                //Control variable to check that the method went well. 1:error ; 0:OK.
                int sstatus = 1;

                // While there is a problem (e.g. Internet broke or the server gave an error) keep on asking for the ocntributions of the participant.
                while (sstatus == 1) {
                    sstatus = processOneUserContributions(user);
                }
            }

            // Writes the number of edits by user by going through the map<username,editsCountOfUser>, which has been populated in the 'processOneUserContributions' method.
            Files.append("*number of edits by user*", globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Set<Map.Entry<String, Integer>> setOfMapEntries = mapEditsOfUsers.entrySet();
            Iterator<Map.Entry<String, Integer>> itSetMapEntries = setOfMapEntries.iterator();
            while (itSetMapEntries.hasNext()) {
                Map.Entry<String, Integer> entry = itSetMapEntries.next();
                Files.append("****" + entry.getKey() + "*" + entry.getValue() + "***", globalReportFile, Charset.defaultCharset());

            }
            Files.append(ls, globalReportFile, Charset.defaultCharset());


            // Write all the Wikidata and non Wikidata Qitems edited.
            Iterator setOfEditedItemsIterator = this.setOfEditedItems.iterator();
            this.numberOfEditedItems = this.setOfEditedItems.size();

            Files.append("------ list of edited items ------", globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());
            while (setOfEditedItemsIterator.hasNext()) {
                String itemI = setOfEditedItemsIterator.next().toString();
                Files.append("edited item: " + itemI, globalReportFile, Charset.defaultCharset());
                Files.append(ls, globalReportFile, Charset.defaultCharset());

              /*  ItemLooker itemLooker = new ItemLooker(itemI);
                boolean itemIDSS = itemLooker.isItemConnectedToDSS();
                if (itemIDSS) {
                    this.numberOfEditedItemsConnectedDSS = this.numberOfEditedItemsConnectedDSS + 1;
                }
               */

            }
            Iterator setOfEditedNonWikidataItemsIt = this.setOfEditedNonWikidataItems.iterator();
            Files.append("------ list of NON WIKIDATA edited items ------", globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());
            while (setOfEditedNonWikidataItemsIt.hasNext()) {
                String itemI = setOfEditedNonWikidataItemsIt.next().toString();
                Files.append("non Wikidata edited item: " + itemI, globalReportFile, Charset.defaultCharset());
                Files.append(ls, globalReportFile, Charset.defaultCharset());


            }


            // Writes global counts.
            Files.append("Number of total Wikidata edits: " + this.wikidataEditsCount, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());
            Files.append("Number of total edits: " + this.allEditsCount, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of edited items: " + this.numberOfEditedItems, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbeditentity-create edits: " + this.wbeditentityCreateEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbsetclaim-create edits: " + this.wbsetclaimCreateEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbsetclaim-udpate edits: " + this.wbsetclaimUpdateEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbcreateclaim-create edits: " + this.wbcreateclaimCreateEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbsetreference-add edits: " + this.wbsetreferenceAddEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbsetreference-set edits: " + this.wbsetreferenceSetEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbsetqualifier edits: " + this.wbsetqualifierEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbsetlabel-add edits: " + this.wbsetlabelAddEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbsetlabel-set edits: " + this.wbsetlabelSetEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbsetdescription-add edits: " + this.wbsetdescriptionAddEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbsetdescription-set edits: " + this.wbsetdescriptionSetEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbsetdescription-remove edits: " + this.wbsetdescriptionRemoveEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbremoveclaims-remove edits: " + this.wbremoveclaimsRemoveEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbremovereferences-remove edits: " + this.wbremovereferencesRemoveEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbsetaliases-add edits: " + this.wbsetaliasesAddEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbsetaliases-set edits: " + this.wbsetaliasesSetEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbsitelink-add edits: " + this.wbsetsitelinkAddEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of wbmergeitems-from edits: " + this.wbmergeitemsFromEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number of clientsitelink-update edits: " + this.clientsitelinkUpdateEdits, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());

            Files.append("number EU edits: " + this.euThings, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());
            Files.append("number ES edits: " + this.esThings, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());
            Files.append("number EN edits: " + this.enThings, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());
            Files.append("number DE edits: " + this.deThings, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());
            Files.append("number GL edits: " + this.glThings, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());
            Files.append("number nolang/other edits: " + this.otherThings, globalReportFile, Charset.defaultCharset());
            Files.append(ls, globalReportFile, Charset.defaultCharset());


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Method to get all the contributions of a particular user or participant.
     * @param userName
     * @return 1 if something went wrong 0 if everything went OK.
     * Note that feedcontributions (Wikidata API) only gives up to 50 contributions. Use query listofcontributions (able to retrieve up to 500 contributions at once).
     */
    public int processOneUserContributions(String userName) {

        // Control variable to return (by default everything OK).
        int result = 0;

        // Count of the number of edits made by the current user being analyzed.
        int editsOfCurrentUser = 0;

        // Create the HTTP client.
        HttpClient client = new DefaultHttpClient();
        HttpGet getContributionsOfUser;

        /* The user name which is read from the input file containing all the user names of the participants of the event
         * can be either a complete user name or a string of type "IP@IP prefix" (in case the user edited anonymously without logging in).
         * We track IPs with a prefix belonging to the UPV/EHU university, Lejona.
         * By exploration we found out that this was: 158.227.136.
         */
        if (userName.contains("IP@")) {
            // Gets the IP prefix.
            String[] split = userName.split("@");
            String IPprefix = split[1];
            // Create the HTTP GET request using the "ucuserprefix" parameter of the Wikidata API.
            getContributionsOfUser = new HttpGet("https://www.wikidata.org/w/api.php?action=query&list=usercontribs&format=json&ucstart=" + this.end + "&ucend=" + this.start + "&ucuserprefix=" + IPprefix + "&uclimit=500");

        } else {
            // Complete user name -- the participant was logged in while editing.
            // Create the HTTP GET request using the "ucuser" parameter of the Wikidata API.
            getContributionsOfUser = new HttpGet("https://www.wikidata.org/w/api.php?action=query&list=usercontribs&format=json&ucstart=" + this.end + "&ucend=" + this.start + "&ucuser=" + userName + "&uclimit=500");

        }



        // Prepares file in which to report all the edits by the user.
        File reportFile = new File(workingDirForFileName + this.reportFilePath);
        String ls = System.getProperty("line.separator");

        try {
            Files.append("********READING CONTRIBUTIONS of *****" + userName + "***", reportFile, Charset.defaultCharset());
            Files.append(ls, reportFile, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
            // If there was an exception, return 1 to show that there was a problem.
            result = 1;
        }

        //Set the header of the HTTP GET request and ask for a JSON response.
        getContributionsOfUser.setHeader("Accept", "application/json");

        HttpResponse response = null;
        try {
            int status = 1;
            // While there is something wrong (e.g. Internet broke or the server was offline) retry.
            while (status == 1) {
                //Execute the HTTP GET request and obtain the response.
                response = client.execute(getContributionsOfUser);
                // Gets the status line.
                int statusCode = response.getStatusLine().getStatusCode();
                // If everything went OK (status == 200)
                if (statusCode == 200) {

                    // Gets the entity in the response (body).
                    HttpEntity responseEntity = response.getEntity();
                    if (responseEntity != null) {
                        ObjectMapper mapper = new ObjectMapper();
                        InputStream in = null;
                        try {
                            in = responseEntity.getContent();
                        } catch (IOException e) {
                            e.printStackTrace();
                            result = 1;
                        }
                        Map<String, Object> contributionsCall = null;
                        try {
                            contributionsCall = mapper.readValue(in,
                                    new TypeReference<Map<String, Object>>() {
                                    });
                        } catch (IOException e) {
                            e.printStackTrace();
                            result = 1;
                        }


                        List<Object> listOfUserContributions;
                        Map<String, Object> contribs;
                        Map<String, Object> contributionI;
                        Integer userId;
                        String user = new String(); //username
                        Integer pageId;
                        Integer revId;
                        Integer parentId;
                        Integer ns;
                        String titleItemId = new String();
                        String timestamp = new String();
                        String comment = new String();
                        Integer size;



                        /**
                         * Shape of API response:
                         * {
                         "batchcomplete": "",
                         "query": {
                         "usercontribs": [
                         {
                         "userid": 2088802,
                         "user": "xxx",
                         "pageid": 22387768,
                         "revid": 225922733,
                         "parentid": 225922207,
                         "ns": 0,
                         "title": "Q20640474",
                         "timestamp": "2015-07-03T15:57:04Z",
                         "top": "",
                         "comment": "[[Property:P276]]: [[Q10313]]",
                        "size": 2369
                    }, ....
                         */

                        //most outer level
                        for (Map.Entry<String, Object> attribute : contributionsCall
                                .entrySet()) {
                            if (attribute.getKey().equals("query") && attribute.getValue() instanceof Map) {

                                contribs = (Map<String, Object>) attribute.getValue();

                                for (Map.Entry<String, Object> entryC : contribs.entrySet()) {

                                    // Reads the contributions
                                    if (entryC.getKey().equals("usercontribs") && entryC.getValue() instanceof List) {

                                        listOfUserContributions = (ArrayList<Object>) entryC.getValue();


                                        for (int i = 0; i < listOfUserContributions.size(); i++) {

                                            // Processes each contribution of this user
                                            contributionI = (Map<String, Object>) listOfUserContributions.get(i);

                                            // Write the processed contribution in the file.
                                            Files.append("--- new contribution ---", reportFile, Charset.defaultCharset());
                                            Files.append(ls, reportFile, Charset.defaultCharset());

                                            for (Map.Entry<String, Object> entry : contributionI.entrySet()) {


                                                //Read one complete contribution: get the user id, user name, pageid, revid, parentid, ns, title of item, timestamp, comment, size of contribution.
                                                if (entry.getKey().equals("userid") && entry.getValue() instanceof Integer) {
                                                    userId = (Integer) entry.getValue();

                                                    Files.append("userid: " + userId, reportFile, Charset.defaultCharset());
                                                    Files.append(ls, reportFile, Charset.defaultCharset());

                                                } else if (entry.getKey().equals("user") && entry.getValue() instanceof String) {
                                                    user = (String) entry.getValue();

                                                    Files.append("user name (user): " + user, reportFile, Charset.defaultCharset());
                                                    Files.append(ls, reportFile, Charset.defaultCharset());

                                                } else if (entry.getKey().equals("pageid") && entry.getValue() instanceof Integer) {
                                                    pageId = (Integer) entry.getValue();

                                                    Files.append("page id: " + pageId, reportFile, Charset.defaultCharset());
                                                    Files.append(ls, reportFile, Charset.defaultCharset());


                                                } else if (entry.getKey().equals("revid") && entry.getValue() instanceof Integer) {
                                                    revId = (Integer) entry.getValue();

                                                    Files.append("rev id: " + revId, reportFile, Charset.defaultCharset());
                                                    Files.append(ls, reportFile, Charset.defaultCharset());

                                                } else if (entry.getKey().equals("parentid") && entry.getValue() instanceof Integer) {
                                                    parentId = (Integer) entry.getValue();

                                                    Files.append("parent id: " + parentId, reportFile, Charset.defaultCharset());
                                                    Files.append(ls, reportFile, Charset.defaultCharset());


                                                } else if (entry.getKey().equals("ns") && entry.getValue() instanceof Integer) {
                                                    ns = (Integer) entry.getValue();

                                                    Files.append("ns: " + ns, reportFile, Charset.defaultCharset());
                                                    Files.append(ls, reportFile, Charset.defaultCharset());

                                                } else if (entry.getKey().equals("title") && entry.getValue() instanceof String) {
                                                    titleItemId = (String) entry.getValue();

                                                    Files.append("title (Item Id): " + titleItemId, reportFile, Charset.defaultCharset());
                                                    Files.append(ls, reportFile, Charset.defaultCharset());

                                                } else if (entry.getKey().equals("timestamp") && ((entry.getValue() instanceof String) || (entry.getValue() instanceof Date))) {
                                                    timestamp = (String) entry.getValue();

                                                    Files.append("timestamp: " + timestamp, reportFile, Charset.defaultCharset());
                                                    Files.append(ls, reportFile, Charset.defaultCharset());

                                                } else if (entry.getKey().equals("comment") && entry.getValue() instanceof String) {
                                                    comment = (String) entry.getValue();

                                                    Files.append("comment: " + comment, reportFile, Charset.defaultCharset());
                                                    Files.append(ls, reportFile, Charset.defaultCharset());

                                                } else if (entry.getKey().equals("size") && ((entry.getValue() instanceof String)) || (entry.getValue() instanceof Integer)) {
                                                    size = (Integer) entry.getValue();

                                                    Files.append("size: " + size, reportFile, Charset.defaultCharset());
                                                    Files.append(ls, reportFile, Charset.defaultCharset());

                                                }

                                            }


                                            // Checks it the contribution is on a Wikidata (Qxxx) item and count accordingly.

                                            if (titleItemId.startsWith("Q")) {
                                                this.setOfEditedItems.add(titleItemId);
                                                this.wikidataEditsCount = this.wikidataEditsCount + 1;



                                                if(comment.contains("wbeditentity-create"))
                                                {
                                                    this.wbeditentityCreateEdits = this.wbeditentityCreateEdits+1;
                                                }
                                                else if (comment.contains("wbsetclaim-create"))
                                                {
                                                    this.wbsetclaimCreateEdits = this.wbsetclaimCreateEdits+1;
                                                }
                                                else if (comment.contains("wbsetclaim-update"))
                                                {
                                                    this.wbsetclaimUpdateEdits = this.wbsetclaimUpdateEdits+1;
                                                }
                                                else if(comment.contains("wbsetreference-add"))
                                                {
                                                    this.wbsetreferenceAddEdits = this.wbsetreferenceAddEdits+1;
                                                }
                                                else if(comment.contains("wbsetreference-set"))
                                                {
                                                    this.wbsetreferenceSetEdits = this.wbsetreferenceSetEdits+1;
                                                }
                                                else if(comment.contains("wbsetqualifier"))
                                                {
                                                    this.wbsetqualifierEdits = this.wbsetqualifierEdits+1;
                                                }
                                                else if(comment.contains("wbsetlabel-add"))
                                                {
                                                    this.wbsetlabelAddEdits = this.wbsetlabelAddEdits+1;
                                                }
                                                else if(comment.contains("wbsetlabel-set"))
                                                {
                                                    this.wbsetlabelSetEdits = this.wbsetlabelSetEdits+1;
                                                }
                                                else if(comment.contains("wbremoveclaims-remove"))
                                                {
                                                    this.wbremoveclaimsRemoveEdits = this.wbremoveclaimsRemoveEdits+1;
                                                }
                                                else if(comment.contains("wbremovereferences-remove"))
                                                {
                                                    this.wbremovereferencesRemoveEdits = this.wbremovereferencesRemoveEdits+1;
                                                }
                                                else if(comment.contains("wbsetdescription-add"))
                                                {
                                                    this.wbsetdescriptionAddEdits = this.wbsetdescriptionAddEdits+1;
                                                }
                                                else if(comment.contains("wbsetdescription-set"))
                                                {
                                                    this.wbsetdescriptionSetEdits = this.wbsetdescriptionSetEdits+1;
                                                }
                                                else if(comment.contains("wbsetaliases-add"))
                                                {
                                                    this.wbsetaliasesAddEdits = this.wbsetaliasesAddEdits+1;
                                                }
                                                else if(comment.contains("wbsetaliases-set"))
                                                {
                                                    this.wbsetaliasesSetEdits = this.wbsetaliasesSetEdits+1;
                                                }
                                                else if(comment.contains("wbsetsitelink-add"))
                                                {
                                                    this.wbsetsitelinkAddEdits = this.wbsetsitelinkAddEdits+1;
                                                }
                                                else if(comment.contains("wbmergeitems-from"))
                                                {
                                                    this.wbmergeitemsFromEdits = this.wbmergeitemsFromEdits+1;
                                                }
                                                else if(comment.contains("wbcreateclaim-create"))
                                                {
                                                    this.wbcreateclaimCreateEdits = this.wbcreateclaimCreateEdits+1;
                                                }
                                                else if(comment.contains("wbsetdescription-remove"))
                                                {
                                                    this.wbsetdescriptionRemoveEdits = this.wbsetdescriptionRemoveEdits+1;
                                                }
                                                else if(comment.contains("clientsitelink-update"))
                                                {
                                                    this.clientsitelinkUpdateEdits = this.clientsitelinkUpdateEdits+1;
                                                }

                                                if(comment.contains("|en"))
                                                { this.enThings = this.enThings+1;}
                                                else if(comment.contains("|eu"))
                                                { this.euThings = this.euThings+1;}
                                                else if(comment.contains("|es"))
                                                { this.esThings = this.esThings+1;}
                                                else if(comment.contains("|de"))
                                                { this.deThings = this.deThings+1;}
                                                else if(comment.contains("|gl"))
                                                { this.glThings = this.glThings+1;}
                                                else //others or no lang
                                                { this.otherThings = this.otherThings+1;}


                                            } else {
                                                setOfEditedNonWikidataItems.add(titleItemId);
                                            }
                                            // Count all edits.
                                            this.allEditsCount = this.allEditsCount + 1;
                                            if (comment.contains("wbeditentity")) {
                                                this.numberOfCreatedItems = this.numberOfEditedItems + 1;
                                            }




                                            editsOfCurrentUser = editsOfCurrentUser + 1;



                                        }


                                    }

                                }
                            }
                        }



 }
                    // Everything was read property -- show it went OK in return.
                    status = 0;

                    //Creates the <key,Value> entry in the map with the user name and his/her number of edits.
                    this.mapEditsOfUsers.put(userName, editsOfCurrentUser);


                }
            }


        } catch (HttpHostConnectException connE) {
            connE.printStackTrace();
            // Shows something that went wrong.
            result = 1;
        } catch (IOException e) {
            e.printStackTrace();
            // Shows something that went wrong.
            result = 1;
        }
        // Always return result (1 / 0).
        finally {
            return result;
        }



    }



    // Getter and setters of class variables.

    public int getWikidataEditsCount() {
        return wikidataEditsCount;
    }

    public void setWikidataEditsCount(int editsCount) {
        this.wikidataEditsCount = editsCount;
    }

    public int getDssEditsCount() {
        return dssEditsCount;
    }

    public void setDssEditsCount(int dssEditsCount) {
        this.dssEditsCount = dssEditsCount;
    }

    public int getDssNewItems() {
        return dssNewItems;
    }

    public void setDssNewItems(int dssNewItems) {
        this.dssNewItems = dssNewItems;
    }

    public int getNewItems() {
        return newItems;
    }

    public void setNewItems(int newItems) {
        this.newItems = newItems;
    }
}
