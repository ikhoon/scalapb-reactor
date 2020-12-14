package test

import com.linecorp.armeria.client.Clients
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.grpc.GrpcService
import reactor.core.scala.publisher.{SFlux, SMono}
import scalapb.reactor.test_service.TestServiceReactorGrpc.{TestService, TestServiceReactorStub}
import scalapb.reactor.test_service.{Request, Response}

object ArmeriaServiceTest {

  // TODO(ikhoon): Use MUnit for testing
  class TestServiceImpl extends TestService {
    override def unary(request: Request): SMono[Response] = {
      SMono.just(Response(s"Hello ${request.in}"))
    }

    override def serverStreaming(request: Request): SFlux[Response] = {
      SFlux.range(0, request.in)
           .map(i => s"Hello $i")
           .map(Response(_))
    }

    override def clientStreaming(request: SFlux[Request]): SMono[Response] = {
      request.reduce(0)(_ + _.in).map(sum => Response(s"Sum: $sum"))
    }

    override def bidiStreaming(request: SFlux[Request]): SFlux[Response] = {
      request.map(i => Response(s"Hello $i"))
    }
  }

  def main(args: Array[String]): Unit = {
    val grpcService = GrpcService.builder()
                             .addService(TestService.bindService(new TestServiceImpl))
                             .build()
    val server = Server.builder()
                       .http(8080)
                       .service(grpcService)
                       .build()
    server.start().join()
  }
}

object ArmeriaClient {
  def main(args: Array[String]): Unit = {
    val client = Clients.newClient("gproto+http://127.0.0.1:8080", classOf[TestServiceReactorStub])
    val res = client.unary(Request(in = 10))
    println(res.block())
  }
}
