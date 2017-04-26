package in.kcrob.testlambda;

/**
 * Created by robin on 02/04/17.
 */

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedOutput;
import in.kcrob.InstapaperSaver;
import in.kcrob.RssMerger;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import com.amazonaws.services.s3.AmazonS3;
import in.kcrob.aws.dynamodb.DynamoDbSaver;

public class TestLambda implements RequestHandler<Map<String,Object>, Response> {
    //Constants
    private final static String dynamoDbTableName = "rss-feeds-last-time";
    private final static String dynamoDbPrimaryKeyName = "type";
    private final static String dynamoDbPrimaryKey = "tech";

    //Other Service
    private final static RssMerger self = new RssMerger();
    private final static InstapaperSaver instapperSaver = new InstapaperSaver();
    private final static DynamoDbSaver dynamoDbSaver = new DynamoDbSaver();
    public static final AmazonS3Client s3Client = new AmazonS3Client();

    public Response handleRequest(Map<String,Object> input, Context context) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        SyndFeed feed = null;
        try{
            feed = self.process(Arrays.asList(feedUrls));
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed, new PrintWriter(stream));
        }
        catch(Exception e) {
            System.out.println("Caught exception while processing");
            e.printStackTrace();
        }

        String out = stream.toString();
        System.out.println("returning " + out);

        s3Client.putObject("in.kcrob.rss", "tech", out);

        //Lets try saving to instapaper only the new ones
        if(feed != null) {
            final long timestamp = dynamoDbSaver.get(dynamoDbTableName, dynamoDbPrimaryKeyName, dynamoDbPrimaryKey).getLong("timestamp");
            feed.getEntries().removeIf(entry -> entry.getPublishedDate().getTime() <= timestamp);
            feed.getEntries().sort(new Comparator<SyndEntry>() {
                @Override
                public int compare(SyndEntry o1, SyndEntry o2) {
                    return o1.getPublishedDate().compareTo(o2.getPublishedDate());
                }
            });
            for(SyndEntry entry : feed.getEntries()) {
                System.out.println("Saving to instapaper - " + entry.getLink() + " because it's pubDate is "+ entry.getPublishedDate());
                if(!instapperSaver.save(entry.getLink())){
                    throw new RuntimeException("Could not connect to instapaper");
                }
                dynamoDbSaver.put(dynamoDbTableName, dynamoDbPrimaryKeyName, dynamoDbPrimaryKey, entry.getPublishedDate().getTime());
            }
        }

        return new Response(200, out);
    }

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
            "http://the-paper-trail.org/blog/feed/",
            "https://www.elastic.co/blog/feed",
            "https://databricks.com/feed"
    };

    public static void main (String[] args) {
        System.out.println(new TestLambda().handleRequest(new HashMap<String,Object>(), null));
    }
}