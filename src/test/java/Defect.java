import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.Exception;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by ian on 15-10-03.
 */
public class Defect {


    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    /**
     * Create a list of Defect objects from a JSONArray
     * @param root
     * @return Created users
     */
    public static List<Defect> fromJson(JSONArray root) throws ParseException {
        List<Defect> defects = new ArrayList<>();
        for(int i = 0; i < root.length(); i++)
            defects.add(fromJson(root.getJSONObject(i)));
        return defects;
    }

    /**
     * Create a Defect object from a JSONObject
     * @param root
     * @return Created defect
     */
    public static Defect fromJson(JSONObject root) throws ParseException {
        Defect defect = new Defect();
        defect.setCreated(new Date (root.getString("created")));
        defect.setModified(new Date(root.getString("modified")));
        defect.setSummary(root.getString("summary"));
        defect.setStatus((Status) root.get("status"));
        defect.setAssignedToUrl(root.getJSONObject("_links").getString("assignedToUrl"));
        defect.setCreatedByUrl(root.getJSONObject("_links").getString("createdBy"));
        defect.setSeverity((Severity) root.get("severity"));

        // extract the post resource URL from the "_links" object
        JSONObject links = root.getJSONObject("_links");
        defect.setUrl(links.getJSONObject("self").getString("href"));

        return defect;
    }

    /* Fields */

    private String url;
    private Date created;
    private Date modified;
    private String summary;
    private Status status;
    private String assignedToUrl;
    private String createdByUrl;
    private Severity severity;


    /* Getters and Setters */

    public String getUrl() {
        return url;
    }

    public Defect setUrl(String url) {
        this.url = url;
        return this;
    }

    public Date getCreated() {
        return created;
    }

    public Defect setCreated(Date created) {
        this.created = created;
        return this;
    }

    public Date getModified() {
        return modified;
    }

    public Defect setModified(Date modified) {
        this.modified = modified;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public Defect setSummary(String summary) {
        this.summary = summary;
        return this;

    }

    public Status getStatus() {
        return status;
    }

    public Defect setStatus(Status status) {
        this.status = status;
        return this;
    }

    public String getAssignedToUrl() {
        return assignedToUrl;
    }

    public Defect setAssignedToUrl(String assignedToUrl) {
        this.assignedToUrl = assignedToUrl;
        return this;
    }

    public String getCreatedByUrl() {
        return createdByUrl;
    }

    public Defect setCreatedByUrl(String createdByUrl) {
        this.createdByUrl = createdByUrl;
        return this;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Defect setSeverity(Severity severity) {
        this.severity = severity;
        return this;
    }

    /**
     * Return the JSON representation of the defect.
     * @return JSON representation of the defect
     */
    public String toJson() throws IOException {
        StringBuilder sb = new StringBuilder();
        if( created ==null || createdByUrl == null || status == null){
            throw new IOException("Missing required fields for JSON");
        }
        sb.append("{ \"created\" : \"");
        sb.append(formatter.format(created));
        sb.append("\" , \"status\": \"");
        sb.append(status);
        sb.append("\" , \"createdBy\": \"");
        sb.append(createdByUrl);

        if(severity != null) {
            sb.append("\" , \"severity\": \"");
            sb.append(severity);
        }

        if(summary != null){
            sb.append("\" , \"summary\": \"");
            sb.append(summary);
        }

        if( modified != null){
            sb.append("{ \"modified\" : \"");
            sb.append(formatter.format(modified));
        }
        if(assignedToUrl != null){
            sb.append("\" , \"assignedToUrl\": \"");
            sb.append(assignedToUrl);
        }
        sb.append("\"}");
        sb=sb;
        return sb.toString();
    }

    /* helper for .equals() */
    private static boolean nullOrEqual(Object o1, Object o2) {
        try {
            // was (o1 == null && o2 == null) || (o1.equals(o2));
            // returns true if: o1 == null and o2 == null,
            // shortcut: if o1 == o2 then o1.equals(o2) by property of equality.
            return (o1 == o2) || (o1.equals(o2));
        }
        catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object rhsObj) {
        Defect rhs = (Defect) rhsObj;

        List<Boolean> tests = new LinkedList<>();
        tests.add( nullOrEqual(this.created,    rhs.created) );
        tests.add( nullOrEqual(this.status,     rhs.status) );
        tests.add( nullOrEqual(this.summary,    rhs.summary) );
        tests.add( nullOrEqual(this.modified,   rhs.modified) );
        tests.add( nullOrEqual(this.url,        rhs.url));
        tests.add( nullOrEqual(this.createdByUrl,  rhs.createdByUrl) );
        tests.add( nullOrEqual(this.assignedToUrl, rhs.assignedToUrl));

        return !tests.contains(false);
    }

}
