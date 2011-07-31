package de.bodden.tamiflex.launching.playout;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;

import de.bodden.tamiflex.launching.LaunchUtil;

public class JUnitPlayOutLaunchDelegate extends JUnitLaunchConfigurationDelegate {

	@Override
	public String getVMArguments(ILaunchConfiguration configuration)
			throws CoreException {
		return LaunchUtil.appendAgentArgs(super.getVMArguments(configuration), false);
	}
	
	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		super.launch(configuration, "run", launch, monitor);
	}

}
