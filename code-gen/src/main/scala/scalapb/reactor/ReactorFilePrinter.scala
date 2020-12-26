package scalapb.reactor

import com.google.protobuf.Descriptors.{FileDescriptor, MethodDescriptor, ServiceDescriptor}
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import scala.collection.JavaConverters._
import scalapb.compiler.FunctionalPrinter.PrinterEndo
import scalapb.compiler.{DescriptorImplicits, FunctionalPrinter, NameUtils, StreamType}

class ReactorFilePrinter(
    implicits: DescriptorImplicits,
    file: FileDescriptor
) {

  import implicits._

  private val AbstractStub = "_root_.io.grpc.stub.AbstractStub"
  private val Channel = "_root_.io.grpc.Channel"
  private val CallOptions = "_root_.io.grpc.CallOptions"
  private val serverServiceDef = "_root_.io.grpc.ServerServiceDefinition"
  private val ReactorServerCalls = "_root_.com.salesforce.reactorgrpc.stub.ServerCalls"
  private val ReactorClientCalls = "_root_.com.salesforce.reactorgrpc.stub.ClientCalls"
  private val ServerCalls = "_root_.io.grpc.stub.ServerCalls"
  private val ClientCalls = "_root_.io.grpc.stub.ClientCalls"
  private val StreamObserver = "_root_.io.grpc.stub.StreamObserver"
  private val Mono = "_root_.reactor.core.publisher.Mono"
  private val SMono = "_root_.reactor.core.scala.publisher.SMono"
  private val SFlux = "_root_.reactor.core.scala.publisher.SFlux"

  private val FileName = file.scalaDirectory + "/" +
    s"Reactor${NameUtils.snakeCaseToCamelCase(baseName(file.getName), true)}"

  def scalaFileName: String =
    FileName + ".scala"

  def content: String = {
    val fp = new FunctionalPrinter()
    fp.add(
      s"package ${file.scalaPackageName}",
      "",
      "import scala.language.implicitConversions",
      ""
    ).print(file.getServices().asScala)((fp, s) => new ServicePrinter(s).print(fp))
      .result()
  }

  def result(): CodeGeneratorResponse.File = {
    val b = CodeGeneratorResponse.File.newBuilder()
    b.setName(scalaFileName)
    b.setContent(content)
    b.build()
  }

  class ServicePrinter(service: ServiceDescriptor) {

    import implicits._

    private[this] val OuterObject = s"Reactor${service.name}Grpc"
    private[this] val OuterObjectFullName = s"${file.scalaPackageName}.$OuterObject"
    private[this] val traitName = s"Reactor${service.name}"
    private[this] val traitFullName = s"$OuterObjectFullName.Reactor${service.name}"
    private[this] val asyncStubName = s"${traitName}Stub"
    private[this] val serviceName: String = service.name
    private[this] val servicePkgName: String = service.getFile.scalaPackageName
    private[this] val grpcDescriptor = s"_root_.$servicePkgName.${serviceName}Grpc.${service.descriptorName}"

    def print(fp: FunctionalPrinter): FunctionalPrinter =
      fp.add(s"object $OuterObject {")
        .indent
        .add(s"val SERVICE: _root_.io.grpc.ServiceDescriptor = $grpcDescriptor")
        .add("")
        .add(
          s"trait $traitName {"
        )
        .indented(
          _.print(service.getMethods().asScala.toVector)(printMethodSignature())
        )
        .add("}")
        .add("")
        .add(s"object $traitName {")
        .indented(
          _.add(s"def bindService(serviceImpl: ${traitFullName}): $serverServiceDef = ").indent
            .add(s"$serverServiceDef.builder($grpcDescriptor)")
            .print(service.getMethods().asScala.toVector)(
              printBindService(_, _)
            )
            .add(".build()")
            .outdent
        )
        .add("}")
        .add("")
        .add(
          s"class $asyncStubName(channel: $Channel, options: $CallOptions = $CallOptions.DEFAULT) ",
          s"extends $AbstractStub[$asyncStubName](channel, options) with ${traitName} {"
        )
        .indented(
          _.print(service.getMethods().asScala.toVector)(printAsyncClientStub(_, _))
            .add(
              s"override def build(channel: $Channel, options: $CallOptions): $asyncStubName = new $asyncStubName(channel, options)"
            )
        )
        .add("}")
        .add("")
        .add(s"def stub(channel: $Channel): $asyncStubName = new $asyncStubName(channel)")
        .add("")
        .outdent
        .add("}")

    private def methodSignature(method: MethodDescriptor): String = {
      val reqType = method.inputType.scalaType
      val resType = method.outputType.scalaType

      s"def ${method.name}" + (method.streamType match {
        case StreamType.Unary =>
          s"(request: $reqType): ${smono(resType)}"
        case StreamType.ClientStreaming =>
          s"(request: ${sflux(reqType)}): ${smono(resType)}"
        case StreamType.ServerStreaming =>
          s"(request: $reqType): ${sflux(resType)}"
        case StreamType.Bidirectional =>
          s"(request: ${sflux(reqType)}): ${sflux(resType)}"
      })
    }

    private def printMethodSignature()(fp: FunctionalPrinter, method: MethodDescriptor): FunctionalPrinter =
      fp.add(methodSignature(method))

    private def printBindService(fp: FunctionalPrinter, method: MethodDescriptor): FunctionalPrinter = {
      val reqType = method.inputType.scalaType
      val resType = method.outputType.scalaType
      val serviceCall = s"serviceImpl.${method.name}"
      val descriptor = s"_root_.$servicePkgName.${serviceName}Grpc.${method.descriptorName}"

      val fp0 = fp
        .add(".addMethod(")
        .indent
        .add(
          s"$descriptor,"
        )

      val fp1 = method.streamType match {
        case StreamType.Unary =>
          fp0
            .add(s"$ServerCalls.asyncUnaryCall(new $ServerCalls.UnaryMethod[$reqType, $resType] {")
            .indent
            .add(s"override def invoke(request: $reqType, observer: ${StreamObserver}[$resType]): Unit =")
            .indent
            // Mono.block() is not a blocking operation. The given Mono is always an instance of JustMono.
            // https://github.com/salesforce/reactive-grpc/blob/ce3b3b20e8192c5e6b38b2ed596531242d9708c0
            // /reactor/reactor-grpc-stub/src/main/java/com/salesforce/reactorgrpc/stub/ServerCalls.java#L38
            .add(s"$ReactorServerCalls.oneToOne(request, observer,")
            .indented(
              javaFunction(
                s"${mono(reqType)}",
                mono(resType),
                "mono",
                s"$serviceCall(mono.block())" +
                  s".asJava()"
              )
            )
            .add(")")
        case StreamType.ClientStreaming =>
          fp0
            .add(
              s"$ServerCalls.asyncClientStreamingCall(new $ServerCalls.ClientStreamingMethod[$reqType, " +
                s"$resType] {"
            )
            .indent
            .add(s"override def invoke(observer: ${StreamObserver}[$resType]): $StreamObserver[$reqType] =")
            .add(s"$ReactorServerCalls.manyToOne(observer,")
            .indented(
              javaFunction(
                s"${flux(reqType)}",
                mono(resType),
                "flux",
                s"$serviceCall($SFlux" +
                  s".fromPublisher(flux)).asJava()"
              )
            )
            .add(", null)")
        case StreamType.ServerStreaming =>
          fp0
            .add(
              s"$ServerCalls.asyncServerStreamingCall(new ${ServerCalls}.ServerStreamingMethod[$reqType, " +
                s"$resType] {"
            )
            .indent
            .add(s"override def invoke(request: $reqType, observer: $StreamObserver[${resType}]): Unit =")
            .add(s"$ReactorServerCalls.oneToMany(request, observer,")
            .indented(javaFunction(s"${mono(reqType)}", flux(resType), "mono", s"$serviceCall(mono.block()).asJava()"))
            .add(")")
        case StreamType.Bidirectional =>
          fp0
            .add(
              s"$ServerCalls.asyncBidiStreamingCall(new $ServerCalls.BidiStreamingMethod[$reqType, " +
                s"$resType] {"
            )
            .indent
            .add(s"override def invoke(observer: $StreamObserver[$resType]): ${StreamObserver}[$reqType] =")
            .add(s"$ReactorServerCalls.manyToMany(observer,")
            .indented(
              javaFunction(
                s"${flux(reqType)}",
                flux(resType),
                "flux",
                s"$serviceCall($SFlux" +
                  s".fromPublisher(flux)).asJava()"
              )
            )
            .add(", null)")
      }

      fp1.outdent
        .add("})")
        .outdent
        .add(")")
    }

    private def javaFunction(in: String, out: String, arg: String, body: String): PrinterEndo = { fp =>
      fp.add(s"new java.util.function.Function[$in, $out] {")
        .indent
        .add(s"def apply($arg: $in): $out = {")
        .indent
        .add(body)
        .outdent
        .add("}")
        .outdent
        .add("}")
    }

    private def printAsyncClientStub(fp: FunctionalPrinter, method: MethodDescriptor): FunctionalPrinter = {
      val reqType = method.inputType.scalaType
      val resType = method.outputType.scalaType
      val serviceCall = s"serviceImpl.${method.name}"
      val methodDescriptor = s"_root_.$servicePkgName.${serviceName}Grpc.${method.descriptorName}"
      val biConsumer = "new java.util.function.BiConsumer"
      val javaFunction = "new java.util.function.Function"

      val fp1 = method.streamType match {
        case StreamType.Unary =>
          fp.add(s"override def ${method.name}(request: $reqType): ${smono(resType)} =")
            .indent
            .add(
              s"$SMono.fromPublisher($ReactorClientCalls.oneToOne($Mono.just(request), " +
                s"$biConsumer[$reqType, $StreamObserver[$resType]] {"
            )
            .indent
            .add(s"def accept(req: $reqType, res: $StreamObserver[$resType]): Unit = {")
            .indent
            .add(
              s"$ClientCalls.asyncUnaryCall(getChannel().newCall($methodDescriptor, getCallOptions()), req, " +
                s"res)"
            )
        case StreamType.ClientStreaming =>
          fp.add(s"override def ${method.name}(request: ${sflux(reqType)}): ${smono(resType)} =")
            .indent
            .add(
              s"$SMono.fromPublisher($ReactorClientCalls.manyToOne(request.asJava(), " +
                s"$javaFunction[$StreamObserver[$resType], $StreamObserver[$reqType]] {"
            )
            .indent
            .add(s"def apply(res: $StreamObserver[$resType]): $StreamObserver[$reqType] = {")
            .indent
            .add(
              s"$ClientCalls.asyncClientStreamingCall(getChannel().newCall($methodDescriptor, getCallOptions" +
                s"()), res)"
            )
        case StreamType.ServerStreaming =>
          fp.add(s"override def ${method.name}(request: $reqType): ${sflux(resType)} =")
            .indent
            .add(
              s"$SFlux.fromPublisher($ReactorClientCalls.oneToMany($Mono.just(request), " +
                s"$biConsumer[$reqType, $StreamObserver[$resType]] {"
            )
            .indent
            .add(s"def accept(req: $reqType, res: $StreamObserver[$resType]): Unit = {")
            .indent
            .indent
            .add(
              s"$ClientCalls.asyncServerStreamingCall(getChannel().newCall($methodDescriptor, getCallOptions()), " +
                s"req, res)"
            )
        case StreamType.Bidirectional =>
          fp.add(s"override def ${method.name}(request: ${sflux(reqType)}): ${sflux(resType)} =")
            .indent
            .add(
              s"$SFlux.fromPublisher($ReactorClientCalls.manyToMany(request.asJava(), " +
                s"$javaFunction[$StreamObserver[$resType], $StreamObserver[$reqType]] {"
            )
            .indent
            .add(s"def apply(res: $StreamObserver[$resType]): $StreamObserver[$reqType] = {")
            .indent
            .add(
              s"$ClientCalls.asyncBidiStreamingCall(getChannel().newCall($methodDescriptor, getCallOptions()), res)"
            )
      }

      fp1.outdent
        .add("}}, getCallOptions()))")
        .outdent
        .add("")
    }

    private def sflux(tpe: String) = s"_root_.reactor.core.scala.publisher.SFlux[$tpe]"

    private def smono(tpe: String) = s"_root_.reactor.core.scala.publisher.SMono[$tpe]"

    private def mono(tpe: String) = s"_root_.reactor.core.publisher.Mono[$tpe]"

    private def flux(tpe: String) = s"_root_.reactor.core.publisher.Flux[$tpe]"
  }
}
