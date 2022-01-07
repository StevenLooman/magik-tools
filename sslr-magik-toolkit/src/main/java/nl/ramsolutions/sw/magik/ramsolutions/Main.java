package nl.ramsolutions.sw.magik.ramsolutions;

import org.sonar.sslr.toolkit.ConfigurationModel;
import org.sonar.sslr.toolkit.Toolkit;

/**
 * Main entry point.
 */
public final class Main {

    private Main() {
    }

    /**
     * Start Toolkit with a Magik parser.
     * @param args Arguments
     */
    public static void main(String[] args) {
        ConfigurationModel configurationModel = new MagikConfigurationModel();
        Toolkit toolkit = new Toolkit("SSLR Magik Toolkit", configurationModel);
        toolkit.run();
    }

}
