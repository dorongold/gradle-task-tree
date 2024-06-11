package com.dorongold.gradle.tasktree

import groovy.transform.TupleConstructor
import org.gradle.api.Action
import org.gradle.internal.graph.GraphRenderer
import org.gradle.internal.logging.text.StyledTextOutput

import static org.gradle.internal.logging.text.StyledTextOutput.Style.Description
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Failure
import static org.gradle.internal.logging.text.StyledTextOutput.Style.FailureHeader
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Identifier
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Info
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Normal
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Success
import static org.gradle.internal.logging.text.StyledTextOutput.Style.SuccessHeader

@TupleConstructor
class NodeAction implements Action<StyledTextOutput> {

    GraphRenderer graphRenderer
    final boolean isFirst
    final TaskTreeTaskBase.TaskDetails taskDetails
    final boolean skipBecauseAlreadyVisited
    final boolean skipBecauseDepthReached
    final boolean withInputs
    final boolean withOutputs
    final boolean withDescription

    @Override
    void execute(final StyledTextOutput styledTextOutput) {
        // print task name
        styledTextOutput.withStyle(isFirst ? Identifier : Normal)
                .text(taskDetails.path)

        if (skipBecauseAlreadyVisited) {
            styledTextOutput.text(" *")
            return
        }

        if (withDescription && taskDetails.description) {
            printTaskDescription(graphRenderer, taskDetails.description, Description)
        }

        if (skipBecauseDepthReached) {
            styledTextOutput.text(" ...")
            return
        }

        if (withInputs) {
            printTaskFiles(graphRenderer, taskDetails.fileInputs, "<- ", FailureHeader, Failure)
        }
        if (withOutputs) {
            printTaskFiles(graphRenderer, taskDetails.fileOutputs, "-> ", SuccessHeader, Success)
        }
    }

    static void printTaskDescription(GraphRenderer graphRenderer, String description, StyledTextOutput.Style textStyle) {
        graphRenderer.output
                .withStyle(textStyle)
                .text(" - " + description)
    }

    static void printTaskFiles(GraphRenderer graphRenderer, List<String> files, String prefix, StyledTextOutput.Style prefixStyle, StyledTextOutput.Style textStyle) {
        graphRenderer.startChildren()
        files.eachWithIndex { File file, int i ->
            graphRenderer.output.println()
            graphRenderer.output
                    .withStyle(Info)
                    .text(graphRenderer.prefix)
            graphRenderer.output
                    .withStyle(prefixStyle)
                    .text(" " * 5 + "${prefix} ")
            graphRenderer.output
                    .withStyle(textStyle)
                    .text(file)
        }
        graphRenderer.completeChildren()
    }

}
