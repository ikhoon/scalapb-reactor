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

import com.google.protobuf.ExtensionRegistry
import protocbridge.Artifact
import protocgen.{CodeGenApp, CodeGenRequest, CodeGenResponse}
import scalapb.compiler._
import scalapb.options.Scalapb

object ReactorCodeGenerator extends CodeGenApp {

  override def registerExtensions(registry: ExtensionRegistry): Unit = {
    Scalapb.registerAllExtensions(registry);
  }

  override def suggestedDependencies: Seq[Artifact] = {
    Seq(
      Artifact(
        "io.projectreactor",
        "reactor-scala-extensions",
        "0.8.0",
        crossVersion = true
      ),
      Artifact(
        "com.salesforce.servicelibs",
        "reactor-grpc-stub",
        "1.0.1"
      ),
      Artifact(
        "com.thesamet.scalapb",
        "scalapb-runtime-grpc",
        scalapb.compiler.Version.scalapbVersion,
        crossVersion = true
      )
    )
  }

  def process(request: CodeGenRequest): CodeGenResponse =
    ProtobufGenerator.parseParameters(request.parameter) match {
      case Right(params) =>
        val implicits =
          new DescriptorImplicits(params, request.allProtos)
        CodeGenResponse.succeed(
          request.filesToGenerate.collect {
            case file if !file.getServices().isEmpty() =>
              new ReactorFilePrinter(implicits, file).result()
          }
        )
      case Left(error) =>
        CodeGenResponse.fail(error)
    }
}
