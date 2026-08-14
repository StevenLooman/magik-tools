package nl.ramsolutions.sw.magik.languageserver.folding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.eclipse.lsp4j.FoldingRange;
import org.junit.jupiter.api.Test;

/** Tests for {@link FoldingRangeProvider}. */
class FoldingRangeProviderTest {

  private List<FoldingRange> provideFoldingRanges(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final FoldingRangeProvider provider = new FoldingRangeProvider();
    return provider.provideFoldingRanges(magikFile);
  }

  private List<FoldingRange> provideFoldingRangesModuleDef(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final ModuleDefFile moduleDefFile =
        new ModuleDefFile(ModuleDefFile.DEFAULT_URI, code, definitionKeeper, null);
    final FoldingRangeProvider provider = new FoldingRangeProvider();
    return provider.provideFoldingRanges(moduleDefFile);
  }

  private List<FoldingRange> provideFoldingRangesProductDef(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final ProductDefFile productDefFile =
        new ProductDefFile(ProductDefFile.DEFAULT_URI, code, definitionKeeper, null);
    final FoldingRangeProvider provider = new FoldingRangeProvider();
    return provider.provideFoldingRanges(productDefFile);
  }

  private List<FoldingRange> provideFoldingRangesLoadList(final String code) {
    final LoadListFile loadListFile = new LoadListFile(LoadListFile.DEFAULT_URI, code);
    final FoldingRangeProvider provider = new FoldingRangeProvider();
    return provider.provideFoldingRanges(loadListFile);
  }

  @Test
  void testProvideFoldingRangesMagikTypedFileFoldsMethodBody() {
    final String code =
        """
        _method a.b()
        \t_local x << 1
        _endmethod
        """;

    final List<FoldingRange> ranges = this.provideFoldingRanges(code);
    assertThat(ranges).hasSize(1);
    final FoldingRange range = ranges.get(0);
    assertThat(range.getStartLine()).isZero();
    assertThat(range.getEndLine()).isEqualTo(2);
  }

  @Test
  void testProvideFoldingRangesModuleDefFileFoldsEndTerminatedBlocks() {
    final String code =
        """
        test_module 1

        description
        \tTest module
        end
        """;

    final List<FoldingRange> ranges = this.provideFoldingRangesModuleDef(code);
    assertThat(ranges).hasSize(1);
    final FoldingRange range = ranges.get(0);
    assertThat(range.getStartLine()).isEqualTo(2);
    assertThat(range.getEndLine()).isEqualTo(4);
  }

  @Test
  void testProvideFoldingRangesProductDefFileFoldsEndTerminatedBlocks() {
    final String code =
        """
        test_product layered_product

        title
        \tTest product
        end
        """;

    final List<FoldingRange> ranges = this.provideFoldingRangesProductDef(code);
    assertThat(ranges).hasSize(1);
    final FoldingRange range = ranges.get(0);
    assertThat(range.getStartLine()).isEqualTo(2);
    assertThat(range.getEndLine()).isEqualTo(4);
  }

  @Test
  void testProvideFoldingRangesLoadListFileReturnsEmpty() {
    final String code =
        """
        source/my_class
        """;

    final List<FoldingRange> ranges = this.provideFoldingRangesLoadList(code);
    assertThat(ranges).isEmpty();
  }
}
