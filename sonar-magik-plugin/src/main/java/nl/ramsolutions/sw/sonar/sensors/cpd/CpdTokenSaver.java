package nl.ramsolutions.sw.sonar.sensors.cpd;

import com.sonar.sslr.api.Token;
import java.util.Comparator;
import java.util.List;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.sonar.TokenLocation;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/** Copy/Paste Detection token saver. */
public class CpdTokenSaver {

  private static final Logger LOGGER = Loggers.get(CpdTokenSaver.class);

  private final SensorContext context;

  /**
   * Constructor.
   *
   * @param context Sensor context.
   */
  public CpdTokenSaver(final SensorContext context) {
    this.context = context;
  }

  /**
   * Save tokens for CPD.
   *
   * @param inputFile Input file.
   * @param productDefFile ProductDef file.
   */
  public void saveCpdTokens(final InputFile inputFile, final ProductDefFile productDefFile) {
    final List<Token> tokens = productDefFile.getTopNode().getTokens();
    this.saveCpdTokens(inputFile, tokens);
  }

  /**
   * Save tokens for CPD.
   *
   * @param inputFile Input file.
   * @param moduleDefFile ModuleDef file.
   */
  public void saveCpdTokens(final InputFile inputFile, final ModuleDefFile moduleDefFile) {
    final List<Token> tokens = moduleDefFile.getTopNode().getTokens();
    this.saveCpdTokens(inputFile, tokens);
  }

  /**
   * Save tokens for CPD.
   *
   * @param inputFile Input file.
   * @param loadListFile LoadList file.
   */
  public void saveCpdTokens(final InputFile inputFile, final LoadListFile loadListFile) {
    final List<Token> tokens = loadListFile.getTopNode().getTokens();
    this.saveCpdTokens(inputFile, tokens);
  }

  /**
   * Save tokens for CPD.
   *
   * @param inputFile Input file.
   * @param magikFile Magik file.
   */
  public void saveCpdTokens(final InputFile inputFile, final MagikFile magikFile) {
    final List<Token> tokens = magikFile.getTopNode().getTokens();
    this.saveCpdTokens(inputFile, tokens);
  }

  /**
   * Save tokens for CPD.
   *
   * @param inputFile Input file.
   * @param tokens Tokens to save.
   */
  private void saveCpdTokens(final InputFile inputFile, final List<Token> tokens) {
    LOGGER.debug("Saving CPD tokens, file: {}", inputFile);

    final NewCpdTokens newCpdTokens = this.context.newCpdTokens().onFile(inputFile);

    // Ensure order of tokens is preserved.
    final Comparator<TokenLocation> byLine = Comparator.comparing(TokenLocation::line);
    final Comparator<TokenLocation> byColumn = Comparator.comparing(TokenLocation::column);

    tokens.stream()
        .filter(token -> !token.getValue().trim().isEmpty())
        .map(TokenLocation::new)
        .sorted(byLine.thenComparing(byColumn))
        .forEach(
            tokenLocation ->
                newCpdTokens.addToken(
                    tokenLocation.line(),
                    tokenLocation.column(),
                    tokenLocation.endLine(),
                    tokenLocation.endColumn(),
                    tokenLocation.getValue()));
    newCpdTokens.save();
  }
}
