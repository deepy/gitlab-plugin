package com.dabsquared.gitlabjenkins.util;

import hudson.model.Result;
import hudson.model.TaskListener;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Awardable;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;

public class VoteUtil {
    private static final Logger LOGGER = Logger.getLogger(VoteUtil.class.getName());

    static public void voteOnBuildResult(Result result, TaskListener listener, GitLabClient client, MergeRequest mergeRequest) {
        if (mergeRequest == null) {
            listener.error("No merge request found, cannot add vote.");
            LOGGER.log(Level.SEVERE, "No merge request found, cannot add vote.");
            return;
        }

        boolean alreadyAwarded = false;
        try {
            Integer userId = client.getCurrentUser().getId();
            for (Awardable award : client.getMergeRequestEmoji(mergeRequest)) {
                if (award.getName().equals(getResultIcon(!isSuccessful(result)))) {
                    if (award.getUser().getId().equals(userId)) {
                        client.deleteMergeRequestEmoji(mergeRequest, award.getId());
                    }
                } else if (award.getName().equals(getResultIcon(isSuccessful(result)))) {
                    if (award.getUser().getId().equals(userId)) {
                        alreadyAwarded = true;
                    }
                }
            }
        } catch (WebApplicationException | ProcessingException e) {
            listener.getLogger().printf("Failed to remove vote on Merge Request for project '%s': %s%n", mergeRequest.getProjectId(), e.getMessage());
            LOGGER.log(Level.SEVERE, String.format("Failed to remove vote on Merge Request for project '%s'", mergeRequest.getProjectId()), e);
        }

        try {
            if (!alreadyAwarded) {
                client.awardMergeRequestEmoji(mergeRequest, getResultIcon(result));
            }
        } catch (NotFoundException e) {
            String message = String.format("Failed to add vote on Merge Request for project '%s'\n" +
                "Got unexpected 404, are you using the wrong API version or trying to vote on your own merge request?", mergeRequest.getProjectId());
            listener.getLogger().println(message);
            LOGGER.log(Level.WARNING, message, e);
        } catch (WebApplicationException | ProcessingException e) {
            listener.getLogger().printf("Failed to add vote on Merge Request for project '%s': %s%n", mergeRequest.getProjectId(), e.getMessage());
            LOGGER.log(Level.SEVERE, String.format("Failed to add vote on Merge Request for project '%s'", mergeRequest.getProjectId()), e);
        }
    }

    static private String getResultIcon(Result result) {
        return getResultIcon(isSuccessful(result));
    }

    static private boolean isSuccessful(Result result) {
        return result == Result.SUCCESS;
    }

    static private String getResultIcon(boolean success) {
        if (success) {
            return "thumbsup";
        } else {
            return "thumbsdown";
        }
    }
}
