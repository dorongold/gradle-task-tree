package com.dorongold.gradle.tasktree

import groovy.transform.TupleConstructor
import org.gradle.api.Action
import org.gradle.internal.Try
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
            printTaskFiles(graphRenderer, taskDetails.fileInputs, "<- ", 'inputs', FailureHeader, Failure)
        }
        if (withOutputs) {
            printTaskFiles(graphRenderer, taskDetails.fileOutputs, "-> ", 'outputs', SuccessHeader, Success)
        }
    }

    static void printTaskDescription(GraphRenderer graphRenderer, Try<String> description, StyledTextOutput.Style textStyle) {
        graphRenderer.output
                .withStyle(textStyle)
                .text(" - " + description.getOrMapFailure { e -> "Error! Failure determining description: [${e}]" })
    }

    static void printTaskFiles(GraphRenderer graphRenderer, Try<List<String>> files, String prefix,
                               String detailName, StyledTextOutput.Style prefixStyle, StyledTextOutput.Style textStyle) {
        graphRenderer.startChildren()
        files.getOrMapFailure { e -> ["[Error determining ${detailName}: ${e}]"] }.eachWithIndex { String file, int i ->
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
