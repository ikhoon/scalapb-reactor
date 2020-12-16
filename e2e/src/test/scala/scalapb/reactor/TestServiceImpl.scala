package scalapb.reactor

import reactor.core.scala.publisher.{SFlux, SMono}
import scalapb.reactor.test_service.ReactorTestServiceGrpc.ReactorTestService
import scalapb.reactor.test_service.{Request, Response}

class TestServiceImpl extends ReactorTestService {
  override def unary(request: Request): SMono[Response] = {
    SMono.just(Response(s"Hello ${request.in}"))
  }

  override def serverStreaming(request: Request): SFlux[Response] = {
    SFlux
      .range(1, request.in)
      .map(i => s"Hello $i")
      .map(Response(_))
  }

  override def clientStreaming(requests: SFlux[Request]): SMono[Response] = {
    requests.reduce(0)(_ + _.in).map(sum => Response(s"Sum: $sum"))
  }

  override def bidiStreaming(requests: SFlux[Request]): SFlux[Response] = {
    requests.map(req => Response(s"Hello ${req.in}"))
  }
}
