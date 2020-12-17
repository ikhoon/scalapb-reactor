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

import com.example.{Main, TestServiceImpl}
import com.example.test_service.ReactorTestServiceGrpc.{ReactorTestService, ReactorTestServiceStub}
import com.example.test_service.{Request, Response}
import com.linecorp.armeria.client.Clients
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.grpc.GrpcService
import reactor.core.scala.publisher.SFlux
import scala.concurrent.duration._

class TestServiceTest extends munit.FunSuite {
  val resources = FunFixture[(Server, ReactorTestServiceStub)](
    setup = { test =>
      val server = Main.newServer(8080, 8443)
      server.start().join()
      val uri = s"gproto+http://127.0.0.1:${server.activeLocalPort()}/"
      val client = Clients.newClient(uri, classOf[ReactorTestServiceStub])
      (server, client)
    },
    teardown = { case (server, _) =>
      server.stop().join()
    }
  )

  val message = "ScalaPB with Reactor"
  resources.test("unary") { case (_, client) =>
    val response = client.unary(Request(message)).block()
    assertEquals(response.out, s"Hello $message!")
  }

  resources.test("serverStream") { case (_, client) =>
    val response = client.serverStreaming(Request(message)).collectSeq().block()
    val expected = (1 to 5).map(i => Response(out = s"Hello $message $i!"))
    assertEquals(response, expected)
  }

  resources.test("clientStream") { case (_, client) =>
    val response = client
      .clientStreaming(
        SFlux
          .range(1, 5)
          .delayElements(100.millis)
          .map(i => Request(i.toString))
      )
      .block()
    assertEquals(response.out, "Hello 1, 2, 3, 4, 5!")
  }

  resources.test("bidiStream") { case (_, client) =>
    val responses = client
      .bidiStreaming(SFlux(1, 2, 3).map(i => Request(i.toString)))
      .map(res => res.out)
      .collectSeq()
      .block()
    val expected = (1 to 3).map(i => s"Hello $i!")
    assertEquals(responses, expected)
  }
}
