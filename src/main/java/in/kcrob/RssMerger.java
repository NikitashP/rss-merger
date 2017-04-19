package in.kcrob;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.SyndFeedOutput;
import com.rometools.rome.io.XmlReader;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;


import com.amazonaws.services.lambda.runtime.Context;

/**
 * Created by robin on 29/03/17.
 */

public class RssMerger {

    private final ExecutorService executor = Executors.newFixedThreadPool(100);
    private final SyndEntryComparator syndEntryComparator = new SyndEntryComparator();
    private final int individualFeedConnectWaitTime = Integer.parseInt(System.getenv("individualFeedConnectWaitTime"));
    private final int individualFeedReadWaitTime = Integer.parseInt(System.getenv("individualFeedReadWaitTime"));
    private final int allFeedsWaitTime = individualFeedConnectWaitTime + individualFeedReadWaitTime + 100;
    private final String outputFeedType = "rss_2.0";
    private final int maxElementsPerFeed = 5;
    private final int maxElementsFinalFeed = 25;

    private Iterable<SyndFeed> collectFeeds(Iterable<String> feedUrls) throws InterruptedException {
        Queue<SyndFeed> feeds = new ConcurrentLinkedQueue<SyndFeed>();

        for(String url : feedUrls) {
            executor.execute(new FeedGetter(url, feeds));
        }

        Thread.sleep(allFeedsWaitTime);

        return feeds;
    }

    public SyndFeed process(Iterable<String> feedUrls) throws InterruptedException {
        Iterable<SyndFeed> inputFeeds = collectFeeds(feedUrls);

        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(outputFeedType);

        feed.setTitle("Aggregating Feeds by kcrob.in");
        feed.setDescription("kcrob.in Aggregated Feed");
        feed.setAuthor("kcrobin");
        feed.setLink("http://kcrob.in");

        List<SyndEntry> entries = new ArrayList<SyndEntry>();

        for (SyndFeed inputFeed: inputFeeds) {
            List<SyndEntry> inputEntries = inputFeed.getEntries();
            inputEntries.removeIf(
                    syndEntry -> syndEntry.getPublishedDate() == null && syndEntry.getUpdatedDate() == null
            );
            inputEntries.sort(syndEntryComparator);
            if(inputEntries.size() > maxElementsPerFeed) {
                inputEntries = inputEntries.subList(0, maxElementsPerFeed);
            }
            entries.addAll(inputEntries);
        }

        entries.sort(syndEntryComparator);
        if(entries.size() > maxElementsFinalFeed) {
            entries = entries.subList(0, maxElementsFinalFeed);
        }
        feed.setEntries(entries);
        return feed;
    }

    private void shutdown() {
        executor.shutdown();
    }

    private class FeedGetter implements Runnable {
        private final String url;
        private final Queue<SyndFeed> feeds;

        FeedGetter(String url, Queue<SyndFeed> feeds) {
            this.url = url;
            this.feeds = feeds;
        }

        @Override
        public void run() {
            HttpURLConnection httpcon = null;
            try {
                URL url = new URL(this.url);
                httpcon = (HttpURLConnection) url.openConnection();
                httpcon.setConnectTimeout(individualFeedConnectWaitTime);
                httpcon.setReadTimeout(individualFeedReadWaitTime);
                // Reading the feed
                SyndFeedInput input = new SyndFeedInput();
                System.out.println("Getting output of " + url);
                feeds.add(input.build(new XmlReader(httpcon)));
                System.out.println("Got output of " + url);
            }
            catch(Exception ignored) {
                System.out.println(ignored);
            }
            finally {
                if(httpcon != null) {
                    httpcon.disconnect();
                }
            }
        }
    }

    private class SyndEntryComparator implements Comparator<SyndEntry> {

        @Override
        public int compare(SyndEntry o1, SyndEntry o2) {
            if(o1.getPublishedDate() == null) {
                o1.setPublishedDate(o1.getUpdatedDate());
            }
            if(o2.getPublishedDate() == null) {
                o2.setPublishedDate(o2.getUpdatedDate());
            }
            return o2.getPublishedDate().compareTo(o1.getPublishedDate());
        }
    }

    /*
    To Run it
     */
    public static void main(String[] args) throws Exception {
        RssMerger self = new RssMerger();
        String[] feedUrls = {
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
        try{
            SyndFeed feed = self.process(Arrays.asList(feedUrls));
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed,new PrintWriter(System.out));
        }
        catch(Exception e) {
            System.out.println("Caught exception while processing");
        }
        finally {
            self.shutdown();
        }
    }
}
