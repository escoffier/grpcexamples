package routeguide;

import grpc.health.v1.CheckHealth;
import grpc.health.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;

public class HealthCheckService extends HealthGrpc.HealthImplBase {
    @Override
    public void check(CheckHealth.HealthCheckRequest request, StreamObserver<CheckHealth.HealthCheckResponse> responseObserver) {
        CheckHealth.HealthCheckResponse response = CheckHealth.HealthCheckResponse.newBuilder()
                .setStatus(CheckHealth.HealthCheckResponse.ServingStatus.SERVING).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        //super.check(request, responseObserver);
    }
}
