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
package de.bodden.tamiflex.launching.playout.ui;

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
import de.bodden.tamiflex.launching.playout.PlayOutLaunchConstants;


@SuppressWarnings("restriction")
public class PlayOutAgentLaunchTab extends AbstractLaunchConfigurationTab {

	private static final Image IMAGE = Activator.getImageDescriptor("icons/poa.gif").createImage();
	private Button fDirectlyRadioButton;
	private Button fToFolderRadioButton;
	private Text fOutputFolderText;
	private Button fOutputFolderButton;
	private Button fCountOption;
	private Button fVerboseOption;
	private Button fDontDumpClasses;
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
		createOutFolderOption(comp);
		createOptionsGroup(comp);
		setToFolderEnabled(false);			
	}
	
	private void createOutFolderOption(Composite parent) {
		Group group = SWTFactory.createGroup(parent, "Output/viewing options", 3, 2, GridData.FILL_HORIZONTAL);
		Composite comp = SWTFactory.createComposite(group, parent.getFont(), 3, 3, GridData.FILL_BOTH, 0, 0);
		fDirectlyRadioButton = createRadioButton(comp, "Show reflective calls directly at runtime");
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fDirectlyRadioButton.setLayoutData(gd);
		fToFolderRadioButton = createRadioButton(comp, "Store into folder");
		fToFolderRadioButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleRadioButtonSelected();
			}
		});
		fOutputFolderText = SWTFactory.createSingleText(comp, 1);
		fOutputFolderText.addModifyListener(fBasicModifyListener);
		fOutputFolderButton = createPushButton(comp, "Browse...", null);	 
		fOutputFolderButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleBrowseButtonSelected();
			}
		});	

		fDirectlyRadioButton.setSelection(true);
	}

	private void createOptionsGroup(Composite parent) {
        Group group = SWTFactory.createGroup(parent, "Additional options", 5, 2, GridData.FILL_HORIZONTAL);
        Composite comp = SWTFactory.createComposite(group, 5, 5, GridData.FILL_BOTH);
        GridLayout ld = (GridLayout)comp.getLayout();
        ld.marginWidth = 1;
        ld.marginHeight = 1;
        fCountOption = createCheckButton(comp, "report how often each trace-file entry was triggered"); 
        GridData gd = new GridData(SWT.BEGINNING, SWT.NORMAL, true, false);
        gd.horizontalSpan = 5;
        fCountOption.setLayoutData(gd);
        fCountOption.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateLaunchConfigurationDialog();
            }
        });		

        fDontDumpClasses = createCheckButton(comp, "do not dump classes, just dump the log file"); 
        gd.horizontalSpan = 5;
        fDontDumpClasses.setLayoutData(gd);
        fDontDumpClasses.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateLaunchConfigurationDialog();
            }
        });		

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
	
	/**
	 * handles the shared radio button being selected
	 */
	private void handleRadioButtonSelected() {
		setToFolderEnabled(isToFolder());
		updateLaunchConfigurationDialog();
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
		String currentContainerString = fOutputFolderText.getText();
		IContainer currentContainer = getContainer(currentContainerString);
		LaunchUtil.FolderSelectionDialog dialog = new LaunchUtil.FolderSelectionDialog(getShell(),
				   currentContainer,
				   "Select output folder");
		dialog.showClosedProjects(false);
		dialog.open();
		Object[] results = dialog.getResult();	
		if ((results != null) && (results.length > 0) && (results[0] instanceof IPath)) {
			IPath path = (IPath)results[0];
			String containerName = path.toOSString();
			fOutputFolderText.setText(containerName);
		}		
	}	
	
	private boolean isToFolder() {
		return fToFolderRadioButton.getSelection();
	}

	private void setToFolderEnabled(boolean enable) {
		fOutputFolderText.setEnabled(enable);
		fOutputFolderButton.setEnabled(enable);
		//if we don't write to a folder then we don't dump classes and we also don't normalize
		fDontDumpClasses.setSelection(!enable);
		fDontDumpClasses.setEnabled(enable);
		fDontNormalize.setSelection(!enable);
		fDontNormalize.setEnabled(enable);
	}

	public String getName() {		
		return "Play-out Agent";
	}

	public void initializeFrom(ILaunchConfiguration configuration) {
		boolean toFolder = false;
		String outFolder = "";
		boolean count = false;
		boolean verbose = false;
		boolean dontDumpClasses = false;
		boolean dontNormalize = false;
		try {
			 toFolder = configuration.getAttribute(PlayOutLaunchConstants.WRITE_TO_FOLDER, false);
			 outFolder = configuration.getAttribute(PlayOutLaunchConstants.OUT_FOLDER_PATH, "");
			 count = configuration.getAttribute(PlayOutLaunchConstants.COUNT, false);
			 verbose = configuration.getAttribute(PlayOutLaunchConstants.VERBOSE, false);
			 dontDumpClasses = configuration.getAttribute(PlayOutLaunchConstants.DONT_DUMP_CLASSES, true);
			 dontNormalize = configuration.getAttribute(PlayOutLaunchConstants.DONT_NORMALIZE, true);
		} catch (CoreException e) {
		}
		fToFolderRadioButton.setSelection(toFolder);
		fDirectlyRadioButton.setSelection(!toFolder);
		fOutputFolderButton.setEnabled(toFolder);
		fOutputFolderText.setEnabled(toFolder);
		fOutputFolderText.setText(outFolder);
		fCountOption.setSelection(count);
		fVerboseOption.setSelection(verbose);
		fDontDumpClasses.setSelection(dontDumpClasses);
		fDontNormalize.setSelection(dontNormalize);
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		setAttribute(PlayOutLaunchConstants.WRITE_TO_FOLDER, configuration, fToFolderRadioButton.getSelection(), false);
		String outFolder = isToFolder() ? fOutputFolderText.getText() : null;
		configuration.setAttribute(PlayOutLaunchConstants.OUT_FOLDER_PATH, outFolder);
		setAttribute(PlayOutLaunchConstants.COUNT, configuration, fCountOption.getSelection(), false);
		setAttribute(PlayOutLaunchConstants.VERBOSE, configuration, fVerboseOption.getSelection(), false);
		setAttribute(PlayOutLaunchConstants.DONT_DUMP_CLASSES, configuration, fDontDumpClasses.getSelection(), false);
		setAttribute(PlayOutLaunchConstants.DONT_NORMALIZE, configuration, fDontNormalize.getSelection(), false);
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setContainer(null);
	}
	
    private boolean validateOutputFolder() {
		if (isToFolder()) {
			String path = fOutputFolderText.getText().trim();
			if(path.isEmpty()) {
				setErrorMessage("No output folder set"); 
				return false;
			}
			IContainer container = getContainer(path);
			if (container == null) {
				if (path==null) {
					setErrorMessage("No output folder set"); 
				} else {
					setErrorMessage("Output folder does not exist");
				}
				return false;
			}
		}
		return true;		
	}
	
	@Override
	public boolean canSave() {
		return validateOutputFolder();
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setMessage(null);
		setErrorMessage(null);
		return validateOutputFolder();
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
