package utils

import com.dorongold.gradle.tasktree.TaskTreeTaskBase
import org.gradle.api.Task
import org.gradle.execution.plan.Node
import org.gradle.execution.plan.TaskInAnotherBuild
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TaskGraphUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskGraphUtils)

    static Set<Node> findRealTaskNodes(Collection<Node> nodes) {
        return nodes.findAll {
            it.task !instanceof TaskTreeTaskBase
        }.findAll {
            it.hasProperty('task')
        }
    }

    static Task getTaskFromTaskNode(Node taskNode) {
        // No unwrapping needed for tasks from included builds: TaskInAnotherBuild.getTask()
        // already delegates to the task in the other build.
        return taskNode.task
    }

    static <T> T visitRecursively(Node taskNode, Closure<T> action) {
        Set<Node> childTaskNodes = getChildTaskNodes(taskNode)
        List<TaskTreeTaskBase.TaskDetails> childrenDetails = new ArrayList<TaskTreeTaskBase.TaskDetails>(childTaskNodes.size())
        for (Node childTaskNode : childTaskNodes) {
            childrenDetails.add(visitRecursively(childTaskNode, action))
        }
        return action(taskNode, childrenDetails)
    }

    static Set<Node> getChildTaskNodes(Node taskNode) {
        Node nodeInOwningBuild
        try {
            nodeInOwningBuild = unwrapTaskInAnotherBuild(taskNode)
        } catch (Exception e) {
            // Printing a task tree should never break the build - omit the subtree instead
            LOGGER.info("[task-tree plugin]: Error determining the task dependencies of ${taskNode}", e)
            return Collections.emptySet()
        }
        nodeInOwningBuild.dependencySuccessors.findAll { it.hasProperty('task') }
    }

    /**
     * A task from an included build is represented in the current build's execution plan by a
     * {@link TaskInAnotherBuild} node, which holds no dependencies of its own. Its dependencies
     * are only reachable through the node representing that task in its own build's execution plan.
     */
    static Node unwrapTaskInAnotherBuild(Node taskNode) {
        if (!(taskNode instanceof TaskInAnotherBuild)) {
            return taskNode
        }
        if (taskNode.hasProperty('targetNode')) {
            // Gradle 9.1 and above expose the node in the other build's execution plan directly
            return taskNode.targetNode
        }
        // Gradle 7.6 - 9.0: IncludedBuildTaskResource -> ExportedTaskNode -> TaskNode
        return taskNode.target.taskNode.taskNode
    }

}
