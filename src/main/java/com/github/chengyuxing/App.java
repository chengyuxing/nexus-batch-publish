package com.github.chengyuxing;

import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class App {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("All args must not be null:\n" +
                    "<repositoryRootPath> <nexusServer> <username:password>");
            System.exit(0);
        }

        // /Users/chengyuxing/Downloads/maven-repository/repository/ http://localhost:8081/repository/maven-releases/ admin:admin@123

        // /home/repository
        final Path rootPath = Paths.get(args[0]);
        // http://...
        final String nexusServer = args[1].endsWith("/") ? args[1] : args[1] + "/";
        // admin:admin@123
        final String usernamePassword = args[2];

        if (!usernamePassword.contains(":")) {
            System.out.println("Username and password format error, please following <username>:<password>");
            System.exit(0);
        }

        System.out.println("init...");

        final String[] up = usernamePassword.split(":");
        final String username = up[0];
        final String password = up[1];

        final OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();

        try (Stream<Path> s = Files.find(rootPath, 50, (p, a) -> {
            String fullName = p.toString();
            return !a.isDirectory() && a.isRegularFile() && (fullName.endsWith(".jar") || fullName.endsWith(".pom"));
        })) {
            AtomicInteger i = new AtomicInteger();
            s.filter(p -> {
                try {
                    return !Files.isHidden(p) && !Files.isSymbolicLink(p);
                } catch (IOException ignore) {
                    return false;
                }
            }).forEach(path -> {
                // com/github/chengyuxing/rabbit-sql/7.8.6/rabbit-sql-7.8.6.jar
                String packagePath = path.subpath(rootPath.getNameCount(), path.getNameCount()).toString();

                RequestBody body = new RequestBody() {
                    @Nullable
                    @Override
                    public MediaType contentType() {
                        return MediaType.parse("application/octet-stream");
                    }

                    @Override
                    public long contentLength() throws IOException {
                        return Files.size(path);
                    }

                    @Override
                    public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
                        try (Source source = Okio.source(path)) {
                            bufferedSink.writeAll(source);
                        }
                    }
                };

                Request request = new Request.Builder()
                        .url(nexusServer + packagePath)
                        .header("Authorization", Credentials.basic(username, password, StandardCharsets.UTF_8))
                        .header("Expect", "")
                        .put(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    ResponseBody responseBody = response.body();
                    if (Objects.nonNull(responseBody)) {
                        String result = responseBody.string().trim();
                        if (!result.isEmpty()) {
                            System.out.println(result);
                        }
                    }
                    System.out.println("+ " + packagePath);
                    i.getAndIncrement();
                } catch (Exception e) {
                    System.out.println("Upload failed: " + packagePath);
                    throw new RuntimeException(e);
                }
            });
            System.out.println("-------------------------------------");
            System.out.println("Congratulations, " + i.get() + " packages pushed!");
        }
    }
}