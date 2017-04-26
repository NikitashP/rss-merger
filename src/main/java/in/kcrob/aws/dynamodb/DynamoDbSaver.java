package in.kcrob.aws.dynamodb;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;

/**
 * Created by robinchugh on 25/04/17.
 */
public class DynamoDbSaver {

    private final static String aWSAccessKeyId = System.getenv("aWSAccessKeyId");
    private final static String aWSSecretKey = System.getenv("aWSSecretKey");

    private static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
            .withRegion(Regions.US_WEST_2)
//            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2"))
            .withCredentials(
                    new AWSCredentialsProvider() {
                        @Override
                        public AWSCredentials getCredentials() {
                            return new AWSCredentials() {
                                @Override
                                public String getAWSAccessKeyId() {
                                    return aWSAccessKeyId;
                                }

                                @Override
                                public String getAWSSecretKey() {
                                    return aWSSecretKey;
                                }
                            };
                        }

                        @Override
                        public void refresh() {

                        }
                    }
            )
            .build();
    DynamoDB dynamoDB = new DynamoDB(client);

    public boolean put(String tableName, String primaryKeyName, String key, Long value) {
        Item item = new Item().withPrimaryKey(primaryKeyName, key).withLong("timestamp", value);
        dynamoDB.getTable(tableName).putItem(item);
        return true;
    }

    public Item get(String tableName, String primaryKeyName, String key) {
        return dynamoDB.getTable(tableName).getItem(primaryKeyName, key);
    }
}