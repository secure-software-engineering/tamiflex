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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import de.bodden.tamiflex.Activator;
import de.bodden.tamiflex.launching.LaunchUtil;
import de.bodden.tamiflex.launching.playin.PlayInLaunchConstants;

@SuppressWarnings("restriction")
public class PlayInAgentLaunchTab extends AbstractLaunchConfigurationTab {

	private static final Image IMAGE = Activator.getImageDescriptor("icons/pia.gif").createImage();
	private Text fInputFolderText;
	private Button fInputFolderButton;
	private Button fVerboseOption;
	private Button fDontNormalize;
	
	
	/**
	 * Modify listener that simply updates the owning launch configuration dialog.
	 */
	private ModifyListener fBasicModifyListener = new ModifyListener() {
		public void modifyText(ModifyEvent evt) {
			updateLaunchConfigurationDialog();
		}
	};
	
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		comp.setLayout(new GridLayout(2, true));
		comp.setFont(parent.getFont());
		createInFolderOption(comp);
		createOptionsGroup(comp);
	}
	
	private void createInFolderOption(Composite parent) {
		Group group = SWTFactory.createGroup(parent, "Input options", 3, 2, GridData.FILL_HORIZONTAL);
		Composite comp = SWTFactory.createComposite(group, parent.getFont(), 3, 3, GridData.FILL_BOTH, 0, 0);
		fInputFolderText = SWTFactory.createSingleText(comp, 1);
		fInputFolderText.addModifyListener(fBasicModifyListener);
		fInputFolderButton = createPushButton(comp, "Browse...", null);	 
		fInputFolderButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleBrowseButtonSelected();
			}
		});	
	}

	private void createOptionsGroup(Composite parent) {
        Group group = SWTFactory.createGroup(parent, "Additional options", 5, 2, GridData.FILL_HORIZONTAL);
        Composite comp = SWTFactory.createComposite(group, 5, 5, GridData.FILL_BOTH);
        GridLayout ld = (GridLayout)comp.getLayout();
        ld.marginWidth = 1;
        ld.marginHeight = 1;
        GridData gd = new GridData(SWT.BEGINNING, SWT.NORMAL, true, false);

        fDontNormalize = createCheckButton(comp, "do not normalize randomized class names"); 
        gd.horizontalSpan = 5;
        fDontNormalize.setLayoutData(gd);
        fDontNormalize.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateLaunchConfigurationDialog();
            }
        });	
        
        fVerboseOption = createCheckButton(comp, "enable verbose output"); 
        gd.horizontalSpan = 5;
        fVerboseOption.setLayoutData(gd);
        fVerboseOption.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateLaunchConfigurationDialog();
            }
        });			
	}	
	
	private IContainer getContainer(String path) {
		Path containerPath = new Path(path);
		return (IContainer) getWorkspaceRoot().findMember(containerPath);
	}
	
	/**
	 * Convenience method for getting the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
	/**
	 * Handles the shared location button being selected
	 */
	private void handleBrowseButtonSelected() { 
		String currentContainerString = fInputFolderText.getText();
		IContainer currentContainer = getContainer(currentContainerString);
		LaunchUtil.FolderSelectionDialog dialog = new LaunchUtil.FolderSelectionDialog(getShell(),
				   currentContainer,
				   "Select input folder");
		dialog.showClosedProjects(false);
		dialog.open();
		Object[] results = dialog.getResult();	
		if ((results != null) && (results.length > 0) && (results[0] instanceof IPath)) {
			IPath path = (IPath)results[0];
			String containerName = path.toOSString();
			fInputFolderText.setText(containerName);
		}		
	}	
	
	public String getName() {		
		return "Play-in Agent";
	}

	public void initializeFrom(ILaunchConfiguration configuration) {
		String inFolder = "";
		boolean verbose = false;
		boolean dontNormalize = false;
		try {
			 inFolder = configuration.getAttribute(PlayInLaunchConstants.IN_FOLDER_PATH, "");
			 verbose = configuration.getAttribute(PlayInLaunchConstants.VERBOSE, false);
			 dontNormalize = configuration.getAttribute(PlayInLaunchConstants.DONT_NORMALIZE, false);
		} catch (CoreException e) {
		}
		fInputFolderText.setText(inFolder);
		fVerboseOption.setSelection(verbose);
		fDontNormalize.setSelection(dontNormalize);
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(PlayInLaunchConstants.IN_FOLDER_PATH, fInputFolderText.getText());
		setAttribute(PlayInLaunchConstants.VERBOSE, configuration, fVerboseOption.getSelection(), false);
		setAttribute(PlayInLaunchConstants.DONT_NORMALIZE, configuration, fDontNormalize.getSelection(), false);
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setContainer(null);
	}
	
    private boolean validateInputFolder() {
		String path = fInputFolderText.getText().trim();
		if(path.isEmpty()) {
			setErrorMessage("No input folder set"); 
			return false;
		}
		IContainer container = getContainer(path);
		if (container == null) {
			if (path==null) {
				setErrorMessage("No input folder set"); 
			} else {
				setErrorMessage("Input folder does not exist");
			}
			return false;
		}
		return true;		
	}
	
	@Override
	public boolean canSave() {
		return validateInputFolder();
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setMessage(null);
		setErrorMessage(null);
		return validateInputFolder();
	}

	protected void updateLaunchConfigurationDialog() {
		if (getLaunchConfigurationDialog() != null) {
			//order is important here due to the call to 
			//refresh the tab viewer in updateButtons()
			//which ensures that the messages are up to date
			getLaunchConfigurationDialog().updateButtons();
			getLaunchConfigurationDialog().updateMessage();
		}
	}
	
	@Override
	public Image getImage() {
		return IMAGE;
	}
}
