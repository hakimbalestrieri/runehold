package com.runehold.ui.village;

import com.google.gson.Gson;
import com.runehold.domain.BuildResult;
import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import com.runehold.domain.layout.GridPoint;
import com.runehold.domain.layout.PlacementResult;
import com.runehold.persistence.RuneholdStateCodec;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VillageFunctionalScenarioTest
{
	@Test
	public void buildMoveCancelMoveAndReloadKeepsFinalPosition()
	{
		LocalDate today = LocalDate.of(2026, 8, 12);
		BuildingCatalog catalog = new BuildingCatalog();
		VillageState state = VillageState.fresh(today);
		Instant start = Instant.parse("2026-08-12T09:00:00Z");
		Village village = new Village(state, catalog, true,
			Clock.fixed(start, ZoneOffset.UTC));
		VillageInteractionModel interaction = new VillageInteractionModel();

		interaction.beginPlacement(BuildingType.MANA_WELL, new GridPoint(17, 17));
		assertFalse(village.previewBuild(
			BuildingType.MANA_WELL,
			interaction.getGhostPosition()).isSuccess());

		GridPoint firstPosition = new GridPoint(1, 12);
		interaction.updateGhost(firstPosition);
		BuildResult built = village.build(BuildingType.MANA_WELL, interaction.getGhostPosition());
		assertTrue(built.isSuccess());
		interaction.finish();
		assertEquals(firstPosition, village.positionOf(BuildingType.MANA_WELL));
		village = new Village(state, catalog, true,
			Clock.fixed(start.plusSeconds(4), ZoneOffset.UTC));
		assertTrue(village.completeConstructionIfReady());

		interaction.select(BuildingType.MANA_WELL);
		interaction.beginMove(BuildingType.MANA_WELL, firstPosition);
		interaction.updateGhost(new GridPoint(7, 7));
		PlacementResult collision = village.previewMove(
			BuildingType.MANA_WELL,
			interaction.getGhostPosition());
		assertFalse(collision.isSuccess());
		interaction.cancel();
		assertEquals(firstPosition, village.positionOf(BuildingType.MANA_WELL));

		GridPoint finalPosition = new GridPoint(14, 14);
		interaction.beginMove(BuildingType.MANA_WELL, firstPosition);
		interaction.updateGhost(finalPosition);
		assertTrue(village.move(
			BuildingType.MANA_WELL,
			interaction.getGhostPosition()).isSuccess());
		interaction.finish();

		RuneholdStateCodec codec = new RuneholdStateCodec(new Gson(), catalog);
		VillageState reopenedState = codec.decode(codec.encode(state), today.plusDays(1));
		Village reopenedVillage = new Village(reopenedState, catalog, true);

		assertEquals(1, reopenedVillage.levelOf(BuildingType.MANA_WELL));
		assertEquals(finalPosition, reopenedVillage.positionOf(BuildingType.MANA_WELL));
	}
}
