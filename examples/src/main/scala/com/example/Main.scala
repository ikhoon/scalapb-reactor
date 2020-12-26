package com.example

import com.example.test_service.ReactorTestServiceGrpc.ReactorTestService
import com.example.test_service.{ReactorTestServiceGrpc, Request}
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.docs.{DocService, DocServiceFilter}
import com.linecorp.armeria.server.grpc.GrpcService
import com.linecorp.armeria.server.logging.LoggingService
import io.grpc.reflection.v1alpha.ServerReflectionGrpc
import org.slf4j.LoggerFactory

object Main {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val server = newServer(8080, 8443)
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = {
        server.stop().join()
        logger.info("Server has been stopped.")
      }
    }))
    server.start().join()
    logger.info("Server has been started. Serving DocService at http://127.0.0.1:{}/docs", server.activeLocalPort)

  }

  def newServer(httpPort: Int, httpsPort: Int): Server = {
    val exampleRequest = Request("Scalapb with Reactor")
    val grpcService =
      GrpcService
        .builder()
        .addService(ReactorTestService.bindService(new TestServiceImpl))
        .supportedSerializationFormats(GrpcSerializationFormats.values)
        .enableUnframedRequests(true)
        .build()

    val serviceName = ReactorTestServiceGrpc.SERVICE.getName
    Server
      .builder()
      .http(httpPort)
      .https(httpsPort)
      .tlsSelfSigned()
      .decorator(LoggingService.newDecorator())
      .service(grpcService)
      .build()
  }
}
