# ScalaPB-Reactor

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/kr.ikhoon.scalapb-reactor/scalapb-reactor-codegen_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/kr.ikhoon.scalapb-reactor/scalapb-reactor-codegen_2.12)
[![Build Status](https://github.com/ikhoon/scalapb-reactor/workflows/Build%20Pull%20Requests/badge.svg)](https://github.com/ikhoon/scalapb-reactor/actions?query=workflow%3A%22Build+Pull+Requests%22)

The ScalaPB gRPC generator to run Reactive gRPC server and client on top Project Reactor's Scala extension.

## Highlights

- Uses [Reactive gRPC](https://github.com/salesforce/reactive-grpc) for Java Runtime which provides [Reactive Streams](https://www.reactive-streams.org/)
  on top of gRPC's back-pressure support.
- Uses [Reactor Scala Extensions](https://github.com/reactor/reactor-scala-extensions) for easily building stream requests and responses.

## Installation

Add the following configuration to `project/plugins.sbt`.

```sbt
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.4")
libraryDependencies += "kr.ikhoon.scalapb-reactor" %% "scalapb-reactor-codegen" % "<latest-version>"
```

Add the following configuration to `build.sbt`.

```sbt
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value,
  scalapb.reactor.ReactorCodeGenerator -> (sourceManaged in Compile).value
)
```

ScalaPB-Reactor 0.3.0 currently supports Scala 2.12 and 2.13.
Please use 0.1.0 for Scala 2.11.

## Example

ScalaPB-Reactor generates your stub file to `<package>.<file-name>.Reactor<service-name>`.
The following proto file will be generated to `com.example.test_service.ReactorTestService`.

```proto
// file: test_service.proto

syntax = "proto3";

package com.example;

message Request {
    string in = 1;
}

message Response {
    string out = 1;
}

service TestService {
    rpc Unary(Request) returns (Response);
    rpc ServerStreaming(Request) returns (stream Response);
    rpc ClientStreaming(stream Request) returns (Response);
    rpc BidiStreaming(stream Request) returns (stream Response);
}
```

The generated gRPC stub for Reactor contains the following structure.
```scala
object ReactorTestServiceGrpc {
  // Implement this trait for your service.
  trait ReactorTestService {
    def unary(request: Request): SMono[Response]
    def serverStreaming(request: Request): SFlux[Response]
    def clientStreaming(request: SFlux[Request]): SMono[Response]
    def bidiStreaming(request: SFlux[Request]): SFlux[Response]
  }
  
  object ReactorTestService {
    // Use this method to bind your service with gRPC server
    def bindService(serviceImpl: ReactorTestService): ServerServiceDefinition = {
       ... 
    }
  }
  
  // You use this class for creating gRPC client stub.
  class ReactorTestServiceStub(channel: Channel, options: CallOptions) 
    extends AbstractStub[ReactorTestServiceStub](channel, options) with ReactorTestService {
  }
}
```

Visit [examples](./examples) to find a fully working example.
