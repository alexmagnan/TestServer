import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ian on 15-10-03.
 */
public class User {

    /* Static Factory Methods */

    /**
     * Create a list of User objects from a JSONArray
     * @param root
     * @return Created users
     */
    public static List<User> fromJson(JSONArray root) throws IOException {
        List<User> users = new ArrayList<>();
        for(int i = 0; i < root.length(); i++)
            users.add(fromJson(root.getJSONObject(i)));
        return users;
    }

    /**
     * Create a User object from a JSONObject
     * @param root
     * @return
     */
    public static User fromJson(JSONObject root) throws IOException {
        User user = new User();
        // --- GET THE REQUIRED FIELDS --- //
        String name = root.getString("name");
        UserType type= UserType.valueOf(root.getString("userType"));
        if(name == null || type == null) throw new IOException("Missing required fields for JSON user");
        user.setName(name).setUserType(type);
        // ------------------------------- //

        // --- GET THE OPTIONAL FIELDS --- //
        if(!root.isNull("imageUrl")){
            user.setImageUrl(root.getString("imageUrl"));
        };
        // ------------------------------- //

        // extract the post resource URL from the "_links" object
        JSONObject links = root.getJSONObject("_links");
        user.setUrl(links.getJSONObject("self").getString("href"));

        return user;
    }

    /* Fields */

    private String url;
    private String name;
    private String imageUrl;
    private UserType userType;

    /* Getters and Setters */

    public String getUrl() {
        return url;
    }

    public User setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getName() {
        return name;
    }

    public User setName(String name) {
        this.name = name;
        return this;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public User setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public UserType getUserType() {
        return userType;
    }

    public User setUserType(UserType userType) {
        this.userType = userType;
        return this;
    }

    /**
     * Return the JSON representation of the user.
     * @return JSON representatio of the user
     */
    public String toJson() throws IOException {
        StringBuilder sb = new StringBuilder();
        // --- APPEND REQUIRED FIELDS --- //
        if(name == null || userType == null) throw new IOException("Missing required fields for JSON user");
        sb.append("{ \"name\" : \"");
        sb.append(name);
        sb.append("\" , \"userType\": \"");
        sb.append(userType);
        // ------------------------------ //

        // --- APPEND OPTIONAL FIELDS --- //
        if(imageUrl != null){
            sb.append("\" , \"imageUrl\": \"");
            sb.append(imageUrl);
        }
        // ------------------------------ //

        sb.append("\"}");
        return sb.toString();
    }

    /* helper for .equals() */
    private static boolean nullOrEqual(Object o1, Object o2) {
        try {
            // returns true if both o1 and o2 are null, or else they are equal
            // note: if o1 == o2 then o1.equals(o2) by properties of equality.
            return (o1 == o2) || (o1.equals(o2));
        }
        catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object rhsObj) {
        User rhs = (User) rhsObj;
        return nullOrEqual(this.url,  rhs.url)
                && nullOrEqual(this.name, rhs.name)
                && nullOrEqual(this.imageUrl, rhs.imageUrl);
    }
}
