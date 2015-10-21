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


    @Test
    public void testUserToJson() throws IOException{
        User user = new User();
        user.setName("foo");
        user.setUserType(UserType.DEVELOPER);
        System.out.println(user.toJson());
    }

    @Test
    public void testUserFromJson() throws IOException{
        HttpResponse response1 = makeHttpJsonRequest(sampleUser.getUrl(), "GET");
        JSONObject root = new JSONTokener(newUser)
        System.out.println("hello");

    }

    @Test
    public void testDefectToJson() throws IOException{
        System.out.println(sampleDefect.toJson());
    }

    @Test
    public void testDefectFromJson() {
        // TODO: optional
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
     * Test Requirement 2 (R): read user
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

    // TODO the rest of the test cases
}
