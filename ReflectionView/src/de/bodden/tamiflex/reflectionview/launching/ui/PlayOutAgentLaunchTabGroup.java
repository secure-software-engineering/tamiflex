package de.bodden.tamiflex.reflectionview.launching.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.internal.debug.ui.launcher.LocalJavaApplicationTabGroup;

/**
 * Inserts a {@link PlayOutAgentLaunchTab} into the tab group for local
 * Java programs.
 */
@SuppressWarnings("restriction")
public class PlayOutAgentLaunchTabGroup extends LocalJavaApplicationTabGroup {

	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		super.createTabs(dialog, mode);
		List<ILaunchConfigurationTab> tabs =
			new ArrayList<ILaunchConfigurationTab>(Arrays.asList(getTabs()));
		tabs.add(1, new PlayOutAgentLaunchTab());
		ILaunchConfigurationTab[] tabsArray = tabs.toArray(new ILaunchConfigurationTab[0]);
		setTabs(tabsArray);
	}

}
