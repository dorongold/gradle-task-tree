package utils

import org.gradle.util.GradleVersion

class GradleUtils {
    public static boolean IS_GRADLE_MIN_7_6 = GradleVersion.current().baseVersion >= GradleVersion.version("7.6")
}
