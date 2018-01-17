package com.isuwang.dapeng.maven.plugin;

import com.isuwang.dapeng.code.Scrooge;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Created by jackliang on 2017/10/19.
 */
@Mojo(name = "thriftGenerator")
public class GenerateFilePlugin extends AbstractMojo {

    @Parameter(property = "thriftGenerator.sourceFilePath")
    private String sourceFilePath;

    @Parameter(property = "thriftGenerator.targetFilePath")
    private String targetFilePath;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * 1、java  2、scala 3、both
     */
    @Parameter(property = "thriftGenerator.language", defaultValue = "both")
    private String language;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        String separator = System.getProperty("file.separator");
        String projectPath = new File(project.getBuild().getOutputDirectory()).getAbsolutePath().replace("target" + System.getProperty("file.separator") + "classes", "");
        sourceFilePath = projectPath + (sourceFilePath == null ? "src" + separator + "main" + separator + "resources" + separator + "thrift" + separator : sourceFilePath);
        targetFilePath = projectPath + (targetFilePath == null ? "src" + separator + "main" + separator : targetFilePath);

        System.out.println(" sourceFilePath: " + sourceFilePath);
        System.out.println(" targetFilePath: " + targetFilePath);

        if (language.equals("both") || language.equals("java")) {
            Scrooge.main(new String[]{"-gen", "java", "-all",
                    "-in", sourceFilePath,
                    "-out", targetFilePath});
//            File commonFile = new File(projectPath + "src/main/java/com/isuwang/soa/common");
//            if (commonFile.exists()) {
//                deleteDir(commonFile);
//            }
        }
        if (language.equals("both") || language.equals("scala")) {
            Scrooge.main(new String[]{"-gen", "scala", "-all",
                    "-in", sourceFilePath,
                    "-out", targetFilePath});

//            File scalaCommonFile = new File(projectPath + "src/main/scala/com/isuwang/soa/scala/common");
//            if (scalaCommonFile.exists()) {
//                deleteDir(scalaCommonFile);
//            }
        }
    }

    private static void deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                if (!children[i].equals("serializer")) {
                    deleteDir(new File(dir, children[i]));

                }
            }
        }
        if (!dir.getName().equals("serializer")) {
            dir.delete();
        }

    }

}
