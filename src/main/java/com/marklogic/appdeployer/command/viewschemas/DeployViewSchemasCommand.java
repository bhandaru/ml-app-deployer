package com.marklogic.appdeployer.command.viewschemas;

import java.io.File;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.appdeployer.ConfigDir;
import com.marklogic.appdeployer.command.AbstractResourceCommand;
import com.marklogic.appdeployer.command.CommandContext;
import com.marklogic.appdeployer.command.SortOrderConstants;
import com.marklogic.mgmt.PayloadParser;
import com.marklogic.mgmt.ResourceManager;
import com.marklogic.mgmt.SaveReceipt;
import com.marklogic.mgmt.viewschemas.ViewManager;
import com.marklogic.mgmt.viewschemas.ViewSchemaManager;

/**
 * Processes each file in the view-schemas directory. For each one, then checks for a (view schema name)-views
 * directory in the view-schemas directory. If it exists, each file in that directory is processed as a view.
 * <p>
 * As of version 2.9.0, this command supports deploying view schemas to any database. Note though that the view schemas
 * aren't deployed into the targeted database, but rather the schemas database associated with the target database.
 */
public class DeployViewSchemasCommand extends AbstractResourceCommand {

	private String currentDatabaseIdOrName;
	private ViewSchemaManager currentViewSchemaManager;

	public DeployViewSchemasCommand() {
		// Don't need to delete anything, as view-schemas all live in a database
		setDeleteResourcesOnUndo(false);
		setExecuteSortOrder(SortOrderConstants.DEPLOY_SQL_VIEWS);
	}

	@Override
	public void execute(CommandContext context) {
		AppConfig appConfig = context.getAppConfig();
		deployViewSchemas(context, appConfig.getConfigDir(), appConfig.getContentDatabaseName());

		for (File dir : appConfig.getConfigDir().getDatabaseResourceDirectories()) {
			deployViewSchemas(context, new ConfigDir(dir), dir.getName());
		}
	}

	protected void deployViewSchemas(CommandContext context, ConfigDir configDir, String databaseIdOrName) {
		currentDatabaseIdOrName = databaseIdOrName;
		currentViewSchemaManager = new ViewSchemaManager(context.getManageClient(), databaseIdOrName);
		processExecuteOnResourceDir(context, configDir.getViewSchemasDir());
	}

	/**
	 * Not used since we override the execute method of the parent class.
	 *
	 * @param context
	 * @return
	 */
	@Override
	protected File[] getResourceDirs(CommandContext context) {
		return null;
	}

	@Override
	protected ResourceManager getResourceManager(CommandContext context) {
		return currentViewSchemaManager;
	}

	@Override
	protected void afterResourceSaved(ResourceManager mgr, CommandContext context, File resourceFile, SaveReceipt receipt) {
		PayloadParser parser = new PayloadParser();
		String viewSchemaName = parser.getPayloadFieldValue(receipt.getPayload(), "view-schema-name");
		File viewDir = new File(resourceFile.getParentFile(), viewSchemaName + "-views");
		if (viewDir.exists()) {
			ViewManager viewMgr = new ViewManager(context.getManageClient(), currentDatabaseIdOrName, viewSchemaName);
			for (File viewFile : listFilesInDirectory(viewDir)) {
				saveResource(viewMgr, context, viewFile);
			}
		}
	}
}
