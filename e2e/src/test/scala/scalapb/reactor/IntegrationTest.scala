package scalapb.reactor

import com.linecorp.armeria.client.Clients
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.grpc.GrpcService
import reactor.core.scala.publisher.{SFlux, SMono}
import scala.concurrent.duration._
import scalapb.reactor.test_service.ReactorTestServiceGrpc.{ReactorTestService, ReactorTestServiceStub}
import scalapb.reactor.test_service.{Request, Response}

class IntegrationTest extends munit.FunSuite {
  val resources = FunFixture[(Server, ReactorTestServiceStub)](
    setup = { test =>
      val server =
        Server
          .builder()
          .service(
            GrpcService
              .builder()
              .addService(ReactorTestService.bindService(new TestServiceImpl))
              .build()
          )
          .build()
      server.start().join()
      val uri = s"gproto+http://127.0.0.1:${server.activeLocalPort()}/"
      val client = Clients.newClient(uri, classOf[ReactorTestServiceStub])
      (server, client)
    },
    teardown = { case (server, _) =>
      server.stop().join()
    }
  )

  resources.test("unary") { case (_, client) =>
    val response = client.unary(Request(in = 1)).block()
    assertEquals(response.out, "Hello 1")
  }

  resources.test("serverStream") { case (_, client) =>
    val response = client.serverStreaming(Request(in = 5)).collectSeq().block()
    val expected = (1 to 5).map(i => Response(out = s"Hello $i")).toSeq
    assertEquals(response, expected)
  }

  resources.test("clientStream") { case (_, client) =>
    val response = client
      .clientStreaming(
        SFlux
          .range(1, 10)
          .delayElements(100.millis)
          .map(i => Request(in = i))
      )
      .block()
    assertEquals(response.out, "Sum: 55")
  }

  resources.test("bidiStream") { case (_, client) =>
    val responses = client
      .bidiStreaming(SFlux(1, 2, 3).map(i => Request(in = i)))
      .map(res => res.out)
      .collectSeq()
      .block()
    val expected = (1 to 3).map(i => s"Hello $i")
    assertEquals(responses, expected)
  }
}
