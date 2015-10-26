import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by ian on 15-10-02.
 */
public class TestServer {

    public static final String SERVER = "localhost";
    public static final int PORT = 9999;
    public static final String PREFIX = "http://" + SERVER + ":" + String.valueOf(PORT);


    /* private helper for makeHttp*Request() */
    private static void copyStreamBuffered(InputStream in, OutputStream out) throws IOException {
        BufferedInputStream inBuf = new BufferedInputStream(in, 1024);
        BufferedOutputStream outBuf = new BufferedOutputStream(out, 1024);
        int i;
        while((i = inBuf.read()) > 0)
            outBuf.write(i);
        outBuf.flush();
    }

    /**
     * Make an HTTP Request
     * @param urlStr URL for request
     * @param method HTTP method
     * @return An HttpResponse with status, headers and response body (if given).
     * @throws IOException
     */
    private static HttpResponse makeHttpJsonRequest(String urlStr, String method) throws IOException {
        return makeHttpJsonRequest(urlStr, method, null);
    }


    /**
     * Make an HTTP Request with JSON body
     * @param urlStr URL for request
     * @param method HTTP method
     * @param requestJson The JSON resquest body
     * @return An HttpResponse with status, headers and response body (if given).
     * @throws IOException
     */
    private static HttpResponse makeHttpJsonRequest(String urlStr, String method, String requestJson) throws IOException {

        // Create URL object for HTTP connection
        URL url = new URL(urlStr);

        // Create HTTP connection from URL
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);

        // If the user has specified JSON for the request, configure connection, add content-type header
        // and copy JSON to output stream
        if(requestJson != null) {
            con.setDoOutput(true);
            con.setRequestProperty("content-type", "application/json");
            copyStreamBuffered(new ByteArrayInputStream(requestJson.getBytes()), con.getOutputStream());
        }

        // Retrieve information connection
        Map<String, List<String>> headers = con.getHeaderFields();
        int status = con.getResponseCode();
        String response = null;
        try {
            // copy JSON from output stream
            ByteArrayOutputStream responseBytes = new ByteArrayOutputStream();
            copyStreamBuffered(con.getInputStream(), responseBytes);
            response = new String(responseBytes.toByteArray());
        }
        catch(IOException e) { /* no response body */ }

        return new HttpResponse(status, headers, response);
    }

    // Sample user created during setUp
    private User sampleUser;
    private Defect sampleDefect;

    /**
     * Create a sampleUser user and defect.
     * @throws IOException
     */
    @Before
    public void setUp() throws IOException {
        // Create sample user.
        sampleUser = new User().setName("Bar")
                               .setImageUrl("http://example.com/bar")
                               .setUserType(UserType.DEVELOPER);
        HttpResponse response = makeHttpJsonRequest(PREFIX + "/user/", "POST", sampleUser.toJson());
        sampleUser.setUrl(response.getHeaders().get("Location").get(0)); // Header from POST contains the URL of the new user

        // Create sample defect.
        sampleDefect = new Defect().setSummary("Unfortunately, Notes has Stopped...")
                                   .setCreated(new Date())
                                   .setCreatedByUrl(sampleUser.getUrl())
                                   .setAssignedToUrl(sampleUser.getUrl())
                                   .setSeverity(Severity.TRIVIAL)
                                   .setStatus(Status.CREATED);
        response = makeHttpJsonRequest(PREFIX + "/defect/", "POST", sampleDefect.toJson());
        sampleDefect.setUrl(response.getHeaders().get("Location").get(0));
    }

    /**
     * Remove all users and defects from the server: restore server to initial state
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {

        String[] repos = new String[] {"defect", "user"}; // The order matters due to FK contraints

        for(String repo : repos) {
            // Get all items from repo
            HttpResponse response = makeHttpJsonRequest(PREFIX + "/" + repo, "GET");
            if (response.getStatus() != 200)
                throw new IOException("Cannot get /" + repo + " repository. Something is wrong, please restart server.");

            // extract the JSON array from inside the response JSON, it it's not there the repo is empty
            JSONArray items = null;
            try {
                JSONObject root = new JSONObject(new JSONTokener(response.getBody()));
                items = root.getJSONObject("_embedded").getJSONArray(repo);
            } catch (JSONException e) { /* empty repo, or else no item array */ }

            // For non-empty repos, delete all items
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    // the URL is located in the "self" -> "href" field.
                    String href = items.getJSONObject(i).getJSONObject("_links").getJSONObject("self").getString("href");

                    // Delete the item
                    HttpResponse deleteResponse = makeHttpJsonRequest(href, "DELETE");
                    if (deleteResponse.getStatus() != 204)
                        throw new Exception("Could not delete " + href + ". Please restart server.");
                }
            }
        }

    }

    /**
     * Test Requirement 0: can connect to server.
     * @throws IOException
     */
    @Test
    public void testConnection() throws IOException {
        HttpResponse response = makeHttpJsonRequest(PREFIX, "GET");
        assertEquals(200, response.getStatus());
    }

    /**
     * Test Requirement 1 : class keeps user information
     * @throws IOException
     */
    @Test
    public void testUserInformation() throws IOException {
        assertEquals(sampleUser.getName(),"Bar");
        assertEquals(sampleUser.getImageUrl(),"http://example.com/bar");
        assertEquals(sampleUser.getUserType(), UserType.DEVELOPER);
    }

    // ----------- TESTING REQUIREMENTS 2 - CRUD USER -------------- //
    /**
     * Test Requirement 2 (C): create user
     * @throws IOException
     */
    @Test
    public void testCreateUser() throws IOException {
        User user = new User().setName("Test")
                .setUserType(UserType.DEVELOPER);
        HttpResponse response = makeHttpJsonRequest(PREFIX + "/user/", "POST", user.toJson());
        assertEquals(response.getStatus(), 201);
    }

    /**
     * Test Requirement 2 (R): read all users
     * @throws IOException
     */
    @Test
    public void testReadAllUser() throws IOException {
        HttpResponse response = makeHttpJsonRequest(PREFIX + "/user", "GET");
        List<User> receivedUser = User.fromJson((new JSONObject(new JSONTokener(response.getBody())).getJSONObject("_embedded").getJSONArray("user")));
        assertEquals(receivedUser.get(0).getName(),"Bar");
        assertEquals(receivedUser.get(0).getImageUrl(),"http://example.com/bar");
        assertEquals(receivedUser.get(0).getUserType(),UserType.DEVELOPER);
        assertEquals(response.getStatus(), 200);
    }

    /**
     * Test Requirement 2 (R): read single user
     * @throws IOException
     */
    @Test
    public void testReadUser() throws IOException {
        HttpResponse response = makeHttpJsonRequest(sampleUser.getUrl(), "GET");
        User receivedUser = User.fromJson(new JSONObject(new JSONTokener(response.getBody())));
        assertEquals("Bar", receivedUser.getName());
        assertEquals("http://example.com/bar", receivedUser.getImageUrl());
        assertEquals(UserType.DEVELOPER, receivedUser.getUserType());
    }

    /**
     * Test Requirement 2 (U): update user
     * @throws IOException
     */
    @Test
    public void testUpdateUser() throws IOException {
        //Updating our sampleUser with this 'new' user
        User user = new User().setName("Test")
                .setUserType(UserType.CUSTOMER);
        HttpResponse response = makeHttpJsonRequest(sampleUser.getUrl(), "PUT", user.toJson());
        assertEquals(response.getStatus(), 204);
        response = makeHttpJsonRequest(sampleUser.getUrl(), "GET");
        User receivedUser = User.fromJson(new JSONObject(new JSONTokener(response.getBody())));
        //Checking if values in sampleUser have updated
        assertEquals("Test", receivedUser.getName());
        assertEquals(UserType.CUSTOMER, receivedUser.getUserType());
    }

    /**
     * Test Requirement 2 (D): delete user
     * @throws IOException
     */
    @Test
    public void testDeleteUser() throws IOException {
        HttpResponse response = makeHttpJsonRequest(sampleUser.getUrl(), "DELETE");
        assertEquals(response.getStatus(), 204);
        response = makeHttpJsonRequest(sampleUser.getUrl(), "GET");
        assertEquals(response.getStatus(), 200);
    }

    /**
     * Test Requirement 2 (CB): created by user
     * @throws IOException
     */
    @Test
    public void testCreatedByUser() throws IOException,ParseException {
        //Get the defects the user created
        HttpResponse response = makeHttpJsonRequest(sampleUser.getUrl()+"/created", "GET");
        assertEquals(response.getStatus(), 200);
        //Checking the values to make sure they match
        List<Defect> defects = Defect.fromJson((new JSONObject(new JSONTokener(response.getBody())).getJSONObject("_embedded").getJSONArray("defect")));
        assertEquals(defects.get(0).getSummary(), sampleDefect.getSummary());
        assertEquals(defects.get(0).getStatus(),sampleDefect.getStatus());
    }

    /**
     * Test Requirement 2 (A): assigned to user
     * @throws IOException
     */
    @Test
    public void testAssignedToUser() throws IOException,ParseException {
        //Get the defects the user was assigned
        HttpResponse response = makeHttpJsonRequest(sampleUser.getUrl()+"/assigned", "GET");
        assertEquals(response.getStatus(), 200);
        List<Defect> defects = Defect.fromJson((new JSONObject(new JSONTokener(response.getBody())).getJSONObject("_embedded").getJSONArray("defect")));
        assertEquals(defects.get(0).getSummary(), sampleDefect.getSummary());
        assertEquals(defects.get(0).getStatus(),sampleDefect.getStatus());
    }

    /**
     * Test Requirement 2 (R): read user by name
     * @throws IOException
     */
    @Test
    public void testReadUserByName() throws IOException {
        HttpResponse response = makeHttpJsonRequest(PREFIX + "/user/search/findByName?name="+sampleUser.getName(), "GET");
        List<User> receivedUser = User.fromJson((new JSONObject(new JSONTokener(response.getBody())).getJSONObject("_embedded").getJSONArray("user")));
        assertEquals(response.getStatus(),200);
        assertEquals("Bar", receivedUser.get(0).getName());
        assertEquals("http://example.com/bar", receivedUser.get(0).getImageUrl());
        assertEquals(UserType.DEVELOPER, receivedUser.get(0).getUserType());
    }
    // ---------------------------------------------------//

    /**
     * Test Requirement 3 (C): create user with non-unique name
     * @throws IOException
     */
    @Test
    public void testUniqueUser() throws IOException {
        //since we are creating new user with existing user, this should fail
        HttpResponse response = makeHttpJsonRequest(PREFIX + "/user/", "POST", sampleUser.toJson());
        assertEquals(response.getStatus(), 409);
    }

    /**
     * Test Requirement 4 : user types
     * @throws IOException
     */
    @Test
    public void testUserTypes() throws IOException {
        //TESTING USERTYPE CUSTOMER
        sampleUser.setUserType(UserType.CUSTOMER);
        HttpResponse response = makeHttpJsonRequest(sampleUser.getUrl(), "PUT", sampleUser.toJson());
        assertEquals(response.getStatus(), 204);
        //TESTING USERTYPE MANAGER
        sampleUser.setUserType(UserType.MANAGER);
        response = makeHttpJsonRequest(sampleUser.getUrl(), "PUT", sampleUser.toJson());
        assertEquals(response.getStatus(), 204);
        //TESTING USERTYPE DEVELOPER
        sampleUser.setUserType(UserType.DEVELOPER);
        response = makeHttpJsonRequest(sampleUser.getUrl(), "PUT", sampleUser.toJson());
        assertEquals(response.getStatus(), 204);
        //TESTING USERTYPE TESTER
        // ** NOTE ** value TESTER was not in DefectServer, had to manually add in the server files
        sampleUser.setUserType(UserType.TESTER);
        response = makeHttpJsonRequest(sampleUser.getUrl(), "PUT", sampleUser.toJson());
        assertEquals(response.getStatus(), 204);
        //TESTING INVALID USERTYPE
        sampleUser.setUserType(UserType.BADVALUE);
        response = makeHttpJsonRequest(sampleUser.getUrl(), "PUT", sampleUser.toJson());
        assertEquals(response.getStatus(), 400);
    }

    /**
     * Test Requirement 5 : class keeps defect information
     * @throws IOException
     */
    @Test
    public void testDefectInformation() throws IOException {
        assertEquals(sampleDefect.getSummary(),"Unfortunately, Notes has Stopped...");
        assertEquals(sampleDefect.getSeverity(),Severity.TRIVIAL);
        assertEquals(sampleDefect.getCreatedByUrl(), sampleUser.getUrl());
        assertEquals(sampleDefect.getStatus(), Status.CREATED);
    }

    /**
     * Test Requirement 7 : status
     * @throws IOException
     */
    @Test
    public void testStatus() throws IOException {
        //TESTING STATUS CREATED
        sampleDefect.setStatus(Status.CREATED);
        HttpResponse response = makeHttpJsonRequest(sampleDefect.getUrl(), "PUT", sampleDefect.toJson());
        assertEquals(response.getStatus(), 204);
        //TESTING STATUS ACCEPTED
        sampleDefect.setStatus(Status.ACCEPTED);
        response = makeHttpJsonRequest(sampleDefect.getUrl(), "PUT", sampleDefect.toJson());
        assertEquals(response.getStatus(), 204);
        //TESTING STATUS FIXED
        sampleDefect.setStatus(Status.FIXED);
        response = makeHttpJsonRequest(sampleDefect.getUrl(), "PUT", sampleDefect.toJson());
        assertEquals(response.getStatus(), 204);
        //TESTING STATUS REOPENED
        sampleDefect.setStatus(Status.REOPENED);
        response = makeHttpJsonRequest(sampleDefect.getUrl(), "PUT", sampleDefect.toJson());
        assertEquals(response.getStatus(), 204);
        //TESTING STATUS CLOSED
        sampleDefect.setStatus(Status.CLOSED);
        response = makeHttpJsonRequest(sampleDefect.getUrl(), "PUT", sampleDefect.toJson());
        assertEquals(response.getStatus(), 204);
        //TESTING INVALID STATUS
        sampleDefect.setStatus(Status.BADVALUE);
        response = makeHttpJsonRequest(sampleDefect.getUrl(), "PUT", sampleDefect.toJson());
        assertEquals(response.getStatus(), 400);
    }
    /**
     * Test Requirement 8 : severity
     * @throws IOException
     */
    @Test
    public void testSeverity() throws IOException {
        //TESTING Severity TRIVIAL
        sampleDefect.setSeverity(Severity.TRIVIAL);
        HttpResponse response = makeHttpJsonRequest(sampleDefect.getUrl(), "PUT", sampleDefect.toJson());
        assertEquals(response.getStatus(), 204);
        //TESTING SEVERITY MINOR
        sampleDefect.setSeverity(Severity.MINOR);
        response = makeHttpJsonRequest(sampleDefect.getUrl(), "PUT", sampleDefect.toJson());
        assertEquals(response.getStatus(), 204);
        //TESTING SEVERITY MAJOR
        sampleDefect.setSeverity(Severity.MAJOR);
        response = makeHttpJsonRequest(sampleDefect.getUrl(), "PUT", sampleDefect.toJson());
        assertEquals(response.getStatus(), 204);
        //TESTING SEVERITY SHOWSTOPPER
        sampleDefect.setSeverity(Severity.SHOWSTOPPER);
        response = makeHttpJsonRequest(sampleDefect.getUrl(), "PUT", sampleDefect.toJson());
        assertEquals(response.getStatus(), 204);
        //TESTING INVALID SEVERITY
        sampleDefect.setSeverity(Severity.BADVALUE);
        response = makeHttpJsonRequest(sampleDefect.getUrl(), "PUT", sampleDefect.toJson());
        assertEquals(response.getStatus(), 400);
    }
    /**
     * Test Requirement 9 : created/assigned to user
     * @throws IOException
     */
    @Test
    public void testUserDefects() throws IOException,ParseException {
        //Get the defects the user created
        HttpResponse response = makeHttpJsonRequest(sampleUser.getUrl()+"/created", "GET");
        assertEquals(response.getStatus(), 200);
        //Checking the values to make sure they match
        List<Defect> defects = Defect.fromJson((new JSONObject(new JSONTokener(response.getBody())).getJSONObject("_embedded").getJSONArray("defect")));
        assertEquals(defects.get(0).getSummary(), sampleDefect.getSummary());
        assertEquals(defects.get(0).getStatus(),sampleDefect.getStatus());
        //Get the defects the user was assigned
        response = makeHttpJsonRequest(sampleUser.getUrl()+"/assigned", "GET");
        assertEquals(response.getStatus(), 200);
    }
    /**
     * Test Requirement 10 : created/modified dates
     * @throws IOException
     */
    @Test
    public void testDefectsDate() throws IOException,ParseException {
        //set Modified to a earlier date than created
        sampleDefect.setModified(Defect.formatter.parse("2001-01-15 03:01:01"));
        HttpResponse response = makeHttpJsonRequest(sampleDefect.getUrl(), "PUT", sampleDefect.toJson());
        //the update should not have worked
        assertEquals(response.getStatus(), 400);
    }

    /**
     * Test Requirement 11 : Assigned Defect
     * @throws IOException
     */
    @Test
    public void testAssignedDefect() throws IOException,ParseException {
        sampleDefect.setStatus(Status.REOPENED);
        HttpResponse response = makeHttpJsonRequest(PREFIX + "/defect/", "POST", sampleDefect.toJson());
        //the create should work because defect has assignedTo and status is REOPENED
        assertEquals(response.getStatus(), 201);

        Defect defect = new Defect().setSummary("Unfortunately, Notes has Stopped...")
                .setCreated(new Date())
                .setCreatedByUrl(sampleUser.getUrl())
                .setSeverity(Severity.TRIVIAL)
                .setStatus(Status.REOPENED);
        response = makeHttpJsonRequest(PREFIX + "/defect/", "POST", defect.toJson());
        //the create should not work because defect doesn't have assignedTo but has status REOPENED
        assertEquals(response.getStatus(), 500);
    }

    /**
     * Test Requirement 12 : Assigned Developer
     * @throws IOException
     */
    @Test
    public void testAssignedDeveloper() throws IOException,ParseException {
        // Create tmp user with UserType as Customer to test.
        User tmpUser = new User().setName("Foo")
                .setImageUrl("http://example.com/bar")
                .setUserType(UserType.CUSTOMER);
        HttpResponse response = makeHttpJsonRequest(PREFIX + "/user/", "POST", tmpUser.toJson());
        tmpUser.setUrl(response.getHeaders().get("Location").get(0)); // Header from POST contains the URL of the new user

        //try to create defect with assignedTo to point to a userType Customer(should not work)
        Defect defect = new Defect().setSummary("Unfortunately, Notes has Stopped...")
                .setCreated(new Date())
                .setCreatedByUrl(sampleUser.getUrl())
                .setSeverity(Severity.TRIVIAL)
                .setAssignedToUrl(tmpUser.getUrl())
                .setStatus(Status.REOPENED);
        response = makeHttpJsonRequest(PREFIX + "/defect/", "POST", defect.toJson());
        assertEquals(response.getStatus(), 409);
    }

}
