package com.memories.ui;

import com.memories.domain.RememberedName;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public final class MemoriesDialog
{
	private static final DateTimeFormatter NOTED_ON_FORMAT = DateTimeFormatter
		.ofLocalizedDate(FormatStyle.MEDIUM)
		.withLocale(Locale.getDefault())
		.withZone(ZoneId.systemDefault());

	private MemoriesDialog()
	{
	}

	/**
	 * @param previousNamesOldestFirst previous names in the order they were recorded, oldest first
	 */
	public static void show(Component parent, String currentName, List<RememberedName> previousNamesOldestFirst)
	{
		JPanel panel = new JPanel(new BorderLayout(0, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		panel.add(new JLabel("Previous names for " + currentName + ":"), BorderLayout.NORTH);

		if (previousNamesOldestFirst.isEmpty())
		{
			panel.add(new JLabel("No name changes recorded yet."), BorderLayout.CENTER);
		}
		else
		{
			panel.add(newestFirstList(previousNamesOldestFirst), BorderLayout.CENTER);
		}

		JOptionPane.showMessageDialog(parent, panel, "Memories — " + currentName, JOptionPane.PLAIN_MESSAGE);
	}

	private static JScrollPane newestFirstList(List<RememberedName> previousNamesOldestFirst)
	{
		DefaultListModel<String> model = new DefaultListModel<>();
		for (int i = previousNamesOldestFirst.size() - 1; i >= 0; i--)
		{
			RememberedName remembered = previousNamesOldestFirst.get(i);
			model.addElement(remembered.getName() + "  —  noted " + format(remembered.getObservedAtEpochMilli()));
		}

		JList<String> list = new JList<>(model);
		list.setVisibleRowCount(Math.min(8, model.size()));

		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(280, Math.min(200, 24 * model.size() + 20)));
		return scrollPane;
	}

	private static String format(long observedAtEpochMilli)
	{
		return NOTED_ON_FORMAT.format(Instant.ofEpochMilli(observedAtEpochMilli));
	}
}
