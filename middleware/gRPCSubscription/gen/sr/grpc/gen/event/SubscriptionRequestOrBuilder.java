// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: weather.proto
// Protobuf Java Version: 4.30.1

package sr.grpc.gen.event;

public interface SubscriptionRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:eventsubscription.SubscriptionRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Unique identifier FOR THIS SPECIFIC SUBSCRIPTION instance, generated by client.
   * Server uses this to manage the stream and for unsubscription.
   * </pre>
   *
   * <code>string client_subscription_id = 1;</code>
   * @return The clientSubscriptionId.
   */
  java.lang.String getClientSubscriptionId();
  /**
   * <pre>
   * Unique identifier FOR THIS SPECIFIC SUBSCRIPTION instance, generated by client.
   * Server uses this to manage the stream and for unsubscription.
   * </pre>
   *
   * <code>string client_subscription_id = 1;</code>
   * @return The bytes for clientSubscriptionId.
   */
  com.google.protobuf.ByteString
      getClientSubscriptionIdBytes();

  /**
   * <pre>
   * The type of event the client wants to subscribe to.
   * </pre>
   *
   * <code>.eventsubscription.EventType event_type = 2;</code>
   * @return The enum numeric value on the wire for eventType.
   */
  int getEventTypeValue();
  /**
   * <pre>
   * The type of event the client wants to subscribe to.
   * </pre>
   *
   * <code>.eventsubscription.EventType event_type = 2;</code>
   * @return The eventType.
   */
  sr.grpc.gen.event.EventType getEventType();

  /**
   * <pre>
   * The specific criteria for the desired events.
   * </pre>
   *
   * <code>.eventsubscription.SubscriptionCriteria criteria = 3;</code>
   * @return Whether the criteria field is set.
   */
  boolean hasCriteria();
  /**
   * <pre>
   * The specific criteria for the desired events.
   * </pre>
   *
   * <code>.eventsubscription.SubscriptionCriteria criteria = 3;</code>
   * @return The criteria.
   */
  sr.grpc.gen.event.SubscriptionCriteria getCriteria();
  /**
   * <pre>
   * The specific criteria for the desired events.
   * </pre>
   *
   * <code>.eventsubscription.SubscriptionCriteria criteria = 3;</code>
   */
  sr.grpc.gen.event.SubscriptionCriteriaOrBuilder getCriteriaOrBuilder();
}
