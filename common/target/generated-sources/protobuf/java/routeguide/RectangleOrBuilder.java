// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: route_guide.proto

package routeguide;

public interface RectangleOrBuilder extends
    // @@protoc_insertion_point(interface_extends:routeguide.Rectangle)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * One corner of the rectangle.
   * </pre>
   *
   * <code>.routeguide.Point lo = 1;</code>
   * @return Whether the lo field is set.
   */
  boolean hasLo();
  /**
   * <pre>
   * One corner of the rectangle.
   * </pre>
   *
   * <code>.routeguide.Point lo = 1;</code>
   * @return The lo.
   */
  routeguide.Point getLo();
  /**
   * <pre>
   * One corner of the rectangle.
   * </pre>
   *
   * <code>.routeguide.Point lo = 1;</code>
   */
  routeguide.PointOrBuilder getLoOrBuilder();

  /**
   * <pre>
   * The other corner of the rectangle.
   * </pre>
   *
   * <code>.routeguide.Point hi = 2;</code>
   * @return Whether the hi field is set.
   */
  boolean hasHi();
  /**
   * <pre>
   * The other corner of the rectangle.
   * </pre>
   *
   * <code>.routeguide.Point hi = 2;</code>
   * @return The hi.
   */
  routeguide.Point getHi();
  /**
   * <pre>
   * The other corner of the rectangle.
   * </pre>
   *
   * <code>.routeguide.Point hi = 2;</code>
   */
  routeguide.PointOrBuilder getHiOrBuilder();
}