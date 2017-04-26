package in.kcrob.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.document.Item;
import org.junit.Test;

/**
 * Created by robinchugh on 25/04/17.
 */
public class TestDynamoDbSaver {
    @Test
    public void testSavingLocal() {
        DynamoDbSaver dynamoDbSaver = new DynamoDbSaver();
        dynamoDbSaver.put("rss-feeds-last-time", "type", "tech", 1493037267000L);
        Item item = dynamoDbSaver.get("rss-feeds-last-time", "type", "tech");
        assert item.getLong("timestamp") < System.currentTimeMillis();
    }
}
