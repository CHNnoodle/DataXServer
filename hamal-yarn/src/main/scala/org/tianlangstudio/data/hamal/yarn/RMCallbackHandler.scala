package org.tianlangstudio.data.hamal.yarn

import java.io.File
import java.util.{Collections, List}

import org.tianlangstudio.data.hamal.core.{Constants, HamalConf}
import org.tianlangstudio.data.hamal.core.HamalConf
//import java.util.Collections

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path, FileContext}
import org.apache.hadoop.yarn.api.records._
import org.apache.hadoop.yarn.client.api.{AMRMClient, NMClient}
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.hadoop.yarn.util.{ConverterUtils, Records}
import scala.jdk.CollectionConverters._
//import scala.collection.JavaConverters._
/**
 * Created by zhuhq on 2016/4/29.
 */
class RMCallbackHandler(nmClient:NMClient,containerCmd:Container => String,hamalConf: HamalConf,yarnConfiguration: Configuration)  extends  AMRMClientAsync.CallbackHandler {
  private val logging = org.slf4j.LoggerFactory.getLogger(classOf[RMCallbackHandler])
  override def onContainersCompleted(statuses: List[ContainerStatus]): Unit = {
    for(containerStatus <- statuses.asScala) {
      logging.info(s"containerId:${containerStatus} exitStatus:${containerStatus}")
    }
  }

  override def onError(e: Throwable): Unit = {
    logging.error("on error",e)

  }

  override def getProgress: Float = {

    0
  }

  override def onShutdownRequest(): Unit = {
    logging.info("on shutdown request")

  }

  override def onNodesUpdated(updatedNodes: List[NodeReport]): Unit = {
    logging.info("on nodes updated")
    for(nodeReport <- updatedNodes.asScala) {
      logging.info(s"node id:${nodeReport} node labels:${nodeReport}");
    }
  }

  override def onContainersAllocated(containers: List[Container]): Unit = {
    logging.info("on containers allocated");
    for (container:Container <- containers.asScala) {
      try {
        // Launch container by create ContainerLaunchContext
        val  ctx = Records.newRecord(classOf[ContainerLaunchContext]);

        //ctx.setCommands(Collections.singletonList(""" echo "begin";sleep 900;echo "end"; """))
        ctx.setCommands(Collections.singletonList(containerCmd(container)))
        val packagePath = hamalConf.getString(Constants.DATAX_EXECUTOR_FILE,"executor.zip");
        val archiveStat = FileSystem.get(yarnConfiguration).getFileStatus(new Path(packagePath))
        val  packageUrl = ConverterUtils.getYarnUrlFromPath(
          FileContext.getFileContext.makeQualified(new Path(packagePath)));
        val packageResource = Records.newRecord[LocalResource](classOf[LocalResource])

        packageResource.setResource(packageUrl);
        packageResource.setSize(archiveStat.getLen);
        packageResource.setTimestamp(archiveStat.getModificationTime);
        packageResource.setType(LocalResourceType.ARCHIVE);
        packageResource.setVisibility(LocalResourceVisibility.APPLICATION)
        ctx.setLocalResources(Collections.singletonMap(Constants.DATAX_EXECUTOR_ARCHIVE_FILE_NAME,packageResource))
        logging.info("[AM] Launching container " + container.getId());
        nmClient.startContainer(container, ctx);
      } catch {
        case ex:Exception =>
          logging.info("[AM] Error launching container " + container.getId() + " " + ex);
      }
    }
  }

}
