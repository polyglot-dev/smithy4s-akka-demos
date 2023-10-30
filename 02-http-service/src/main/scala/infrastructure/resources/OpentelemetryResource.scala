package infrastructure
package resources

import main.Configs.*

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.BatchSpanProcessor
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import java.util.concurrent.TimeUnit

object OpentelemetryResource{
  def init(): OpenTelemetry = {
// Export traces to Jaeger over OTLP
    val jaegerOtlpExporter: OtlpGrpcSpanExporter =
        OtlpGrpcSpanExporter.builder()
            .setEndpoint("http://localhost:4317")
            .setTimeout(30, TimeUnit.SECONDS)
            .build()

    val serviceNameResource: Resource  =
        Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "otel-jaeger-example"))

    // Set to process the spans by the Jaeger Exporter
    val tracerProvider: SdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(jaegerOtlpExporter).build())
            .setResource(Resource.getDefault().merge(serviceNameResource))
            .build()
    val openTelemetry: OpenTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()

    // TODO: make proper use of Cats Effect Resource
    // it's always a good idea to shut down the SDK cleanly at JVM exit.
    Runtime.getRuntime().addShutdownHook(new Thread(()=> tracerProvider.close))

    openTelemetry
  }

}
