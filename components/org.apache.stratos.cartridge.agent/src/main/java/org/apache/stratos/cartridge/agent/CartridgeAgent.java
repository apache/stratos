package org.apache.stratos.cartridge.agent;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.phase.Phase;
import org.apache.stratos.cartridge.agent.runtime.DataHolder;

/**
 * Cartridge agent runnable.
 */
public class CartridgeAgent implements Runnable {

    private static final Log log = LogFactory.getLog(CartridgeAgent.class);

    @Override
    public void run() {
        if(log.isInfoEnabled()) {
            log.info("Cartridge agent started");
        }
        
        List<Phase> phases = DataHolder.getInstance().getPhases();
        
        // ==== basic flow ====
        // Initializing Phase
        // -- StartListeners extension
        // -- Instance Started ext.
        // Starting Phase
        // -- Start Servers ext.
        // -- Wait till activate ext.
        // -- PersistenceVolumeExtensionExecutor
        // Working Phase
        // -- Log publisher ext.
        // -- keep agent live ext.
        // CleanUp Phase
        
        // execute all the phases of the agent
        for (Phase phase : phases) {
			phase.execute();
		}

        
    }

    public void terminate() {
        DataHolder.getInstance().setTerminated(true);
    }
}
