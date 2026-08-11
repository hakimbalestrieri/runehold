package com.runehold;

import com.google.gson.Gson;
import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.ManaLedger;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import com.runehold.persistence.RuneholdStateCodec;
import com.runehold.persistence.RuneholdStateStore;
import com.runehold.ui.RuneholdController;
import com.runehold.ui.RuneholdPanel;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.Objects;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
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
	private Gson gson;

	private BuildingCatalog catalog;
	private RuneholdStateStore stateStore;
	private VillageState state;
	private RuneholdXpEventAdapter xpEventAdapter;
	private RuneholdPanel panel;
	private NavigationButton navigationButton;
	private String activeProfileKey;

	@Override
	protected void startUp()
	{
		catalog = new BuildingCatalog();
		stateStore = new RuneholdStateStore(
			configManager,
			new RuneholdStateCodec(gson, catalog));
		loadCurrentProfile();
		log.debug("Runehold started");
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
		activeProfileKey = null;
		xpEventAdapter = null;
		state = null;
		stateStore = null;
		catalog = null;
		log.debug("Runehold stopped");
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		ensureCurrentProfile();
		if (xpEventAdapter.record(event) > 0)
		{
			stateStore.save(state);
			panel.refresh();
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
		activeProfileKey = configManager.getRSProfileKey();
		state = stateStore.load(LocalDate.now());
		state.clearXpBaselines();

		Village village = new Village(state, catalog);
		ManaLedger ledger = new ManaLedger(state, LocalDate::now);
		xpEventAdapter = new RuneholdXpEventAdapter(ledger);
		RuneholdController controller = new RuneholdController(
			state,
			village,
			catalog,
			updatedState ->
			{
				stateStore.save(updatedState);
				panel.refresh();
			});
		panel = new RuneholdPanel(controller);
		replaceNavigation();
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
