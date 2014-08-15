/**
 *
 */
package org.apache.stratos.cartridge.agent;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.extensions.ExtensionHandler;

/**
 *
 */
public class ArtifactCopyTask implements Runnable {

    private static final Log log = LogFactory.getLog(ArtifactCopyTask.class);
    private final ExtensionHandler extensionHandler;
    private String source;
    private String destination;

    public ArtifactCopyTask(String src, String des) {
    	this.source = src;
    	this.destination = des;
        extensionHandler = CartridgeAgent.getExtensionHandler();
    }

    @Override
    public void run() {
        if (log.isDebugEnabled()) {
            log.debug("Executing Artifact Copy Task source[" + source +"] destination[" + destination +"] ");
        }        

        if (new File(destination).exists()) {
            extensionHandler.onCopyArtifactsExtension(source, destination);
        }
    }

}
