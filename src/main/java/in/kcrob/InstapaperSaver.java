package in.kcrob;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;

/**
 * Created by robinchugh on 24/04/17.
 */
public class InstapaperSaver {

    private final static String instapaperAuth = System.getenv("instapaperAuth");

    public boolean save(String link) {
        URL url = null;
        try {
            url = new URL("https://www.instapaper.com/api/add?url="+ URLEncoder.encode(link, "UTF-8"));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            //add authentication header
            con.setRequestProperty("authorization", "Basic "+instapaperAuth);

            int responseCode = con.getResponseCode();

            return responseCode == 201;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}