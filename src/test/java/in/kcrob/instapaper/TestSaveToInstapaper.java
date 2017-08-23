package in.kcrob.instapaper;

import in.kcrob.InstapaperSaver;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by robinchugh on 24/04/17.
 */
public class TestSaveToInstapaper{

    @Test
    public void testSave() throws IOException {
        String testUrl = "http://stackoverflow.com/questions/29941376/httpurlconnection-get-call-with-parameters-not-working";
        assert new InstapaperSaver().save2(testUrl);
    }
}