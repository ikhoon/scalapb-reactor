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

import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorRequest, CodeGeneratorResponse}
import protocbridge.{Artifact, ProtocCodeGenerator}
import scala.jdk.CollectionConverters._
import scalapb.compiler._
import scalapb.options.compiler.Scalapb

object ReactorCodeGenerator extends ProtocCodeGenerator {

  override def run(req: Array[Byte]): Array[Byte] = {
    val registry = ExtensionRegistry.newInstance()
    Scalapb.registerAllExtensions(registry)
    val request = CodeGeneratorRequest.parseFrom(req, registry)
    process(request).toByteArray
  }

  override def suggestedDependencies: Seq[Artifact] = {
    Seq(
      Artifact(
        "io.projectreactor",
        "reactor-scala-extensions",
        "0.7.1",
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

  def process(request: CodeGeneratorRequest): CodeGeneratorResponse = {
    val builder = CodeGeneratorResponse.newBuilder
    ProtobufGenerator.parseParameters(request.getParameter) match {
      case Right(params) =>
        try {
          val filesByName: Map[String, FileDescriptor] =
            request.getProtoFileList.asScala.foldLeft[Map[String, FileDescriptor]](Map.empty) { case (acc, fp) =>
              val deps = fp.getDependencyList.asScala.map(acc)
              acc + (fp.getName -> FileDescriptor.buildFrom(fp, deps.toArray))
            }

          val implicits = new DescriptorImplicits(params, filesByName.values.toVector)
          val genFiles = request.getFileToGenerateList.asScala.map(filesByName)
          val srvFiles = genFiles.map(new ReactorFilePrinter(implicits, _).result())
          builder.addAllFile(srvFiles.asJava)
        } catch {
          case e: GeneratorException =>
            builder.setError(e.message)
        }

      case Left(error) =>
        builder.setError(error)
    }
    builder.build()
  }

}
