package in.kcrob.testlambda;

/**
 * Created by robin on 02/04/17.
 */
public class Response {
    private Integer statusCode;
    private String body;

    public Response(Integer statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }
}