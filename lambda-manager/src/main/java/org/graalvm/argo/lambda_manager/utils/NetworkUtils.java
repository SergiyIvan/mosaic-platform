package org.graalvm.argo.lambda_manager.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.utils.logger.Logger;

public class NetworkUtils {

	/**
	 * Waits for a port to be open.
	 * @param node - ip or dns name.
	 * @param port - port to probe.
	 * @param attempts - how many attemps to perform. A 1 second delay is used between attempts.
	 * @return True if the open is open. False otherwise.
	 */
    public static boolean waitForOpenPort(String node, int port, int attempts) {
        for (int i = 0; i < attempts; i++) {
            try {
                if (isPortOpen(node, port)) {
                    Logger.log(Level.INFO, node + ":" + port + " open!");
                    return true;
                } else {
                    Logger.log(Level.INFO, node + ":" + port + " not open. Waiting...");
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                // Ignored.
            }
        }
        Logger.log(Level.WARNING, node + ":" + port + " not open. Giving up.");
        return false;
    }

    /**
     * Checks if a particular port is open in a local or remote node.
     * @param node - ip or dns name.
     * @param port - port to probe.
     * @return True if the port is open. False otherwise.
     */
    private static boolean isPortOpen(String node, int port) {
        Socket s = null;
        boolean isOpen = false;
        try {
            s = new Socket();
            s.setReuseAddress(true);
            SocketAddress sa = new InetSocketAddress(node, port);
            s.connect(sa, 1000);
        } catch (IOException e) {
            // Ignoring.
        } finally {
            if (s != null) {
                isOpen = s.isConnected();
                try {
                    s.close();
                } catch (IOException e) {
                    // Ignoring.
                }
            }
        }
        return isOpen;
    }
}
