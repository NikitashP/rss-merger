package in.kcrob.testlambda;

/**
 * Created by robin on 02/04/17.
 */

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;

public class TestLambda implements RequestHandler<String, String> {
    public String handleRequest(String request, Context context) {
        String greetingString = String.format("Hello %s %s.", "robin", "chugh");
        return greetingString;
    }

    public static void main (String[] args) {
        System.out.println(new TestLambda().handleRequest("", null));
    }
}
