/*******************************************************************************
 * Copyright (c) 2010 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package de.bodden.tamiflex.launching.playin.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.internal.debug.ui.launcher.LocalJavaApplicationTabGroup;

/**
 * Inserts a {@link PlayInAgentLaunchTab} into the tab group for local
 * Java programs.
 */
@SuppressWarnings("restriction")
public class PlayInAgentLaunchTabGroup extends LocalJavaApplicationTabGroup {

	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		super.createTabs(dialog, mode);
		List<ILaunchConfigurationTab> tabs =
			new ArrayList<ILaunchConfigurationTab>(Arrays.asList(getTabs()));
		tabs.add(1, new PlayInAgentLaunchTab());
		ILaunchConfigurationTab[] tabsArray = tabs.toArray(new ILaunchConfigurationTab[0]);
		setTabs(tabsArray);
	}

}
