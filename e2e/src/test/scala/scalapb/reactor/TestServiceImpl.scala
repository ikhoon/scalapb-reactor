/*
 * Copyright 2020 kr.ikhoon.scalapb-reactor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
