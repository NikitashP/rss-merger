package in.kcrob.testlambda;

/**
 * Created by robin on 02/04/17.
 */

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.SyndFeedOutput;
import com.rometools.rome.io.XmlReader;
import in.kcrob.InstapaperSaver;
import in.kcrob.RssMerger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import com.amazonaws.services.s3.AmazonS3;
import in.kcrob.aws.dynamodb.DynamoDbSaver;
import jersey.repackaged.com.google.common.collect.Iterators;

public class TestLambda implements RequestHandler<Map<String,Object>, Response> {
    //Constants
    private final static String dynamoDbTableName = "rss-feeds-last-time";
    private final static String dynamoDbPrimaryKeyName = "type";
    private final static String dynamoDbPrimaryKey = "tech";

    //Other Service
    private final static RssMerger self = new RssMerger();
    private final static InstapaperSaver instapperSaver = new InstapaperSaver();
    private final static DynamoDbSaver dynamoDbSaver = new DynamoDbSaver();
    private static final AmazonS3Client s3Client = new AmazonS3Client();

    private final static String s3BucketName = "in.kcrob.rss";
    private final static String s3FileName = "tech";

    public Response handleRequest(Map<String,Object> input, Context context) {
        return handleRequestNew(input, context);
    }

    private Response handleRequestNew(Map<String,Object> input, Context context) {
        try {
            final Map<String, SyndFeed> feedMap = self.collectFeeds(Arrays.asList(feedUrls));
            feedMap.forEach((feedUrl, newFeed) -> {

                final Iterable<String> newLinks = getDiffFromOld(feedUrl, newFeed);


                if(newLinks != null ){ //Null means we caught an exception somewhere, so nothing to do.

                    if(Iterators.size(newLinks.iterator()) > 0) {
                        newLinks.forEach(instapperSaver::save1);
                        newLinks.forEach(instapperSaver::save2);
                        try {
                            System.out.println("Putting to S3");
                            final String feedXml = feedToXml(newFeed);
                            s3Client.putObject(s3BucketName, feedUrl, feedXml);
                        } catch (IOException | FeedException e) {
                            e.printStackTrace();

                        }
                    }
                    else{
                        System.out.println("No Diff, doing nothing");
                    }
                }

            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new Response(200, "success");
    }

    private String feedToXml(SyndFeed newFeed) throws IOException, FeedException {

        SyndFeedOutput output = new SyndFeedOutput();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        output.output(newFeed, new PrintWriter(stream));

        return stream.toString();
    }

    private Iterable<String> getDiffFromOld(String feedUrl, SyndFeed newFeed) {
        System.out.println("Processing " + feedUrl);
        final HashSet<String> newUrls = new HashSet<>();
        newFeed.getEntries().forEach((entry) -> newUrls.add(entry.getLink().trim()));

        if(s3Client.doesObjectExist(s3BucketName, feedUrl)) {
            final HashSet<String> oldUrls = new HashSet<>();

            final S3Object object = s3Client.getObject(s3BucketName, feedUrl);
            final S3ObjectInputStream objectContent = object.getObjectContent();
            SyndFeedInput temp = new SyndFeedInput();
            try {
                final SyndFeed oldFeed = temp.build(new XmlReader(objectContent));
                oldFeed.getEntries().forEach((entry) -> oldUrls.add(entry.getLink().trim()));

                newUrls.forEach(url -> System.out.println("Feed - " + feedUrl + "NEW - " + url));
                oldUrls.forEach(url -> System.out.println("Feed - " + feedUrl + "OLD - " + url));

                newUrls.removeAll(oldUrls);

                newUrls.forEach(url -> System.out.println("Feed - " + feedUrl + "DIFF - " + url));

                return newUrls;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            System.out.println("Looks like an introduction of new feed, simply putting it in S3");
            return Collections.emptyList();
        }

        return null;
    }

    private Response handleRequestOld(Map<String,Object> input, Context context) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        SyndFeed newFeed = null;
        try{
            newFeed = self.process(Arrays.asList(feedUrls));
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(newFeed, new PrintWriter(stream));
        }
        catch(Exception e) {
            System.out.println("Caught exception while processing");
            e.printStackTrace();
        }

        String out = stream.toString();

        //Lets try saving to instapaper only the new ones
        if(newFeed != null) {

            //Lets get current newFeed
            final S3Object object = s3Client.getObject(s3BucketName, s3FileName);
            final S3ObjectInputStream objectContent = object.getObjectContent();
            SyndFeedInput temp = new SyndFeedInput();
            try {
                final SyndFeed oldFeed = temp.build(new XmlReader(objectContent));
                final HashSet<String> newUrls = new HashSet<>();
                final HashSet<String> oldUrls = new HashSet<>();

                for(SyndEntry entry : newFeed.getEntries()) {
                    newUrls.add(entry.getLink());
                }

                for(SyndEntry entry : oldFeed.getEntries()) {
                    oldUrls.add(entry.getLink());
                }

                newUrls.forEach(url -> System.out.println("NEW - " + url));
                oldUrls.forEach(url -> System.out.println("OLD - " + url));
                newUrls.removeAll(oldUrls);
                newUrls.forEach(url -> System.out.println("DIFF - " + url));
                for(String url: newUrls) {
                    System.out.println("Saving to instapaper - " + url);
                    if(!instapperSaver.save1(url)){
                        throw new RuntimeException("Could not connect to instapaper");
                    }
                }
                s3Client.putObject(s3BucketName, s3FileName, out);
            } catch (FeedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
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
            "https://databricks.com/feed",
            "http://www.grpc.io/feed.xml",
            "https://www.headspace.com/blog/feed/",
            "http://feeds.feedburner.com/brainyquote/QUOTEBR"
    };

    public static void main (String[] args) {
        System.out.println(new TestLambda().handleRequest(new HashMap<String,Object>(), null));
    }
}