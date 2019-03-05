package org.stevenlooman.sw.sonar;

import org.sonar.api.Plugin;
import org.stevenlooman.sw.sonar.language.Magik;
import org.stevenlooman.sw.sonar.sensors.MagikSquidSensor;

public class MagikPlugin implements Plugin {

  public static final String FILE_SUFFIXES_KEY = "sonar.magik.file.suffixes";

  @Override
  public void define(Context context) {
    context.addExtension(Magik.class);
    context.addExtension(MagikProfile.class);
    context.addExtension(MagikRuleRepository.class);
    context.addExtension(MagikSquidSensor.class);
  }
}