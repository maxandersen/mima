package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import picocli.CommandLine;

/**
 * Classpath.
 */
@CommandLine.Command(name = "classpath", description = "Resolves Maven Artifact and prints out the classpath")
public final class Classpath extends ResolverCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to print classpath for")
    private String gav;

    @Override
    protected Integer doCall(Context context) throws DependencyResolutionException {
        info("Classpath {}", gav);

        Artifact artifact = new DefaultArtifact(gav);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
        collectRequest.setRepositories(context.remoteRepositories());
        DependencyRequest dependencyRequest =
                new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE));

        DependencyResult dependencyResult =
                context.repositorySystem().resolveDependencies(getRepositorySystemSession(), dependencyRequest);

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        dependencyResult.getRoot().accept(nlg);
        info("");
        info("{}", nlg.getClassPath());
        return 0;
    }
}
