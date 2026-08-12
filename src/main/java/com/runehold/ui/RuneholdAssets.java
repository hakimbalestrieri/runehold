package com.runehold.ui;

import com.runehold.domain.BuildingType;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JLabel;

/**
 * Supplies game-native presentation assets without coupling Swing components to
 * RuneLite's asynchronous image APIs.
 */
public interface RuneholdAssets
{
	Font regularFont(float size);

	Font boldFont(float size);

	void addManaIcon(JLabel label);

	void addBuildingIcon(BuildingType type, JLabel label);

	void addGatheringIcon(BuildingType type, JLabel label);

	void addUpgradeIcon(JButton button);
}
