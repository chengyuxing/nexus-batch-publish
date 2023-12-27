package com.github.chengyuxing;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
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
                .authenticator(new Authenticator() {
                    @NotNull
                    @Override
                    public Request authenticate(@Nullable Route route, @NotNull Response response) {
                        String credentials = Credentials.basic(username, password, StandardCharsets.UTF_8);
                        return response.request().newBuilder()
                                .header("Authorization", credentials)
                                .build();
                    }
                }).build();

        try (Stream<Path> s = Files.find(rootPath, 50, (p, a) -> {
            String fullName = p.toString();
            return !a.isDirectory() && (fullName.endsWith(".jar") || fullName.endsWith(".pom"));
        })) {
            s.forEach(path -> {
                // com/github/chengyuxing/rabbit-sql/7.8.6/rabbit-sql-7.8.6.jar
                String packagePath = path.subpath(rootPath.getNameCount(), path.getNameCount()).toString();

                RequestBody body = MultipartBody.create(path.toFile(), MediaType.parse("multipart/form-data"));

                Request request = new Request.Builder()
                        .url(nexusServer + packagePath)
                        .put(body)
                        .build();

                try {
                    Thread.sleep(100);
                    try (Response response = httpClient.newCall(request).execute()) {
                        ResponseBody responseBody = response.body();
                        if (Objects.nonNull(responseBody)) {
                            String result = responseBody.string().trim();
                            if (!result.isEmpty()) {
                                System.out.println(result);
                            }
                        }
                        System.out.println("+ " + packagePath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            System.out.println("-------------------------------------");
            System.out.println("Congratulations, all packages pushed!");
        }
    }
}