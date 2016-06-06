/*
 * Copyright 2016, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.sync.api;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zanata.sync.common.exception.RepoSyncException;
import org.zanata.sync.common.exception.ZanataSyncException;
import org.zanata.sync.common.model.ErrorMessage;
import org.zanata.sync.common.model.UsernamePasswordCredential;
import org.zanata.sync.plugin.Plugins;
import org.zanata.sync.plugin.git.service.RepoSyncService;
import org.zanata.sync.plugin.zanata.ZanataSyncService;
import org.zanata.sync.plugin.zanata.service.impl.ZanataSyncServiceImpl;
import com.google.common.base.Strings;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Path("/job")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobResource {
    private static final Logger log =
            LoggerFactory.getLogger(JobResource.class);
    private static final Plugins PLUGINS = new Plugins();


    // TODO until we make trigger job an aync task, we won't be able to get status or cancel running job (To make it an async task, we will need database backend to store running job)
    /**
     * Get job status
     *
     * @param id
     *         - job identifier
     */
    @Path("status/{id}")
    @GET
    public Response getJobStatus(@PathParam(value = "id") String id) {

        return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
    }

    /**
     * Cancel job if it is running
     *
     * @param id
     *         - job identifier
     * @return - http code
     */
    @Path("cancel/{id}")
    @POST
    public Response cancelRunningJob(@PathParam("id") String id) {
//        try {
//            if (Strings.isNullOrEmpty(id)) {
//                return Response.status(Response.Status.NOT_FOUND).build();
//            }
//            schedulerServiceImpl.cancelRunningJob(new Long(id), type);
//            return Response.ok().build();
//        } catch (SchedulerException e) {
//            log.error("cancel error", e);
//            return Response.serverError().build();
//        } catch (JobNotFoundException e) {
//            log.warn("cancel job not found", e);
        return Response.status(
                Response.Status.SERVICE_UNAVAILABLE).build();
//        }
    }

    @OPTIONS
    public Response options() {
        // TODO return supported method and their accepted body (e.g. jobDetail map)
        return Response.ok().build();
    }

    /**
     * trigger job with jobDetail containing following fields:
     * <pre>
     *  <ul>
     *      <li>srcRepoUrl</li>
     *      <li>srcRepoUsername</li>
     *      <li>srcRepoSecret</li>
     *      <li>srcRepoBranch</li>
     *      <li>syncToZanataOption=source|trans|both</li>
     *      <li>srcRepoType=git|anything else we may support in the future</li>
     *      <li>zanataUrl</li>
     *      <li>zanataUsername</li>
     *      <li>zanataSecret</li>
     *   </ul>
     * </pre>
     *
     * @param id
     *         - work identifier
     * @param jobDetail
     *         detail about a job.
     * @return - http code
     * @see JobDetailEntry
     */
    @Path("/2zanata/start/{id}")
    @POST
    public Response triggerJobToSyncToZanata(@PathParam(value = "id") String id,
            Map<String, String> jobDetail) {
        List<String> unknownEntries = JobDetailEntry.unknownEntries(jobDetail);
        if (unknownEntries.size() > 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage("unknown entry",
                            "unknown entries:" + unknownEntries)).build();
        }

        Either<RepoSyncService, Response> srcRepoPlugin =
                createRepoSyncService(jobDetail);
        Either<ZanataSyncService, Response> zanataSyncService =
                createZanataSyncService(jobDetail);

        return syncToZanata(srcRepoPlugin, zanataSyncService, id);
    }

    private static Either<RepoSyncService, Response> createRepoSyncService(
            Map<String, String> jobDetail) {
        String repoUrl = JobDetailEntry.srcRepoUrl.extract(jobDetail);
        String repoUsername = Strings.nullToEmpty(JobDetailEntry.srcRepoUsername.extract(jobDetail));
        String repoSecret = Strings.nullToEmpty(JobDetailEntry.srcRepoSecret.extract(jobDetail));
        String repoBranch = JobDetailEntry.srcRepoBranch.extract(jobDetail);
        String repoType = JobDetailEntry.srcRepoType.extract(jobDetail);


        if (repoUrl == null || repoType == null) {
            return Either.fromRight(RepoSyncService.class,
                    Response.status(Response.Status.NOT_ACCEPTABLE)
                            .entity(new ErrorMessage("Missing entries",
                                    "missing repo url and type")).build());
        }

        RepoSyncService srcRepoPlugin = PLUGINS.getSrcRepoPlugin(repoType);
        srcRepoPlugin.setCredentials(
                new UsernamePasswordCredential(repoUsername, repoSecret));
        srcRepoPlugin.setUrl(repoUrl);
        srcRepoPlugin.setBranch(repoBranch);
        return Either.fromLeft(srcRepoPlugin, Response.class);
    }

    private static Either<ZanataSyncService, Response> createZanataSyncService(
            Map<String, String> jobDetail) {
        // TODO at the moment we assumes zanata.xml is in the repo so this is not needed
        String zanataUrl = JobDetailEntry.zanataUrl.extract(jobDetail);
        String zanataUsername =
                JobDetailEntry.zanataUsername.extract(jobDetail);
        String zanataSecret = JobDetailEntry.zanataSecret.extract(jobDetail);
        String syncToZanataOption =
                JobDetailEntry.syncToZanataOption.extract(jobDetail);

        if (zanataUsername == null || zanataSecret == null) {
            return Either.fromRight(ZanataSyncService.class, Response.status(
                    Response.Status.NOT_ACCEPTABLE)
                    .entity(new ErrorMessage("Missing entries",
                            "missing zanata username and secret"))
                    .build());
        }

        return Either.fromLeft(
                new ZanataSyncServiceImpl(zanataUsername, zanataSecret,
                        syncToZanataOption), Response.class);
    }

    private Response syncToZanata(Either<RepoSyncService, Response> srcRepoPlugin,
            Either<ZanataSyncService, Response> zanataSyncService,
            String id) {
//        updateProgress(syncWorkConfig.getId(), 0,
//                "Sync to server starts", JobStatusType.RUNNING);
        try (AutoCleanablePath workingDir = new AutoCleanablePath(Files.createTempDirectory(id))){
            return srcRepoPlugin.map(plugin -> zanataSyncService.map(zanata -> {
                //        updateProgress(syncWorkConfig.getId(), 25,
//                "Cloning repository to " + workingDir, JobStatusType.RUNNING);
                plugin.setWorkingDir(workingDir.toFile());
                plugin.cloneRepo();
                zanata.pushToZanata(workingDir.toPath());
                return Response.ok().build();
            }, Function.identity()), Function.identity());


//        updateProgress(syncWorkConfig.getId(), 50,
//                "Pushing files to server from " + workingDir,
//                JobStatusType.RUNNING);



        } catch (Exception e) {
            throw new ZanataSyncException("Fail to sync to Zanata",
                    e);
        }
    }

    /**
     * trigger job to sync to source repo with jobDetail. JobDetail should
     * contain same fields as in org.zanata.sync.api.JobResourceImpl#triggerJobToSyncToZanata(java.lang.String,
     * java.util.Map).
     *
     * @param id
     *         - work identifier
     * @param jobDetail
     *         detail about a job.
     * @return - http code
     * @see JobDetailEntry
     */
    @Path("2repo/start/{id}")
    @POST
    public Response triggerJobToSyncToSourceRepo(
            @PathParam(value = "id") String id,
            Map<String, String> jobDetail) {
        List<String> unknownEntries = JobDetailEntry.unknownEntries(jobDetail);
        if (unknownEntries.size() > 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage("unknown entry",
                            "unknown entries:" + unknownEntries)).build();
        }

        Either<RepoSyncService, Response> srcRepoPlugin =
                createRepoSyncService(jobDetail);
        Either<ZanataSyncService, Response> zanataSyncService =
                createZanataSyncService(jobDetail);
        syncToSrcRepo(id, srcRepoPlugin, zanataSyncService);

        return Response.ok().build();
    }

    private void syncToSrcRepo(String id,
            Either<RepoSyncService, Response> srcRepoPlugin,
            Either<ZanataSyncService, Response> zanataSyncService) {
        //        updateProgress(syncWorkConfig.getId(), 0,
//                "Sync to repository starts", JobStatusType.RUNNING);
        try (AutoCleanablePath workingDir = new AutoCleanablePath(Files.createTempDirectory(id))){
//        updateProgress(syncWorkConfig.getId(), 20,
//                "Cloning repository to " + destDir, JobStatusType.RUNNING);
            srcRepoPlugin.map(plugin -> zanataSyncService.map(zanata -> {
                plugin.setWorkingDir(workingDir.toFile());
                plugin.cloneRepo();
                zanata.pullFromZanata(workingDir.toPath());
                plugin.syncTranslationToRepo();
                return Response.ok().build();
            }, Function.identity()), Function.identity());

//            updateProgress(syncWorkConfig.getId(), 40,
//                    "Pulling files from translation server to " + destDir,
//                    JobStatusType.RUNNING);

            // we only sync translation to source repo

//            updateProgress(syncWorkConfig.getId(), 60,
//                    "Commits to repository from " + destDir, JobStatusType.RUNNING);

//            updateProgress(syncWorkConfig.getId(), 80,
//                    "Cleaning directory: " + destDir, JobStatusType.RUNNING);
        } catch (Exception e) {
            throw new RepoSyncException("failed to sync to source repo", e);
        }
    }


}
