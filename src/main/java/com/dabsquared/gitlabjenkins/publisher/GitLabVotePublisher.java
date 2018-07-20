package com.dabsquared.gitlabjenkins.publisher;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.util.VoteUtil;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;

import java.io.IOException;
import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundConstructor;
import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

/**
 * @author Robin Müller
 */
public class GitLabVotePublisher extends MergeRequestNotifier implements SimpleBuildStep {

    @DataBoundConstructor
    public GitLabVotePublisher() { }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.GitLabVotePublisher_DisplayName();
        }
    }

    @Override
    protected void perform(Run<?, ?> build, TaskListener listener, GitLabClient client, MergeRequest mergeRequest) {
        VoteUtil.voteOnBuildResult(build.getResult(), listener, client, mergeRequest);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) {
        GitLabClient client = getClient(run);
        if (client == null) {
            listener.getLogger().println("No GitLab connection configured");
            return;
        }

        VoteUtil.voteOnBuildResult(run.getResult(), listener, client, getMergeRequest(run));
    }

}
