package org.stevenlooman.sw.magik.toolkit;

import org.sonar.sslr.toolkit.ConfigurationModel;
import org.sonar.sslr.toolkit.Toolkit;

public final class MagikToolkit {

  private MagikToolkit() {
  }

  /**
   * Start Toolkit with a Magik parser.
   * @param args Arguments
   */
  public static void main(String[] args) {
    ConfigurationModel cm = new MagikConfigurationModel();
    Toolkit toolkit = new Toolkit("SSLR Magik Toolkit", cm);
    toolkit.run();
  }
}
