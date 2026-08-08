package utils

import org.gradle.api.artifacts.component.BuildIdentifier
import org.gradle.api.internal.TaskInternal
import org.gradle.composite.internal.IncludedBuildTaskResource
import org.gradle.execution.plan.Node
import org.gradle.execution.plan.TaskInAnotherBuild
import org.gradle.util.Path
import spock.lang.Specification

/**
 * The shape of the object graph behind a task from an included build changed in Gradle 9.1, which
 * broke unwrapping it (issue #62). These tests pin both shapes, since a plugin built against a
 * single Gradle version cannot see the other one, and the pre-9.1 path is only exercised by users
 * still on older Gradle versions.
 */
class TaskGraphUtilsTest extends Specification {

    private static final Path TASK_IDENTITY_PATH = Path.path(':includedProject:taskInIncludedProject')
    private static final String TASK_PATH = ':taskInIncludedProject'

    def "a node that is not a task from an included build is returned as is"() {
        given:
        Node node = Stub(Node)

        expect:
        TaskGraphUtils.unwrapTaskInAnotherBuild(node).is(node)
    }

    def "unwraps a task from an included build on Gradle 9.1 and above"() {
        given: 'a node exposing the target node directly, as TaskInAnotherBuild.getTargetNode() does'
        Node nodeInIncludedBuild = Stub(Node)
        Node taskInAnotherBuild = new ModernTaskInAnotherBuild(nodeInIncludedBuild)

        expect:
        TaskGraphUtils.unwrapTaskInAnotherBuild(taskInAnotherBuild).is(nodeInIncludedBuild)
    }

    def "unwraps a task from an included build on Gradle 7.6 - 9.0"() {
        given: 'IncludedBuildTaskResource -> ExportedTaskNode -> TaskNode, none of them exposed directly'
        Node nodeInIncludedBuild = Stub(Node)
        def exportedTaskNode = new LegacyExportedTaskNode(taskNode: nodeInIncludedBuild)
        def resource = new LegacyIncludedBuildTaskResource(taskNode: exportedTaskNode)
        Node taskInAnotherBuild = new LegacyTaskInAnotherBuild(resource)

        expect:
        TaskGraphUtils.unwrapTaskInAnotherBuild(taskInAnotherBuild).is(nodeInIncludedBuild)
    }

    def "a subtree is omitted rather than failing the build when a task from an included build cannot be unwrapped"() {
        given: 'Gradle 9.1+ throws from getTaskNode() for a task that was never scheduled'
        Node taskInAnotherBuild = new UnresolvableTaskInAnotherBuild()

        when:
        Set<Node> children = TaskGraphUtils.getChildTaskNodes(taskInAnotherBuild)

        then:
        children.isEmpty()
        noExceptionThrown()
    }

    /**
     * Gradle 9.1 and above: TaskInAnotherBuild.getTargetNode() returns the node in the included
     * build's execution plan, so getTarget() is never needed.
     */
    private static class ModernTaskInAnotherBuild extends TaskInAnotherBuild {
        Node targetNode

        ModernTaskInAnotherBuild(Node targetNode) {
            super(TASK_IDENTITY_PATH, TASK_PATH, null as BuildIdentifier)
            this.targetNode = targetNode
        }

        @Override
        protected IncludedBuildTaskResource getTarget() {
            throw new UnsupportedOperationException('getTarget() should not be used when getTargetNode() is available')
        }
    }

    /** Gradle 7.6 - 9.0: the node is only reachable through two levels of internal fields. */
    private static class LegacyTaskInAnotherBuild extends TaskInAnotherBuild {
        private final IncludedBuildTaskResource target

        LegacyTaskInAnotherBuild(IncludedBuildTaskResource target) {
            super(TASK_IDENTITY_PATH, TASK_PATH, null as BuildIdentifier)
            this.target = target
        }

        @Override
        protected IncludedBuildTaskResource getTarget() {
            return target
        }
    }

    private static class UnresolvableTaskInAnotherBuild extends TaskInAnotherBuild {

        UnresolvableTaskInAnotherBuild() {
            super(TASK_IDENTITY_PATH, TASK_PATH, null as BuildIdentifier)
        }

        @Override
        protected IncludedBuildTaskResource getTarget() {
            throw new IllegalStateException("Task '${TASK_PATH}' was never scheduled for execution.")
        }
    }

    private static class LegacyIncludedBuildTaskResource implements IncludedBuildTaskResource {
        /** An ExportedTaskNode - not the node in the execution plan. */
        Object taskNode

        @Override
        void queueForExecution() {}

        @Override
        void onComplete(Runnable action) {}

        @Override
        TaskInternal getTask() { return null }

        @Override
        IncludedBuildTaskResource.State getTaskState() { return IncludedBuildTaskResource.State.Scheduled }

        @Override
        String healthDiagnostics() { return '' }
    }

    private static class LegacyExportedTaskNode {
        Node taskNode
    }

}
