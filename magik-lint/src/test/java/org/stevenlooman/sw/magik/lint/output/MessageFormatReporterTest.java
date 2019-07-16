package org.stevenlooman.sw.magik.lint.output;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.lint.CheckInfo;
import org.stevenlooman.sw.magik.lint.CheckInfraction;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MessageFormatReporterTest {

  class DummyCheckInfo extends CheckInfo {

    public DummyCheckInfo() {
      super(null);
    }

    @Override
    public String getSqKey() {
      return "dummy";
    }

    @Override
    public String getSeverity() {
      return "major";
    }

  }

  @Test
  public void testWindowsPath() throws FileNotFoundException {
    OutputStream os = new ByteArrayOutputStream();
    PrintStream stream = new PrintStream(os);

    Path path = Paths.get("c:\\git\\test.magik");
    CheckInfo checkInfo = new DummyCheckInfo();
    MagikIssue issue = MagikIssue.fileIssue("message", null);
    CheckInfraction infraction = new CheckInfraction(path, checkInfo, issue);

    MessageFormatReporter reporter = new MessageFormatReporter(stream, "${path}", null);
    reporter.reportIssue(infraction);

    String result = os.toString().trim();
    assertThat(result).isEqualTo(path.toString());
  }

}
