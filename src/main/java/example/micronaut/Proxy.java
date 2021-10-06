package example.micronaut;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.client.ProxyHttpClient;
import io.micronaut.http.filter.OncePerRequestHttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import java.net.URI;
import java.util.Optional;
import lombok.SneakyThrows;
import org.reactivestreams.Publisher;

@Filter("/proxy/**")
public class Proxy extends OncePerRequestHttpServerFilter {
  private final ProxyHttpClient client;

  @Property(name = "micronaut.http.services.url")
  private String url;

  @Property(name = "micronaut.http.services.path")
  private String path;

  public Proxy(ProxyHttpClient client) {
    this.client = client;
  }

  @Override
  protected Publisher<MutableHttpResponse<?>> doFilterOnce(
    HttpRequest<?> request, ServerFilterChain chain) {
    if (!isPreflightRequest(request)) {
      return Publishers.map(client.proxy(mutateRequest(request)), response -> response);
    } else {
      return chain.proceed(request);
    }
  }

  @SneakyThrows
  MutableHttpRequest<?> mutateRequest(HttpRequest<?> request) {
    URI uri = new URI(url);
    MutableHttpRequest<?> mutableHttpRequest =
      request
        .mutate()
        .uri(
          b ->
            b.scheme(uri.getScheme())
              .host(uri.getHost())
              .port(uri.getPort())
              .replacePath(
                StringUtils.prependUri(
                  path, request.getPath().substring("/proxy".length()))));
    mutableHttpRequest.getHeaders().set(HttpHeaders.HOST, uri.getHost());
    return mutableHttpRequest;
  }

  boolean isPreflightRequest(HttpRequest<?> request) {
    io.micronaut.http.HttpHeaders headers = request.getHeaders();
    Optional<String> origin = headers.getOrigin();
    return origin.isPresent()
      && HttpMethod.OPTIONS == request.getMethod();
  }
}
