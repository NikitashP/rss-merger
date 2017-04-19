package in.kcrob.testlambda;

/**
 * Created by robin on 02/04/17.
 */

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedOutput;
import in.kcrob.RssMerger;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import com.amazonaws.services.s3.AmazonS3;

public class TestLambda implements RequestHandler<Map<String,Object>, Response> {
    public Response handleRequest(Map<String,Object> input, Context context) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try{
            SyndFeed feed = self.process(Arrays.asList(feedUrls));
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed,new PrintWriter(stream));
        }
        catch(Exception e) {
            System.out.println("Caught exception while processing");
            e.printStackTrace();
        }

        String out = stream.toString();
        System.out.println("returning " + out);
        return new Response(200, out);
    }

    private static RssMerger self = new RssMerger();

    private static String[] feedUrls = {
            "http://comicfeeds.chrisbenard.net/view/dilbert/default",
            "https://jvns.ca/atom.xml",
            "http://rystsov.info/feed.xml",
            "https://martinfowler.com/feed.atom",
            "http://feeds.feedburner.com/UdiDahan-TheSoftwareSimplist",
            "http://www.charlespetzold.com/rss.xml",
            "https://8thlight.com/blog/feed/rss.xml",
            "https://githubengineering.com/atom.xml",
            "https://engineering.linkedin.com/blog.rss",
            "http://antirez.com/rss",
            "http://www.michaelnygard.com/atom.xml",
            "http://psy-lob-saw.blogspot.com/feeds/posts/default?alt=rss",
            "http://brendangregg.com/blog/rss.xml",
            "http://the-paper-trail.org/blog/feed/"
    };


    public static void main (String[] args) {
        System.out.println(new TestLambda().handleRequest(new HashMap<String,Object>(), null));
    }
}