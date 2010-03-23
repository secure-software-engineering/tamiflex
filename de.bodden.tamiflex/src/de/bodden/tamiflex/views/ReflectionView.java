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
package de.bodden.tamiflex.views;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.bodden.tamiflex.views.TreeObject.Kind;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class ReflectionView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "reflectionview.views.ReflectionView";
	
	private TreeViewer viewer;
	private Action hideLibMethods;
	private Action doubleClickAction;
	private boolean hideJREMethods;

	private ReflectionViewContentProvider contentProvider;

	private Set<IPath> traceFiles = new HashSet<IPath>();

	private Menu contextMenu;

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	public boolean isHideJREMethods() {
		return hideJREMethods;
	}

	/**
	 * The constructor.
	 */
	public ReflectionView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		contentProvider = new ReflectionViewContentProvider(getViewSite(),this);
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new ReflectionViewLabelProvider());
		viewer.setSorter(new ViewerSorter());
		viewer.setInput(getViewSite());
		final Tree tree = viewer.getTree();

		tree.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				viewer.getControl().setMenu(null);
				Point point = new Point (e.x, e.y);
				TreeItem treeItem = tree.getItem (point);
				if (treeItem != null) {
					Object node = treeItem.getData();
					if(node instanceof TreeParent) {
						TreeParent parent = (TreeParent) node;
						if(parent.getParent()==TreeObject.INVISIBLE_ROOT_NODE) {
							viewer.getControl().setMenu(ReflectionView.this.contextMenu);
						} 						
					}
				}
			}
		});
		tree.addMouseListener(new MouseListener() {
			
			public void mouseUp(MouseEvent e) {	}
			
			@Override
			public void mouseDown(MouseEvent e) {
				viewer.getControl().setMenu(null);
				Point point = new Point (e.x, e.y);
				TreeItem treeItem = tree.getItem (point);
				if (treeItem != null) tree.setSelection(treeItem);
			}
			
			public void mouseDoubleClick(MouseEvent e) { }
		});
		
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "ReflectionView.viewer");
		makeActions();
		createContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ReflectionView.this.fillContextMenu(manager);
			}
		});
		contextMenu = menuMgr.createContextMenu(viewer.getControl());
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(hideLibMethods);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				super.fill(menu, index);
				MenuItem menuItem = new MenuItem(menu,0);
				menuItem.setText("Remove log from view");
				menuItem.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						TreeItem[] selection = viewer.getTree().getSelection();
						if(selection.length==1) {
							TreeItem treeItem = selection[0];
							TreeObject node = (TreeObject) treeItem.getData();
							if(node.getKind()==Kind.ONLINEMONITOR) {
								contentProvider.removeRoot(node);
							} else if (node.getKind()==Kind.TRACEFILE) {
								TraceFileNode tfn = (TraceFileNode)node;
								traceFiles.remove(tfn.getAbsolutePath());
								contentProvider.removeRoot(node);
							}
						}
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
			}			
		});
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
	}

	private void makeActions() {
		hideLibMethods = new Action("Hide source methods contained in library",IAction.AS_CHECK_BOX) {
			public void run() {
				hideJREMethods = isChecked();
				refresh();
			}
		};
		hideLibMethods.setToolTipText("Hides all source methods from classes belonging to the Java Runtime Library");
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if(obj instanceof TreeObject) {
					TreeObject treeObject = (TreeObject) obj;
					treeObject.handleDoubleClick();
				}
			}
		};
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	public void refresh() {
		contentProvider.initialize();
		viewer.refresh();
	}

	public void addTraceFile(IPath file) {
		this.traceFiles.add(file);		
	}

	public Set<IPath> getCurrentTraceFiles() {
		return Collections.unmodifiableSet(traceFiles);
	}
	
	public ReflectionViewContentProvider getContentProvider() {
		return contentProvider;
	} 
}
