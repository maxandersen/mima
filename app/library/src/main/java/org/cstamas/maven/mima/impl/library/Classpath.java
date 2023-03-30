package org.cstamas.maven.mima.impl.library;

import java.nio.file.Paths;
import org.cstamas.maven.mima.context.Context;
import org.cstamas.maven.mima.context.ContextOverrides;
import org.cstamas.maven.mima.context.Runtime;
import org.cstamas.maven.mima.context.Runtimes;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.DependencyResolutionException;

public class Classpath {

    public String classpath(ContextOverrides overrides, String artifactStr) throws DependencyResolutionException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();

        try (Context context = runtime.create(overrides)) {
            Resolver resolver = new Resolver(context);
            DefaultArtifact artifact = new DefaultArtifact(artifactStr);
            return resolver.classpath(artifact);
        }
    }

    public static void main(String... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("g:a:v");
        }
        Classpath classpath = new Classpath();
        try {
            ContextOverrides overrides = ContextOverrides.Builder.create()
                    .localRepository(Paths.get("target/simple"))
                    .build();

            String cp = classpath.classpath(overrides, args[0]);
            System.out.println("Classpath of " + args[0] + " is:");
            System.out.println(cp);
        } catch (DependencyResolutionException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
