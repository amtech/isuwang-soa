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

    @Parameter(property = "thriftGenerator.sourceFilePath", defaultValue = "src\\main\\resources\\thrift\\")
    private String sourceFilePath;

    @Parameter(property = "thriftGenerator.targetFilePath", defaultValue = "src\\main\\")
    private String targetFilePath;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String projectPath = new File(project.getBuild().getOutputDirectory()).getAbsolutePath().replace("target\\classes", "");
        String sourceFilePath=projectPath+ "src\\main\\resources\\thrift\\";
        String targetFilePath=projectPath+"src\\main\\";

        System.out.println(" sourceFilePath: " + sourceFilePath);
        System.out.println(" targetFilePath: " + targetFilePath);

        Scrooge.main(new String[]{"-gen", "java", "-all",
                "-in", sourceFilePath,
                "-out", targetFilePath});

        System.out.println( projectPath+"src\\main\\java\\com\\isuwang\\soa\\common");

        File commonFile = new File(projectPath+"src\\main\\java\\com\\isuwang\\soa\\common");
        if (commonFile.exists()){
            deleteDir(commonFile);
        }

        Scrooge.main(new String[]{"-gen", "scala", "-all",
                "-in", sourceFilePath,
                "-out", targetFilePath});


    }
    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

}
