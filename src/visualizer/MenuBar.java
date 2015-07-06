package visualizer;

import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.StringJoiner;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class MenuBar implements ActionListener {

	private JMenuBar menuBar;

	private JMenuItem loadModelItem, exitItem, aboutItem;

	private Visualizer3D application;

	private static int menuBarHeight = 0;

	public static int getMenuBarHeight() {
		return menuBarHeight;
	}

	public MenuBar(Visualizer3D app, JFrame frame) {
		application = app;

		menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");
		JMenu menuAbout = new JMenu("About");

		menuBar.add(menuFile);
		menuBar.add(menuAbout);

		loadModelItem = new JMenuItem("Open .obj file");
		loadModelItem.addActionListener(this);
		exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(this);

		aboutItem = new JMenuItem("About");
		aboutItem.addActionListener(this);

		menuFile.add(loadModelItem);
		menuFile.add(exitItem);
		menuAbout.add(aboutItem);

		frame.setJMenuBar(menuBar);
		frame.pack();
		MenuBar.menuBarHeight = menuBar.getHeight();

	}

	private void openModelSelectionDialog() {
		FileDialog dialog = new FileDialog((Dialog) null);

		dialog.setFile("*.obj");

		dialog.setVisible(true);

		String selected = dialog.getFile();
		String dir = dialog.getDirectory();

		if (dir != null && selected != null) {
			String modelPath = dir + selected;

			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append(dir);
			strBuilder.append(selected.substring(0, selected.indexOf(".")));

			String partialPath = strBuilder.toString();

			StringJoiner strJoiner = new StringJoiner(".");
			strJoiner.add(partialPath).add("jpg");

			if (!new File(strJoiner.toString()).isFile()) {
				strJoiner = new StringJoiner(".");
				strJoiner.add(partialPath).add("png");
			}

			application.issueLoadModel(modelPath, strJoiner.toString());
		}

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == loadModelItem) {
			openModelSelectionDialog();
		} else if (e.getSource() == exitItem) {
			int result = JOptionPane.showConfirmDialog(null,
					"Are you sure you want to exit?", "Exit",
					JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				System.exit(0);
			}

		} else if (e.getSource() == aboutItem) {

			JOptionPane
					.showMessageDialog(
							null,
							"Created by Daniele Vettorel as final project of the "
									+ "Advanced Programming course, at the Free University of Bolzano.\n All rights are reserved.",
							"About", JOptionPane.INFORMATION_MESSAGE);
		}
	}
}
