package br.com.bb.http_client_benchmark;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;

import static org.asynchttpclient.Dsl.asyncHttpClient;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;



@State(Scope.Benchmark)
public class Main {

    Vertx vertx = Vertx.vertx();
    WebClient client = WebClient.create(vertx);

    AsyncHttpClient asyncHttpClient = asyncHttpClient();

    Client jerseyClient = ClientBuilder.newBuilder().build();
    WebTarget webTarget = jerseyClient.target("http://localhost:8080/shoot");
    Invocation.Builder invocationBuilder = webTarget.request();

    OkHttpClient clientOkCkient = new OkHttpClient();
    Request request = new Request.Builder().url("http://localhost:8080/shoot").build();

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public Boolean VERTX() throws ExecutionException, InterruptedException {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        client.get(8080, "localhost", "/shoot")
                .send(ar -> {
                    future.complete(true);
                });
        return future.get();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public Response ASYNC() throws ExecutionException, InterruptedException {
        Future<Response> response = asyncHttpClient.prepareGet("http://localhost:8080/shoot").execute();
        return response.get();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public javax.ws.rs.core.Response JERSEY() {
        return invocationBuilder.get();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public okhttp3.Response OKHTTP() throws IOException {
        return clientOkCkient.newCall(request).execute();
    }

    @TearDown
    public void finish() throws IOException {
        vertx.eventBus().close(ar -> {
            ar.succeeded();
        });
        vertx.close();
        asyncHttpClient.close();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Main.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .forks(1)
                .threads(1)
                .build();
        new Runner(opt).run();
    }
}
