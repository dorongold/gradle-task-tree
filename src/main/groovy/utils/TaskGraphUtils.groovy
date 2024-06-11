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
        if (taskNode instanceof TaskInAnotherBuild) {
            taskNode = taskNode.target.taskNode.taskNode
        }
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
        if (taskNode instanceof TaskInAnotherBuild) {
            taskNode = taskNode.target.taskNode.taskNode
        }
        taskNode.dependencySuccessors.findAll { it.hasProperty('task') }
    }

}
