package app.nymbl.customresource;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeApplicationsResponse;
import software.amazon.awssdk.services.elasticbeanstalk.waiters.ElasticBeanstalkWaiter;
import software.amazon.lambda.powertools.cloudformation.AbstractCustomResourceHandler;
import software.amazon.lambda.powertools.cloudformation.Response;

import java.util.Objects;
import java.util.Optional;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */

public class App extends AbstractCustomResourceHandler {
    private final static Logger log = LogManager.getLogger(App.class);
    private final ElasticBeanstalkClient elasticBeanstalkClient;

    public App() {
        super();
        elasticBeanstalkClient  = ElasticBeanstalkClient.builder().httpClientBuilder(ApacheHttpClient.builder()).build();
    }

    /**
     * This method is invoked when CloudFormation Creates the Custom Resource.
     * In this example, the method creates an Amazon S3 Bucket with the provided `ApplicationName`
     *
     * @param cloudFormationCustomResourceEvent Create Event from CloudFormation
     * @param context                           Lambda Context
     * @return Response to send to CloudFormation
     */
    @Override
    protected Response create(CloudFormationCustomResourceEvent cloudFormationCustomResourceEvent, Context context) {
        // Validate the CloudFormation Custom Resource event
        Objects.requireNonNull(cloudFormationCustomResourceEvent, "cloudFormationCustomResourceEvent cannot be null.");
        Objects.requireNonNull(cloudFormationCustomResourceEvent.getResourceProperties().get("ApplicationName"),
                "ApplicationName cannot be null.");

        log.info(cloudFormationCustomResourceEvent);
        String physicalResourceId = (String) cloudFormationCustomResourceEvent.getResourceProperties().get("ApplicationName");
        log.info("Application Name {}", physicalResourceId);
        try {
            // Get the application
            Optional<String> optionalApp = getApplication(physicalResourceId);
            return optionalApp.isPresent()
                ? optionalApp.get().matches(physicalResourceId)
                    ? Response.builder()
                        .noEcho(false)
                        .physicalResourceId(physicalResourceId)
                        .status(Response.Status.SUCCESS)
                        .value(Map.of("Application", physicalResourceId))
                        .build()
                    : Response.builder()
                        .noEcho(false)
                        .physicalResourceId(physicalResourceId)
                        .status(Response.Status.FAILED)
                        .value(Map.of("Application", physicalResourceId))
                        .build()
                : Response.failed(physicalResourceId);
        } catch (AwsServiceException | SdkClientException e) {
            // In case of error, return a failed response, with the bucketName as the physicalResourceId
            log.error(e);
            return Response.failed(physicalResourceId);
        }
    }

    /**
     * This method is invoked when CloudFormation Updates the Custom Resource.
     * In this example, the method creates an Amazon S3 Bucket with the provided `BucketName`, if the `BucketName` differs from the previous `BucketName` (for initial creation)
     *
     * @param cloudFormationCustomResourceEvent Update Event from CloudFormation
     * @param context                           Lambda Context
     * @return Response to send to CloudFormation
     */
    @Override
    protected Response update(CloudFormationCustomResourceEvent cloudFormationCustomResourceEvent, Context context) {
        // Validate the CloudFormation Custom Resource event
        Objects.requireNonNull(cloudFormationCustomResourceEvent, "cloudFormationCustomResourceEvent cannot be null.");
        Objects.requireNonNull(cloudFormationCustomResourceEvent.getResourceProperties().get("ApplicationName"),
                "ApplicationName cannot be null.");

        log.info(cloudFormationCustomResourceEvent);
        String physicalResourceId = cloudFormationCustomResourceEvent.getPhysicalResourceId();
        log.info("Physical Resource ID {}", physicalResourceId);
        return Response.failed(physicalResourceId);
    }

    /**
     * This method is invoked when CloudFormation Deletes the Custom Resource.
     * NOTE: CloudFormation will DELETE a resource, if during the UPDATE a new physicalResourceId is returned.
     * Refer to the <a href="https://docs.powertools.aws.dev/lambda/java/utilities/custom_resources/#understanding-the-cloudformation-custom-resource-lifecycle">Powertools Java Documentation</a> for more details.
     *
     * @param cloudFormationCustomResourceEvent Delete Event from CloudFormation
     * @param context                           Lambda Context
     * @return Response to send to CloudFormation
     */
    @Override
    protected Response delete(CloudFormationCustomResourceEvent cloudFormationCustomResourceEvent, Context context) {
        // Validate the CloudFormation Custom Resource event
        Objects.requireNonNull(cloudFormationCustomResourceEvent, "cloudFormationCustomResourceEvent cannot be null.");
        Objects.requireNonNull(cloudFormationCustomResourceEvent.getPhysicalResourceId(),
                "PhysicalResourceId cannot be null.");

        log.info(cloudFormationCustomResourceEvent);
        // Get the physicalResourceId. physicalResourceId is the value provided to CloudFormation in the Create request.
        String applicationName = cloudFormationCustomResourceEvent.getPhysicalResourceId();
        log.info("ApplicationName Name {}", applicationName);
        return Response.success(applicationName);
        // Check if a bucket with bucketName exists
        // if (bucketExists(bucketName)) {
        //     try {
        //         // If it exists, delete the bucket
        //         s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
        //         log.info("Bucket Deleted {}", bucketName);
        //         // Return a successful response with bucketName as the physicalResourceId
        //         return Response.success(bucketName);
        //     } catch (AwsServiceException | SdkClientException e) {
        //         // Return a failed response in case of errors during the bucket deletion
        //         log.error(e);
        //         return Response.failed(bucketName);
        //     }
        // } else {
        //     // If the bucket does not exist, return a successful response with the bucketName as the physicalResourceId
        //     log.info("Bucket already deleted - no action");
        //     return Response.success(bucketName);
        // }

    }

    // private boolean bucketExists(String bucketName) {
    //     try {
    //         HeadBucketResponse headBucketResponse =
    //                 s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
    //         if (headBucketResponse.sdkHttpResponse().isSuccessful()) {
    //             return true;
    //         }
    //     } catch (NoSuchBucketException e) {
    //         log.info("Bucket does not exist");
    //         return false;   return false;
    // }
    //     }
    //  

    private Optional<String> getApplication(String applicationName) {
        // ElasticBeanstalkWaiter waiter = elasticBeanstalkClient.waiter();
        DescribeApplicationsRequest describeApplicationsRequest = DescribeApplicationsRequest.builder().applicationNames(applicationName).build();
        DescribeApplicationsResponse describeApplicationsResponse = elasticBeanstalkClient.describeApplications(describeApplicationsRequest);
        // WaiterResponse<HeadBucketResponse> waiterResponse =
        //         waiter.waitUntilBucketExists(HeadBucketRequest.builder().bucket(bucketName).build());
        // waiterResponse.matched().response().ifPresent(log::info);
        if (describeApplicationsResponse.hasApplications() && !describeApplicationsResponse.applications().isEmpty()) {
            String application = describeApplicationsResponse.applications().get(0).applicationName();
            log.info("Application Found {}", application);
            return Optional.of(application);
        } else {
            return Optional.empty();
        }
    }
}
