// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: helloworld.proto

package helloworld;

public final class HelloWorldProto {
  private HelloWorldProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_helloworld_IdMessage_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_helloworld_IdMessage_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_helloworld_NameMessage_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_helloworld_NameMessage_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_helloworld_HelloRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_helloworld_HelloRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_helloworld_HelloReply_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_helloworld_HelloReply_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\020helloworld.proto\022\nhelloworld\"\027\n\tIdMess" +
      "age\022\n\n\002id\030\001 \001(\t\"\033\n\013NameMessage\022\014\n\004name\030\001" +
      " \001(\t\"\034\n\014HelloRequest\022\014\n\004name\030\001 \001(\t\"\035\n\nHe" +
      "lloReply\022\017\n\007message\030\001 \001(\t2\206\001\n\007Greeter\022>\n" +
      "\010SayHello\022\030.helloworld.HelloRequest\032\026.he" +
      "lloworld.HelloReply\"\000\022;\n\007GetName\022\025.hello" +
      "world.IdMessage\032\027.helloworld.NameMessage" +
      "\"\000B%\n\nhelloworldB\017HelloWorldProtoP\001\242\002\003HL" +
      "Wb\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_helloworld_IdMessage_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_helloworld_IdMessage_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_helloworld_IdMessage_descriptor,
        new java.lang.String[] { "Id", });
    internal_static_helloworld_NameMessage_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_helloworld_NameMessage_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_helloworld_NameMessage_descriptor,
        new java.lang.String[] { "Name", });
    internal_static_helloworld_HelloRequest_descriptor =
      getDescriptor().getMessageTypes().get(2);
    internal_static_helloworld_HelloRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_helloworld_HelloRequest_descriptor,
        new java.lang.String[] { "Name", });
    internal_static_helloworld_HelloReply_descriptor =
      getDescriptor().getMessageTypes().get(3);
    internal_static_helloworld_HelloReply_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_helloworld_HelloReply_descriptor,
        new java.lang.String[] { "Message", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}