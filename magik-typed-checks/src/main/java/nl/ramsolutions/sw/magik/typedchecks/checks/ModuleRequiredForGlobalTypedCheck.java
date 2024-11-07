package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.RecognitionException;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefFileScanner;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.moduledef.ModuleUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check to test if the module is required for a used global. */
@Rule(key = ModuleRequiredForGlobalTypedCheck.CHECK_KEY)
public class ModuleRequiredForGlobalTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ModuleRequiredForGlobal";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ModuleRequiredForGlobalTypedCheck.class);
  private static final String MESSAGE = "Module '%s' defining global '%s' is not required";
  // Create from base session:
  // ```
  // _for mod _over sw_module_manager.loaded_modules.fast_elements()
  // _loop
  //   !terminal!.write(mod.name, %,)
  // _endloop
  // ```
  private static final String DEFAULT_ALWAYS_LOADED_MODULES =
      "ace_core,acpt,acpt_geometry,actions,application,authorisation,base_dialogs,bookmark_manager,"
          + "browser,browser_sub_dialogs,cached_record_collection,collection_export_engine,"
          + "command_design_pattern,component_framework,connection_service,construction_pack_common,"
          + "core_resources,dataset_controller_core,datastore_base,datastore_geometry_raster,"
          + "datastore_geometry_raster_acps,datastore_geometry_spatial_predicate,datastore_geometry_tin,"
          + "datastore_geometry_topology_engine,datastore_geometry_transforms,datastore_geometry_vector,"
          + "debugger_core,dialogs,display_grid,document_list_views,document_manager,documents,drafting,"
          + "ds_src,dxf_exemplars,dynamic_transform_manager,extdb,formats,formatted_document,"
          + "geometry_raster,geometry_raster_streams,geometry_set_factory,geometry_tin,"
          + "geometry_transforms,geometry_vector,history_manager,infraction_checker,interaction_handler,"
          + "layout_engine,layout_gui,lazy_record_collection,legacy_widget_emulation,licence_manager,"
          + "magik_gui_components,magikscript,map_plugin,map_projections_catalogue,map_rendering,"
          + "map_support,map_trail,memento,messages_base,mgr_src,model,module_management,"
          + "module_management_magik_gui,multiple_deletion_dialog,network_analysis_plugin,"
          + "network_follower,oledb_provider_component,oledb_reader,options_plugin,outlook_bar_plugin,"
          + "pdf_generator,pdf_generator_application,plotting,predicates,printer_setup_wizard,profiler,"
          + "progress_manager,query_designers,query_dialog,rotate_view_dialog,rwo_actions_plugin,"
          + "rwo_core,rwo_sets,scrapbook,secure_storage,selection_set_lister,session_management,"
          + "short_transaction_manager_base,short_transaction_manager_client,"
          + "short_transaction_manager_server,simple_xml,sockets,style_core,style_properties,"
          + "style_symbol_magik_gui,style_widgets,super_dd,sw_automation,sw_automation_client,"
          + "sw_core_magik_sessions,sw_job_engine,swift_address_callouts,swift_base,swift_find,"
          + "swift_google,swift_layout_series,swift_map,swift_plugins,swift_sketch,"
          + "swift_view_application,sys_acps,sys_misc,sys_src,threading_tools,tics,transient_urwo_som,"
          + "tree,tree_item,undo_manager,units_configuration,units_core,units_definitions,"
          + "universal_extdb_rwo,universal_rwo,urn_manager,value_managers,wcm_credential_dialog,widgets,"
          + "xml_output";

  /** List of comment words, separated by ','. */
  @RuleProperty(
      key = "always loaded modules",
      defaultValue = "" + DEFAULT_ALWAYS_LOADED_MODULES,
      description =
          "List of modules which are always loaded, separated by ',', such as the modules from the base session",
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String alwaysLoadedModules = DEFAULT_ALWAYS_LOADED_MODULES;

  private ModuleDefinition moduleDefinition;
  private Set<String> requiredModules;

  @Override
  protected void walkPreMagik(final AstNode node) {
    this.moduleDefinition = this.readModuleDefinition();
    this.requiredModules = this.getRequiredModules();
  }

  @CheckForNull
  private ModuleDefinition readModuleDefinition() {
    final URI uri = this.getMagikFile().getUri();
    final Path path = Path.of(uri);
    final Path moduleDefPath = ModuleDefFileScanner.getModuleDefFileForPath(path);
    if (moduleDefPath == null) {
      return null;
    }

    final ModuleDefFile moduleDefFile;
    final IDefinitionKeeper definitionKeeper = this.getDefinitionKeeper();
    try {
      // TODO: Better get this from IDefinitionKeeper, instead of reading this for every file.
      moduleDefFile = new ModuleDefFile(moduleDefPath, definitionKeeper, null);
    } catch (final RecognitionException exception) {
      LOGGER.warn("Unable to parse module.def");
      return null;
    } catch (final IOException exception) {
      LOGGER.warn("Caught exception", exception);
      return null;
    }

    return moduleDefFile.getModuleDefinition();
  }

  private Set<String> getRequiredModules() {
    if (this.moduleDefinition == null) {
      return Collections.emptySet();
    }

    final Set<String> seen = new HashSet<>();

    final IDefinitionKeeper definitionKeeper = this.getDefinitionKeeper();
    final Deque<ModuleDefinition> stack = new ArrayDeque<>();
    stack.add(this.moduleDefinition);
    while (!stack.isEmpty()) {
      final ModuleDefinition currentModuleDefinition = stack.pop();
      final String moduleName = currentModuleDefinition.getName();
      if (seen.contains(moduleName)) {
        continue;
      }

      seen.add(moduleName);

      currentModuleDefinition.getUsages().stream()
          .map(ModuleUsage::getName)
          .map(definitionKeeper::getModuleDefinitions)
          .flatMap(Collection::stream)
          .forEach(stack::push);
    }

    // Also add all modules from this this.alwaysLoadedModules.
    Stream.of(this.alwaysLoadedModules.split(","))
        .map(String::strip)
        .filter(mod -> !mod.isEmpty())
        .forEach(seen::add);

    return seen;
  }

  @Override
  protected void walkPostMagik(final AstNode node) {
    this.moduleDefinition = null;
  }

  @Override
  protected void walkPostIdentifier(final AstNode node) {
    // Get own module + requires.
    if (this.moduleDefinition == null) {
      return;
    }

    final AstNode parent = node.getParent();
    if (!parent.is(MagikGrammar.ATOM)) {
      return;
    }

    final GlobalScope globalScope = this.getMagikFile().getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(node);
    if (scope == null) {
      return;
    }

    final ScopeEntry scopeEntry = scope.getScopeEntry(node);
    if (scopeEntry == null || !scopeEntry.isType(ScopeEntry.Type.GLOBAL)) {
      return;
    }

    final LocalTypeReasonerState state = this.getTypeReasonerState();
    final ExpressionResultString result = state.getNodeType(parent);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    if (typeStr.isUndefined()) {
      return;
    }

    // See if the target module is required.
    final IDefinitionKeeper definitionKeeper = this.getDefinitionKeeper();
    definitionKeeper.getExemplarDefinitions(typeStr).stream()
        .filter(def -> def.getModuleName() != null)
        .filter(def -> !this.requiredModules.contains(def.getModuleName()))
        .forEach(
            def -> {
              final String globalModuleName = def.getModuleName();
              final String typeStringStr = typeStr.getFullString();
              final String message = String.format(MESSAGE, globalModuleName, typeStringStr);
              this.addIssue(node, message);
            });
  }
}
