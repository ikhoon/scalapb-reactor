package com.example

import com.example.test_service.ReactorTestServiceGrpc.ReactorTestService
import com.example.test_service.{Request, Response}
import reactor.core.scala.publisher.{SFlux, SMono}

class TestServiceImpl extends ReactorTestService {
  override def unary(request: Request): SMono[Response] = {
    SMono.just(Response(s"Hello ${request.in}!"))
  }

  override def serverStreaming(request: Request): SFlux[Response] = {
    SFlux
      .range(1, 5)
      .map(i => s"Hello ${request.in} $i!")
      .map(Response(_))
  }

  override def clientStreaming(requests: SFlux[Request]): SMono[Response] = {
    requests.map(_.in).collectSeq().map(_.mkString(", ")).map(all => Response(s"Hello $all!"))
  }

  override def bidiStreaming(requests: SFlux[Request]): SFlux[Response] = {
    requests.map(req => Response(s"Hello ${req.in}!"))
  }
}
