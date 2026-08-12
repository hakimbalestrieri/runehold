package com.runehold;

import com.google.gson.Gson;
import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.AssignmentResult;
import com.runehold.domain.BuildingType;
import com.runehold.domain.ManaLedger;
import com.runehold.domain.ResourceCollectResult;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import com.runehold.domain.layout.GridPoint;
import com.runehold.persistence.RuneholdStateCodec;
import com.runehold.persistence.RuneholdStateStore;
import com.runehold.ui.RuneholdAssets;
import com.runehold.ui.RuneholdController;
import com.runehold.ui.RuneholdPanel;
import com.runehold.ui.RuneLiteRuneholdAssets;
import com.runehold.ui.village.VillageAnimationSettings;
import com.runehold.ui.village.VillageWindow;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Function;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Runehold",
	description = "Turn newly earned XP into mana and grow a persistent village"
)
public class RuneholdPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private RuneholdConfig config;

	@Inject
	private Gson gson;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	private BuildingCatalog catalog;
	private RuneholdStateStore stateStore;
	private VillageState state;
	private RuneholdXpEventAdapter xpEventAdapter;
	private RuneholdController controller;
	private RuneholdPanel panel;
	private VillageWindow villageWindow;
	private NavigationButton navigationButton;
	private RuneholdAssets uiAssets;
	private String activeProfileKey;

	@Override
	protected void startUp()
	{
		uiAssets = new RuneLiteRuneholdAssets(itemManager);
		catalog = new BuildingCatalog();
		stateStore = new RuneholdStateStore(
			configManager,
			new RuneholdStateCodec(gson, catalog));
		loadCurrentProfile();
		log.debug("Runehold started");
	}

	@Provides
	RuneholdConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneholdConfig.class);
	}

	@Override
	protected void shutDown()
	{
		if (state != null)
		{
			state.clearXpBaselines();
			if (Objects.equals(activeProfileKey, configManager.getRSProfileKey()))
			{
				stateStore.save(state);
			}
		}

		removeNavigation();
		closeVillageWindow();
		activeProfileKey = null;
		xpEventAdapter = null;
		controller = null;
		panel = null;
		state = null;
		stateStore = null;
		catalog = null;
		uiAssets = null;
		log.debug("Runehold stopped");
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		ensureCurrentProfile();
		if (xpEventAdapter.record(event) > 0)
		{
			stateStore.save(state);
			refreshPanel();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();
		if (gameState == GameState.LOGGED_IN)
		{
			ensureCurrentProfile();
			return;
		}

		if (gameState == GameState.LOGIN_SCREEN
			|| gameState == GameState.LOGIN_SCREEN_AUTHENTICATOR
			|| gameState == GameState.LOGGING_IN
			|| gameState == GameState.CONNECTION_LOST
			|| gameState == GameState.HOPPING)
		{
			state.clearXpBaselines();
		}
	}

	private void ensureCurrentProfile()
	{
		String currentProfileKey = configManager.getRSProfileKey();
		if (!Objects.equals(activeProfileKey, currentProfileKey))
		{
			loadCurrentProfile();
		}
	}

	private void loadCurrentProfile()
	{
		closeVillageWindow();
		activeProfileKey = configManager.getRSProfileKey();
		state = stateStore.load(LocalDate.now());
		state.clearXpBaselines();

		Village village = new Village(state, catalog, Boolean.getBoolean("runehold.testing"));
		ManaLedger ledger = new ManaLedger(state, LocalDate::now);
		xpEventAdapter = new RuneholdXpEventAdapter(ledger);
		RuneholdController loadedController = new RuneholdController(
			state,
			village,
			catalog,
			stateStore::save);
		if (loadedController.completeConstructionIfReady())
		{
			log.debug("Completed Runehold construction while loading profile");
		}
		if (!loadedController.applyOfflineProgress().isEmpty())
		{
			log.debug("Applied Runehold gathering progress while loading profile");
		}
		RuneholdPanel loadedPanel = new RuneholdPanel(
			loadedController.getViewModel(),
			type -> requestBuildingAction(loadedController, type),
			() -> openVillage(loadedController),
			gatheringCommands(loadedController),
			uiAssets);
		controller = loadedController;
		panel = loadedPanel;
		replaceNavigation();
	}

	private RuneholdPanel.GatheringCommands gatheringCommands(RuneholdController source)
	{
		return new RuneholdPanel.GatheringCommands()
		{
			@Override
			public void assign(String workerId, BuildingType site)
			{
				runGatheringCommand(source, controller ->
					noticeFor(controller.assignWorker(workerId, site)));
			}

			@Override
			public void release(String workerId)
			{
				runGatheringCommand(source, controller ->
					noticeFor(controller.removeWorker(workerId)));
			}

			@Override
			public void collect(BuildingType site)
			{
				runGatheringCommand(source, controller ->
					noticeFor(controller.collectGatheringSite(site)));
			}
		};
	}

	static String noticeFor(AssignmentResult result)
	{
		return result.isSuccess() ? null : result.getMessage();
	}

	static String noticeFor(ResourceCollectResult result)
	{
		if (result.getCollected() > 0 && !result.isStorageFull())
		{
			return null;
		}
		if (result.getCollected() == 0 && result.getRemainingAtSite() == 0)
		{
			return "Nothing to collect yet.";
		}
		return "Village storage is full. "
			+ result.getRemainingAtSite()
			+ " "
			+ result.getResourceType().getDisplayName().toLowerCase(java.util.Locale.US)
			+ " stayed at the site.";
	}

	/**
	 * Runs a gathering command on the client thread and surfaces its refusal, if any, in
	 * the panel. The command must never fail silently.
	 */
	private void runGatheringCommand(
		RuneholdController source,
		Function<RuneholdController, String> command)
	{
		clientThread.invokeLater(() ->
		{
			if (controller != source)
			{
				return;
			}
			String notice = command.apply(source);
			refreshPanel(notice);
		});
	}

	private void requestBuildingAction(RuneholdController source, BuildingType type)
	{
		if (source.getViewModel().getBuilding(type).getCurrentLevel() == 0)
		{
			openVillage(source, type);
			return;
		}
		requestUpgrade(source, type);
	}

	private void requestUpgrade(RuneholdController source, BuildingType type)
	{
		clientThread.invokeLater(() ->
		{
			if (controller != source)
			{
				return;
			}

			source.beginTimedUpgrade(type);
			refreshPanel();
		});
	}

	private void refreshPanel()
	{
		refreshPanel(null);
	}

	private void refreshPanel(String notice)
	{
		if (controller != null && panel != null)
		{
			com.runehold.ui.RuneholdViewModel viewModel = controller.getViewModel();
			panel.refresh(viewModel, notice);
			if (villageWindow != null)
			{
				villageWindow.updateAnimationSettings(animationSettings());
				villageWindow.refresh(viewModel);
			}
		}
	}

	private void requestBuild(
		RuneholdController source,
		BuildingType type,
		GridPoint destination)
	{
		clientThread.invokeLater(() ->
		{
			if (controller != source)
			{
				return;
			}
			source.build(type, destination);
			refreshPanel();
		});
	}

	private void requestMove(
		RuneholdController source,
		BuildingType type,
		GridPoint destination)
	{
		clientThread.invokeLater(() ->
		{
			if (controller != source)
			{
				return;
			}
			source.move(type, destination);
			refreshPanel();
		});
	}

	private void requestConstructionCompletion(RuneholdController source)
	{
		clientThread.invokeLater(() ->
		{
			if (controller == source && source.completeConstructionIfReady())
			{
				refreshPanel();
			}
		});
	}

	private void requestGroveCollection(RuneholdController source)
	{
		clientThread.invokeLater(() ->
		{
			if (controller == source)
			{
				source.collectManaGrove();
				refreshPanel();
			}
		});
	}

	private void openVillage(RuneholdController source)
	{
		openVillage(source, null);
	}

	private void openVillage(RuneholdController source, BuildingType placementType)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (controller != source)
			{
				return;
			}
			if (villageWindow == null)
			{
				villageWindow = new VillageWindow(
					controller.getViewModel(),
					catalog,
					uiAssets,
					new VillageWindow.Commands()
					{
						@Override
						public void build(BuildingType type, GridPoint destination)
						{
							requestBuild(source, type, destination);
						}

						@Override
						public void move(BuildingType type, GridPoint destination)
						{
							requestMove(source, type, destination);
						}

						@Override
						public void upgrade(BuildingType type)
						{
							requestUpgrade(source, type);
						}

						@Override
						public void collectManaGrove()
						{
							requestGroveCollection(source);
						}

						@Override
						public void completeConstruction()
						{
							requestConstructionCompletion(source);
						}
					},
					animationSettings());
			}
			villageWindow.updateAnimationSettings(animationSettings());
			villageWindow.showWindow();
			if (placementType != null)
			{
				villageWindow.beginPlacement(placementType);
			}
		});
	}

	private void closeVillageWindow()
	{
		VillageWindow window = villageWindow;
		villageWindow = null;
		if (window != null)
		{
			window.close();
		}
	}

	private VillageAnimationSettings animationSettings()
	{
		return new VillageAnimationSettings(
			config == null || config.ambientAnimations(),
			config == null ? 2 : config.characterDensity(),
			config == null ? RuneholdAnimationQuality.STANDARD : config.animationQuality(),
			config != null && config.reduceMotion());
	}

	private void replaceNavigation()
	{
		removeNavigation();
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/runehold_icon.png");
		navigationButton = NavigationButton.builder()
			.tooltip("Runehold")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
	}

	private void removeNavigation()
	{
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
	}
}
