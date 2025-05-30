// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: weather.proto
// Protobuf Java Version: 4.30.1

package sr.grpc.gen.event;

public interface WeatherUpdateOrBuilder extends
    // @@protoc_insertion_point(interface_extends:eventsubscription.WeatherUpdate)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * String field (matches criteria.target_identifier)
   * </pre>
   *
   * <code>string city = 1;</code>
   * @return The city.
   */
  java.lang.String getCity();
  /**
   * <pre>
   * String field (matches criteria.target_identifier)
   * </pre>
   *
   * <code>string city = 1;</code>
   * @return The bytes for city.
   */
  com.google.protobuf.ByteString
      getCityBytes();

  /**
   * <pre>
   * Numeric field (double)
   * </pre>
   *
   * <code>double current_temperature_celsius = 2;</code>
   * @return The currentTemperatureCelsius.
   */
  double getCurrentTemperatureCelsius();

  /**
   * <pre>
   * Numeric field (double)
   * </pre>
   *
   * <code>double humidity_percent = 3;</code>
   * @return The humidityPercent.
   */
  double getHumidityPercent();

  /**
   * <pre>
   * Numeric field (int32)
   * </pre>
   *
   * <code>int32 wind_speed_kph = 4;</code>
   * @return The windSpeedKph.
   */
  int getWindSpeedKph();

  /**
   * <pre>
   * Enum field
   * </pre>
   *
   * <code>.eventsubscription.WeatherCondition current_condition = 5;</code>
   * @return The enum numeric value on the wire for currentCondition.
   */
  int getCurrentConditionValue();
  /**
   * <pre>
   * Enum field
   * </pre>
   *
   * <code>.eventsubscription.WeatherCondition current_condition = 5;</code>
   * @return The currentCondition.
   */
  sr.grpc.gen.event.WeatherCondition getCurrentCondition();

  /**
   * <pre>
   * String field
   * </pre>
   *
   * <code>string detailed_description = 6;</code>
   * @return The detailedDescription.
   */
  java.lang.String getDetailedDescription();
  /**
   * <pre>
   * String field
   * </pre>
   *
   * <code>string detailed_description = 6;</code>
   * @return The bytes for detailedDescription.
   */
  com.google.protobuf.ByteString
      getDetailedDescriptionBytes();

  /**
   * <pre>
   * Repeated message field
   * </pre>
   *
   * <code>repeated .eventsubscription.DailyForecast forecast = 7;</code>
   */
  java.util.List<sr.grpc.gen.event.DailyForecast> 
      getForecastList();
  /**
   * <pre>
   * Repeated message field
   * </pre>
   *
   * <code>repeated .eventsubscription.DailyForecast forecast = 7;</code>
   */
  sr.grpc.gen.event.DailyForecast getForecast(int index);
  /**
   * <pre>
   * Repeated message field
   * </pre>
   *
   * <code>repeated .eventsubscription.DailyForecast forecast = 7;</code>
   */
  int getForecastCount();
  /**
   * <pre>
   * Repeated message field
   * </pre>
   *
   * <code>repeated .eventsubscription.DailyForecast forecast = 7;</code>
   */
  java.util.List<? extends sr.grpc.gen.event.DailyForecastOrBuilder> 
      getForecastOrBuilderList();
  /**
   * <pre>
   * Repeated message field
   * </pre>
   *
   * <code>repeated .eventsubscription.DailyForecast forecast = 7;</code>
   */
  sr.grpc.gen.event.DailyForecastOrBuilder getForecastOrBuilder(
      int index);
}
