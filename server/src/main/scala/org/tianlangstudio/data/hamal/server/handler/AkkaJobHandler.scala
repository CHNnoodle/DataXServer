package org.tianlangstudio.data.hamal.server.handler

import java.util

import akka.actor.ActorRef
import akka.util.Timeout
import com.tianlangstudio.data.datax.JobInfo._
import com.tianlangstudio.data.datax.ext.thrift.{TaskCost, TaskResult, ThriftServer}
import com.tianlangstudio.data.datax.util.{ConfigUtil, Utils}
import com.tianlangstudio.data.datax.{CancelJob, Constants, SubmitJob}

import scala.concurrent.duration._

/**
 * Created by zhuhq on 2016/4/27.
 */
class AkkaJobHandler(jobSchedulerActor:ActorRef){
  implicit val timeout = Timeout(30 seconds)

  def submitJob(jobConfPath: String): String = {
    submitJobWithParams(jobConfPath,null)
  }

  def getJobStatus(jobId: String): String = {
    if(jobId2ExecutorId.contains(jobId) || acceptedJobIds.contains(jobId) || rerunJobIds.contains(jobId)) {
      Constants.JOB_STATUS_RUNNING
    }else if(jobId2Result.contains(jobId)) {
      Constants.JOB_STATUS_DONE
    }else {
      ""
    }
  }

  def getJobCost(jobId: String): TaskCost = {
    null
  }

  def submitJobWithParams(jobConfPath: String, params: util.Map[String, String]): String = {
    //val jobId = UUID.randomUUID().toString
    val jobDesc = ConfigUtil.readJobDescIfInFileAndReplaceHolder(jobConfPath,params)
    //val jobId = DigestUtils.md5Hex(jobDesc);
    val jobId = Utils.genJobId()
    jobSchedulerActor ! SubmitJob(jobId,jobDesc)
    jobId
  }

  def cancelJob(jobId: String): Boolean = {
    jobSchedulerActor ! CancelJob(jobId)
    true
  }

  def getJobResult(jobId: String): TaskResult = {
    if(Constants.JOB_STATUS_DONE.equals(getJobStatus(jobId))) {
      jobId2Result.get(jobId) match {
        case Some(taskResult) =>
          taskResult
        case _ =>
          null
      }
    }else {
      null
    }
  }
}
