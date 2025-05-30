// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: weather.proto
// Protobuf Java Version: 4.30.1

package sr.grpc.gen.event;

public interface ConcertAlertOrBuilder extends
    // @@protoc_insertion_point(interface_extends:eventsubscription.ConcertAlert)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Matches criteria.target_identifier
   * </pre>
   *
   * <code>string artist = 1;</code>
   * @return The artist.
   */
  java.lang.String getArtist();
  /**
   * <pre>
   * Matches criteria.target_identifier
   * </pre>
   *
   * <code>string artist = 1;</code>
   * @return The bytes for artist.
   */
  com.google.protobuf.ByteString
      getArtistBytes();

  /**
   * <code>string venue = 2;</code>
   * @return The venue.
   */
  java.lang.String getVenue();
  /**
   * <code>string venue = 2;</code>
   * @return The bytes for venue.
   */
  com.google.protobuf.ByteString
      getVenueBytes();

  /**
   * <code>string city = 3;</code>
   * @return The city.
   */
  java.lang.String getCity();
  /**
   * <code>string city = 3;</code>
   * @return The bytes for city.
   */
  com.google.protobuf.ByteString
      getCityBytes();

  /**
   * <code>int64 event_date_unix_seconds = 4;</code>
   * @return The eventDateUnixSeconds.
   */
  long getEventDateUnixSeconds();

  /**
   * <code>int32 tickets_available = 5;</code>
   * @return The ticketsAvailable.
   */
  int getTicketsAvailable();

  /**
   * <pre>
   * Repeated string
   * </pre>
   *
   * <code>repeated string ticket_links = 6;</code>
   * @return A list containing the ticketLinks.
   */
  java.util.List<java.lang.String>
      getTicketLinksList();
  /**
   * <pre>
   * Repeated string
   * </pre>
   *
   * <code>repeated string ticket_links = 6;</code>
   * @return The count of ticketLinks.
   */
  int getTicketLinksCount();
  /**
   * <pre>
   * Repeated string
   * </pre>
   *
   * <code>repeated string ticket_links = 6;</code>
   * @param index The index of the element to return.
   * @return The ticketLinks at the given index.
   */
  java.lang.String getTicketLinks(int index);
  /**
   * <pre>
   * Repeated string
   * </pre>
   *
   * <code>repeated string ticket_links = 6;</code>
   * @param index The index of the value to return.
   * @return The bytes of the ticketLinks at the given index.
   */
  com.google.protobuf.ByteString
      getTicketLinksBytes(int index);
}
