package sr.grpc.gen.event;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * The gRPC service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.71.0)",
    comments = "Source: weather.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class EventSubscriptionServiceGrpc {

  private EventSubscriptionServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "eventsubscription.EventSubscriptionService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<sr.grpc.gen.event.SubscriptionRequest,
      sr.grpc.gen.event.EventNotification> getSubscribeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Subscribe",
      requestType = sr.grpc.gen.event.SubscriptionRequest.class,
      responseType = sr.grpc.gen.event.EventNotification.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<sr.grpc.gen.event.SubscriptionRequest,
      sr.grpc.gen.event.EventNotification> getSubscribeMethod() {
    io.grpc.MethodDescriptor<sr.grpc.gen.event.SubscriptionRequest, sr.grpc.gen.event.EventNotification> getSubscribeMethod;
    if ((getSubscribeMethod = EventSubscriptionServiceGrpc.getSubscribeMethod) == null) {
      synchronized (EventSubscriptionServiceGrpc.class) {
        if ((getSubscribeMethod = EventSubscriptionServiceGrpc.getSubscribeMethod) == null) {
          EventSubscriptionServiceGrpc.getSubscribeMethod = getSubscribeMethod =
              io.grpc.MethodDescriptor.<sr.grpc.gen.event.SubscriptionRequest, sr.grpc.gen.event.EventNotification>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Subscribe"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  sr.grpc.gen.event.SubscriptionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  sr.grpc.gen.event.EventNotification.getDefaultInstance()))
              .setSchemaDescriptor(new EventSubscriptionServiceMethodDescriptorSupplier("Subscribe"))
              .build();
        }
      }
    }
    return getSubscribeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<sr.grpc.gen.event.UnsubscriptionRequest,
      sr.grpc.gen.event.UnsubscriptionResponse> getUnsubscribeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Unsubscribe",
      requestType = sr.grpc.gen.event.UnsubscriptionRequest.class,
      responseType = sr.grpc.gen.event.UnsubscriptionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<sr.grpc.gen.event.UnsubscriptionRequest,
      sr.grpc.gen.event.UnsubscriptionResponse> getUnsubscribeMethod() {
    io.grpc.MethodDescriptor<sr.grpc.gen.event.UnsubscriptionRequest, sr.grpc.gen.event.UnsubscriptionResponse> getUnsubscribeMethod;
    if ((getUnsubscribeMethod = EventSubscriptionServiceGrpc.getUnsubscribeMethod) == null) {
      synchronized (EventSubscriptionServiceGrpc.class) {
        if ((getUnsubscribeMethod = EventSubscriptionServiceGrpc.getUnsubscribeMethod) == null) {
          EventSubscriptionServiceGrpc.getUnsubscribeMethod = getUnsubscribeMethod =
              io.grpc.MethodDescriptor.<sr.grpc.gen.event.UnsubscriptionRequest, sr.grpc.gen.event.UnsubscriptionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Unsubscribe"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  sr.grpc.gen.event.UnsubscriptionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  sr.grpc.gen.event.UnsubscriptionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new EventSubscriptionServiceMethodDescriptorSupplier("Unsubscribe"))
              .build();
        }
      }
    }
    return getUnsubscribeMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static EventSubscriptionServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<EventSubscriptionServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<EventSubscriptionServiceStub>() {
        @java.lang.Override
        public EventSubscriptionServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new EventSubscriptionServiceStub(channel, callOptions);
        }
      };
    return EventSubscriptionServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static EventSubscriptionServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<EventSubscriptionServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<EventSubscriptionServiceBlockingV2Stub>() {
        @java.lang.Override
        public EventSubscriptionServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new EventSubscriptionServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return EventSubscriptionServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static EventSubscriptionServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<EventSubscriptionServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<EventSubscriptionServiceBlockingStub>() {
        @java.lang.Override
        public EventSubscriptionServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new EventSubscriptionServiceBlockingStub(channel, callOptions);
        }
      };
    return EventSubscriptionServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static EventSubscriptionServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<EventSubscriptionServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<EventSubscriptionServiceFutureStub>() {
        @java.lang.Override
        public EventSubscriptionServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new EventSubscriptionServiceFutureStub(channel, callOptions);
        }
      };
    return EventSubscriptionServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * The gRPC service definition.
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Subscribe to events. Server streams notifications.
     * </pre>
     */
    default void subscribe(sr.grpc.gen.event.SubscriptionRequest request,
        io.grpc.stub.StreamObserver<sr.grpc.gen.event.EventNotification> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSubscribeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unsubscribe from events. Unary call.
     * </pre>
     */
    default void unsubscribe(sr.grpc.gen.event.UnsubscriptionRequest request,
        io.grpc.stub.StreamObserver<sr.grpc.gen.event.UnsubscriptionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnsubscribeMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service EventSubscriptionService.
   * <pre>
   * The gRPC service definition.
   * </pre>
   */
  public static abstract class EventSubscriptionServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return EventSubscriptionServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service EventSubscriptionService.
   * <pre>
   * The gRPC service definition.
   * </pre>
   */
  public static final class EventSubscriptionServiceStub
      extends io.grpc.stub.AbstractAsyncStub<EventSubscriptionServiceStub> {
    private EventSubscriptionServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected EventSubscriptionServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new EventSubscriptionServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Subscribe to events. Server streams notifications.
     * </pre>
     */
    public void subscribe(sr.grpc.gen.event.SubscriptionRequest request,
        io.grpc.stub.StreamObserver<sr.grpc.gen.event.EventNotification> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getSubscribeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Unsubscribe from events. Unary call.
     * </pre>
     */
    public void unsubscribe(sr.grpc.gen.event.UnsubscriptionRequest request,
        io.grpc.stub.StreamObserver<sr.grpc.gen.event.UnsubscriptionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnsubscribeMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service EventSubscriptionService.
   * <pre>
   * The gRPC service definition.
   * </pre>
   */
  public static final class EventSubscriptionServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<EventSubscriptionServiceBlockingV2Stub> {
    private EventSubscriptionServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected EventSubscriptionServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new EventSubscriptionServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Subscribe to events. Server streams notifications.
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, sr.grpc.gen.event.EventNotification>
        subscribe(sr.grpc.gen.event.SubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getSubscribeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unsubscribe from events. Unary call.
     * </pre>
     */
    public sr.grpc.gen.event.UnsubscriptionResponse unsubscribe(sr.grpc.gen.event.UnsubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnsubscribeMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service EventSubscriptionService.
   * <pre>
   * The gRPC service definition.
   * </pre>
   */
  public static final class EventSubscriptionServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<EventSubscriptionServiceBlockingStub> {
    private EventSubscriptionServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected EventSubscriptionServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new EventSubscriptionServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Subscribe to events. Server streams notifications.
     * </pre>
     */
    public java.util.Iterator<sr.grpc.gen.event.EventNotification> subscribe(
        sr.grpc.gen.event.SubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getSubscribeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Unsubscribe from events. Unary call.
     * </pre>
     */
    public sr.grpc.gen.event.UnsubscriptionResponse unsubscribe(sr.grpc.gen.event.UnsubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnsubscribeMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service EventSubscriptionService.
   * <pre>
   * The gRPC service definition.
   * </pre>
   */
  public static final class EventSubscriptionServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<EventSubscriptionServiceFutureStub> {
    private EventSubscriptionServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected EventSubscriptionServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new EventSubscriptionServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Unsubscribe from events. Unary call.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<sr.grpc.gen.event.UnsubscriptionResponse> unsubscribe(
        sr.grpc.gen.event.UnsubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnsubscribeMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SUBSCRIBE = 0;
  private static final int METHODID_UNSUBSCRIBE = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SUBSCRIBE:
          serviceImpl.subscribe((sr.grpc.gen.event.SubscriptionRequest) request,
              (io.grpc.stub.StreamObserver<sr.grpc.gen.event.EventNotification>) responseObserver);
          break;
        case METHODID_UNSUBSCRIBE:
          serviceImpl.unsubscribe((sr.grpc.gen.event.UnsubscriptionRequest) request,
              (io.grpc.stub.StreamObserver<sr.grpc.gen.event.UnsubscriptionResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSubscribeMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              sr.grpc.gen.event.SubscriptionRequest,
              sr.grpc.gen.event.EventNotification>(
                service, METHODID_SUBSCRIBE)))
        .addMethod(
          getUnsubscribeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              sr.grpc.gen.event.UnsubscriptionRequest,
              sr.grpc.gen.event.UnsubscriptionResponse>(
                service, METHODID_UNSUBSCRIBE)))
        .build();
  }

  private static abstract class EventSubscriptionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    EventSubscriptionServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return sr.grpc.gen.event.EventSubscriptionProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("EventSubscriptionService");
    }
  }

  private static final class EventSubscriptionServiceFileDescriptorSupplier
      extends EventSubscriptionServiceBaseDescriptorSupplier {
    EventSubscriptionServiceFileDescriptorSupplier() {}
  }

  private static final class EventSubscriptionServiceMethodDescriptorSupplier
      extends EventSubscriptionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    EventSubscriptionServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (EventSubscriptionServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new EventSubscriptionServiceFileDescriptorSupplier())
              .addMethod(getSubscribeMethod())
              .addMethod(getUnsubscribeMethod())
              .build();
        }
      }
    }
    return result;
  }
}
